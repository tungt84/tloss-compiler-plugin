package org.tloss.compiler.soot.swift;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.tloss.compiler.soot.TransformHelper;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.NeExpr;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.internal.AbstractInstanceFieldRef;
import soot.jimple.internal.JReturnStmt;
import soot.util.Chain;

public class SwiftTransformHelper extends TransformHelper {
	Logger logger = Logger.getLogger(SwiftTransformHelper.class.getName());

	public String fromJavaType(String className) {
		if (className.trim().endsWith("[]")) {
			return "[" + fromJavaType(className.substring(0, className.length() - 2)) + "]";
		}
		if ("java.lang.String".equals(className)) {
			return "String";
		}
		if ("java.lang.Integer".equals(className) || "int".equals(className)) {
			return "Int32";
		}
		if ("java.lang.Float".equals(className) || "float".equals(className)) {
			return "Float";
		}
		if ("java.lang.Double".equals(className) || "double".equals(className)) {
			return "Double";
		}
		if ("java.lang.Boolean".equals(className) || "boolean".equals(className)) {
			return "Bool";
		}
		if ("java.lang.Short".equals(className) || "short".equals(className)) {
			return "Int16";
		}
		if ("java.lang.Character".equals(className) || "char".equals(className)) {
			return "Character";
		}
		if (className.indexOf('.') >= 0) {
			return className.replace('.', '_').replace('$', '_');
		}
		return null;
	}

	public String fromJavaType(soot.Type type) {

		String className = type.toString();
		return fromJavaType(className);
	}

	public void defineLocals(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options) throws IOException {
		for (Local l : locals.getElementsUnsorted()) {
			if (!l.getName().equals("this"))
				writer.write("let " + (toVariableName(l.getName())) + ":" + fromJavaType(l.getType()) + ";\n");
		}
	}

	public boolean compileLValueBox(StringBuffer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, Unit unit, ValueBox box, boolean skipThis,
			String phaseName, Map<String, String> options) throws IOException {
		if (skipThis && "this".equals(box.getValue().toString().trim()))
			return true;
		logger.info("compileLValueBox - value:" + box.getValue() + ",valueClass:" + box.getValue().getClass());
		Value value = box.getValue();
		if (value instanceof AbstractInstanceFieldRef) {
			AbstractInstanceFieldRef abstractInstanceFieldRef = (AbstractInstanceFieldRef) value;
			writer.append(toVariableName(abstractInstanceFieldRef.getBase().toString()) + "."
					+ abstractInstanceFieldRef.getField().getName()+"!");
			return false;
		}
		writer.append(toVariableName(box.getValue().toString()));
		return false;
	}

	public String toVariableName(String var) {
		if ("this".equals(var)) {
			return "self";
		}
		var = var.toString().replace('$', '_');
		return var;
	}

	public void compileJStaticInvokeExpr(StringBuffer writer, soot.jimple.internal.JStaticInvokeExpr v) {
		SootClass c = v.getMethod().getDeclaringClass();
		writer.append(fromJavaType(c.toString())).append(".").append(v.getMethod().getName()).append("(");
		int n = v.getArgCount();
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				if (i == 0) {
					writer.append(toVariableName(v.getArg(i).toString()));
				} else {
					writer.append("," + toVariableName(v.getArg(i).toString()));
				}
			}
		}
		writer.append(")");
	}

	public boolean compileRValueBox(StringBuffer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, Unit unit, ValueBox box, boolean skipThis,
			String phaseName, Map<String, String> options) throws IOException {

		Value value = box.getValue();
		logger.info("compileRValueBox - value:" + value + ",valueClass:" + value.getClass());
		if (value instanceof soot.jimple.ParameterRef) {
			writer.append("_parameter" + ((soot.jimple.ParameterRef) value).getIndex());
			return false;
		} else if (value instanceof Constant) {
			writer.append(box.getValue().toString());
			return false;
		} else if (value instanceof soot.jimple.internal.JNewExpr) {
			return true;
		} else if (value instanceof soot.jimple.internal.JimpleLocal) {
			writer.append(toVariableName(box.getValue().toString()));
			return false;
		} else if (value instanceof AbstractInstanceFieldRef) {
			AbstractInstanceFieldRef abstractInstanceFieldRef = (AbstractInstanceFieldRef) value;
			writer.append(toVariableName(abstractInstanceFieldRef.getBase().toString()) + "."
					+ abstractInstanceFieldRef.getField().getName()+"!");
			return false;
		} else if (value instanceof soot.jimple.StaticFieldRef) {
			soot.jimple.StaticFieldRef staticFieldRef = (soot.jimple.StaticFieldRef) value;
			writer.append(
					fromJavaType(staticFieldRef.getField().getType()) + "." + staticFieldRef.getField().getName());
			return false;
		} else if (value instanceof soot.jimple.InstanceInvokeExpr) {
			soot.jimple.InstanceInvokeExpr invokeExpr = (soot.jimple.InstanceInvokeExpr) value;
			writer.append(toVariableName(invokeExpr.getBase().toString())).append(".")
					.append(invokeExpr.getMethodRef().name() + "(");
			int n = invokeExpr.getArgCount();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					if (i == 0) {
						writer.append(toVariableName(invokeExpr.getArg(i).toString()));
					} else {
						writer.append("," + toVariableName(invokeExpr.getArg(i).toString()));
					}
				}
			}
			writer.append(")");
			return false;
		} else if (value instanceof soot.jimple.internal.JCastExpr) {
			soot.jimple.internal.JCastExpr v = (soot.jimple.internal.JCastExpr) value;
			writer.append(toVariableName(v.getOp().toString()) + " as " + fromJavaType(v.getCastType()));
			return false;
		} else if (value instanceof soot.jimple.internal.JInstanceOfExpr) {
			soot.jimple.internal.JInstanceOfExpr v = (soot.jimple.internal.JInstanceOfExpr) value;
			writer.append(toVariableName(v.getOp().toString()) + " is " + fromJavaType(v.getCheckType()));
			return false;
		} else if (value instanceof soot.jimple.internal.JStaticInvokeExpr) {
			soot.jimple.internal.JStaticInvokeExpr v = (soot.jimple.internal.JStaticInvokeExpr) value;
			compileJStaticInvokeExpr(writer, v);
			return false;
		} else if (value instanceof soot.jimple.ConditionExpr) {
			if (value instanceof EqExpr) {
				EqExpr eqExpr = (EqExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append("==");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else if (value instanceof GeExpr) {
				GeExpr eqExpr = (GeExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append(">=");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else if (value instanceof GtExpr) {
				GtExpr eqExpr = (GtExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append(">");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else if (value instanceof LeExpr) {
				LeExpr eqExpr = (LeExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append("<=");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else if (value instanceof LtExpr) {
				LtExpr eqExpr = (LtExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append("<");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else if (value instanceof NeExpr) {
				NeExpr eqExpr = (NeExpr) value;
				compileLValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp1Box(),
						skipThis, phaseName, options);
				writer.append("!=");
				compileRValueBox(writer, sootClass, method, b, locals, patchingChain, unit, eqExpr.getOp2Box(),
						skipThis, phaseName, options);
				return false;
			} else {
				throw new IOException("Unsupport Value: " + value.getClass() + ",Unit:" + unit);
			}
		} else {
			throw new IOException("Unsupport Value: " + value.getClass() + ",Unit:" + unit);
		}
	}

	public void compileIdentityStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, IdentityStmt identityStmt, String phaseName, Map<String, String> options)
			throws IOException {
		ValueBox left = identityStmt.getLeftOpBox();
		ValueBox right = identityStmt.getRightOpBox();
		logger.info("compileIdentityStmt: " + identityStmt + "(" + left.getClass() + ":" + right.getClass() + ")");
		StringBuffer buffer = new StringBuffer();
		boolean skip = compileLValueBox(buffer, sootClass, method, b, locals, patchingChain, identityStmt, left, true,
				phaseName, options);
		if (!skip) {
			buffer.append("=");
			skip = compileRValueBox(buffer, sootClass, method, b, locals, patchingChain, identityStmt, right, true,
					phaseName, options);
			buffer.append(";\n");
		}
		if (!skip) {
			writer.write(buffer.toString());
		}
	}

	public void compileAssignStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, AssignStmt assignStmt, String phaseName, Map<String, String> options)
			throws IOException {
		ValueBox left = assignStmt.getLeftOpBox();
		ValueBox right = assignStmt.getRightOpBox();
		logger.info("compileAssignStmt: " + assignStmt + "(" + left.getClass() + ":" + right.getClass() + ")");
		StringBuffer buffer = new StringBuffer();
		boolean skip = compileLValueBox(buffer, sootClass, method, b, locals, patchingChain, assignStmt, left, true,
				phaseName, options);
		if (!skip) {
			buffer.append("=");
			skip = compileRValueBox(buffer, sootClass, method, b, locals, patchingChain, assignStmt, right, true,
					phaseName, options);
			buffer.append(";\n");
		}
		if (!skip) {
			writer.write(buffer.toString());
		}
	}

	public boolean compileInvokeStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, InvokeStmt invokeStmt, String phaseName, Map<String, String> options)
			throws IOException {

		logger.info("compileInvokeStmt: " + invokeStmt + ",invokeClass:" + invokeStmt.getInvokeExpr().getClass());
		soot.jimple.InvokeExpr expr = invokeStmt.getInvokeExpr();
		if (expr instanceof soot.jimple.internal.JVirtualInvokeExpr) {
			soot.jimple.internal.JVirtualInvokeExpr invokeExpr = (soot.jimple.internal.JVirtualInvokeExpr) expr;
			writer.write(
					toVariableName(invokeExpr.getBase().toString()) + "." + invokeExpr.getMethodRef().name() + "(");
			int n = invokeExpr.getArgCount();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					if (i == 0) {
						writer.write(toVariableName(invokeExpr.getArg(i).toString()));
					} else {
						writer.write("," + toVariableName(invokeExpr.getArg(i).toString()));
					}
				}
			}
			writer.write(");\n");
			return false;
		} else if (expr instanceof soot.jimple.internal.JSpecialInvokeExpr) {
			soot.jimple.internal.JSpecialInvokeExpr invokeExpr = (soot.jimple.internal.JSpecialInvokeExpr) expr;
			if (invokeExpr.getMethodRef().name().equals("<init>")) {
				if (invokeExpr.getBase().toString().equals("this")) {
					if (!"java.lang.Object".equals(invokeExpr.getMethodRef().declaringClass().toString())) {
						writer.write("super.init" + "(");
						int n = invokeExpr.getArgCount();
						if (n > 0) {
							for (int i = 0; i < n; i++) {
								if (i == 0) {
									writer.write(toVariableName(invokeExpr.getArg(i).toString()));
								} else {
									writer.write("," + toVariableName(invokeExpr.getArg(i).toString()));
								}
							}
						}
						writer.write(");\n");
						return true;
					} else {
						// skip specialinvoke this.<java.lang.Object: void <init>()>();
						return false;
					}

				} else {
					writer.write(toVariableName(invokeExpr.getBase().toString()) + "="
							+ fromJavaType(invokeExpr.getMethodRef().declaringClass().toString().replace('$', '.'))
							+ "(");
					int n = invokeExpr.getArgCount();
					if (n > 0) {
						for (int i = 0; i < n; i++) {
							if (i == 0) {
								writer.write(toVariableName(invokeExpr.getArg(i).toString()));
							} else {
								writer.write("," + toVariableName(invokeExpr.getArg(i).toString()));
							}
						}
					}
					writer.write(");\n");
					return false;
				}

			} else {
				writer.write("super" + "." + invokeExpr.getMethodRef().name() + "(");
				int n = invokeExpr.getArgCount();
				if (n > 0) {
					for (int i = 0; i < n; i++) {
						if (i == 0) {
							writer.write(toVariableName(invokeExpr.getArg(i).toString()));
						} else {
							writer.write("," + toVariableName(invokeExpr.getArg(i).toString()));
						}
					}
				}
				writer.write(");\n");
				return false;
			}
		} else if (expr instanceof soot.jimple.internal.JStaticInvokeExpr) {
			StringBuffer buffer = new StringBuffer();
			compileJStaticInvokeExpr(buffer, (soot.jimple.internal.JStaticInvokeExpr) expr);
			writer.write(buffer.toString() + ";\n");
			return false;
		} else {
			throw new IOException("Unsupport InvokeExpr: " + expr.getClass() + ",Unit:" + invokeStmt);
		}
	}

	public void compileIfStmt(List<Label> labels, Writer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, IfStmt ifStmt, String phaseName,
			Map<String, String> options) throws IOException {
		StringBuffer buffer = new StringBuffer();
		buffer.append("if (");
		compileRValueBox(buffer, sootClass, method, b, locals, patchingChain, ifStmt, ifStmt.getConditionBox(), false,
				phaseName, options);
		buffer.append("){\n");
		compileGotoStmt(buffer, labels, ifStmt.getTarget(), method);
		buffer.append("}\n");
		writer.write(buffer.toString());
	}

	public void compileGotoStmt(StringBuffer buffer, List<Label> labels, Unit unit, SootMethod method) {
		boolean labeled = false;
		for (int i = 0; i < labels.size() && !labeled; i++) {
			if (unit.equals(labels.get(i).getUnit())) {
				labeled = true;
				buffer.append("__label__control=\"" + labels.get(i).getLabel() + "\";\n");
				buffer.append("break " + method.getName() + "_Loop;\n");
			}
		}
	}

	public void compileGotoStmt(List<Label> labels, Writer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, GotoStmt ifStmt, String phaseName,
			Map<String, String> options) throws IOException {
		StringBuffer buffer = new StringBuffer();
		compileGotoStmt(buffer, labels, ifStmt.getTarget(), method);
		writer.write(buffer.toString());
	}

	public void compileReturnVoidStmt(Writer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, ReturnVoidStmt ifStmt, String phaseName,
			Map<String, String> options) throws IOException {
		writer.write("return;\n");
	}

	public void compileReturnStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, JReturnStmt ifStmt, String phaseName, Map<String, String> options)
			throws IOException {
		writer.write("return " + toVariableName(ifStmt.getOp().toString()) + ";\n");
	}

	public List<Label> preCompile(PatchingChain<Unit> patchingChain) {
		List<Label> labels = new ArrayList<Label>();
		Unit unit = patchingChain.getFirst();
		int i = 1;
		do {

			if (unit instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) unit;
				Label label = new Label("label" + i, ifStmt.getTarget());
				i++;
				labels.add(label);

			} else if (unit instanceof soot.jimple.internal.JGotoStmt) {
				soot.jimple.internal.JGotoStmt gotoStmt = (soot.jimple.internal.JGotoStmt) unit;
				Label label = new Label("label" + i, gotoStmt.getTarget());
				i++;
				labels.add(label);
			}
			unit = patchingChain.getSuccOf(unit);
		} while (unit != null);
		return labels;
	}

	public boolean compilePatchingChain(List<Label> labels, Writer writer, SootClass sootClass, SootMethod method,
			Body b, Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName,
			Map<String, String> options) throws IOException {
		Unit unit = patchingChain.getFirst();
		boolean override = false;
		if (labels.size() > 0) {
			writer.write("let __label__control=\"START\";\n");
			writer.write(method.getName() + "_Loop: while(__label__control != \"STOP\"){\n");
			writer.write("switch (__label__control){\n");
			writer.write("case \"START\":\n");
		}
		do {
			boolean labeled = false;
			for (int i = 0; i < labels.size() && !labeled; i++) {
				if (unit.equals(labels.get(i).getUnit())) {
					labeled = true;
					writer.write("case \"" + labels.get(i).getLabel() + "\":\n");
				}
			}
			if (unit instanceof IdentityStmt) {
				IdentityStmt identityStmt = (IdentityStmt) unit;
				compileIdentityStmt(writer, sootClass, method, b, locals, patchingChain, identityStmt, phaseName,
						options);
			} else if (unit instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) unit;
				compileAssignStmt(writer, sootClass, method, b, locals, patchingChain, assignStmt, phaseName, options);
			} else if (unit instanceof InvokeStmt) {
				InvokeStmt invokeStmt = (InvokeStmt) unit;
				boolean tmp = compileInvokeStmt(writer, sootClass, method, b, locals, patchingChain, invokeStmt,
						phaseName, options);
				override = override || tmp;
			} else if (unit instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) unit;
				compileIfStmt(labels, writer, sootClass, method, b, locals, patchingChain, ifStmt, phaseName, options);
			} else if (unit instanceof soot.jimple.internal.JGotoStmt) {
				soot.jimple.internal.JGotoStmt gotoStmt = (soot.jimple.internal.JGotoStmt) unit;
				compileGotoStmt(labels, writer, sootClass, method, b, locals, patchingChain, gotoStmt, phaseName,
						options);
			} else if (unit instanceof ReturnVoidStmt) {
				ReturnVoidStmt returnStmt = (ReturnVoidStmt) unit;
				compileReturnVoidStmt(writer, sootClass, method, b, locals, patchingChain, returnStmt, phaseName,
						options);
			} else if (unit instanceof soot.jimple.internal.JReturnStmt) {
				soot.jimple.internal.JReturnStmt returnStmt = (soot.jimple.internal.JReturnStmt) unit;
				compileReturnStmt(writer, sootClass, method, b, locals, patchingChain, returnStmt, phaseName, options);
			} else {
				throw new IOException("Unsupport Unit: " + unit.getClass() + ",Unit:" + unit);
			}
			unit = patchingChain.getSuccOf(unit);
		} while (unit != null);
		if (labels.size() > 0) {
			writer.write("}\n");
			writer.write("}\n");
		}
		return override;
	}

	/**
	 * 
	 * @param writer
	 * @param sootClass
	 * @param method
	 * @param b
	 * @param locals
	 * @param patchingChain
	 * @param phaseName
	 * @param options
	 * @return true neu can overwrite
	 * @throws IOException
	 */
	public boolean compileBody(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options) throws IOException {
		defineLocals(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
		List<Label> labels = preCompile(patchingChain);
		return compilePatchingChain(labels, writer, sootClass, method, b, locals, patchingChain, phaseName, options);
	}

	public static Map<String, String> classDeclare = new HashMap<String, String>();

	protected void addClassDeclare(Body b) {
		synchronized (classDeclare) {
			SootMethod method = b.getMethod();
			SootClass sootClass = method.getDeclaringClass();
			String className;
			synchronized (classDeclare) {
				if (!classDeclare.containsKey(sootClass.getType().getClassName())) {
					className = ("class " + sootClass.getType().getClassName().replace('.', '_') + ":"
							+ sootClass.getSuperclass().getType().getClassName().replace('.', '_') + "{\n");
					if (sootClass.getFieldCount() > 0) {
						Chain<SootField> fields = sootClass.getFields();
						SootField field = fields.getFirst();
						do {
							className += "var " + field.getName() + " : " + fromJavaType(field.getType()) + "?;\n";
							field = fields.getSuccOf(field);
						} while (field != null);
					}
					classDeclare.put(sootClass.getType().getClassName(), className);
				}
			}
		}
	}

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		super.internalTransform(b, phaseName, options);
		addClassDeclare(b);
	}

	@Override
	protected void internalTransform(OutputStream outputStream, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options)
			throws IOException {
		
		if ("<init>".equals(method.getName())) {
			// ham khoi tao
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "utf-8");
			StringWriter bodyWriter = new StringWriter();
			int count = method.getParameterCount();
			bodyWriter.write("init(");
			if (count > 0) {
				for (int i = 0; i < count; i++) {
					if (i == 0) {
						bodyWriter.write("_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					} else {
						bodyWriter.write(",_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					}
				}
			}
			bodyWriter.write(") {\n");
			boolean override = compileBody(bodyWriter, sootClass, method, b, locals, patchingChain, phaseName, options);
			bodyWriter.write("}\n");
			if (override) {
				writer.write("override ");
			}
			writer.write(bodyWriter.toString());
			writer.flush();
			writer.close();
		} else {
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "utf-8");
			if (Modifier.toString(method.getModifiers()).contains("static")) {
				writer.write("static ");
			}
			int count = method.getParameterCount();
			if(sootClass.getSuperclass().declaresMethod(method.getSubSignature())) {
				writer.write(" override ");
			}
			writer.write("func ");
			writer.write(method.getName());
			writer.write("(");
			if (count > 0) {
				for (int i = 0; i < count; i++) {
					if (i == 0) {
						writer.write("_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					} else {
						writer.write(",_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					}
				}
			}
			writer.write(")");
			if (!method.getReturnType().toString().equalsIgnoreCase("void")) {
				writer.write("->");
				writer.write(fromJavaType(method.getReturnType()));
			}
			writer.write("{\n");
			compileBody(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
			writer.write("}\n");
			writer.flush();
			writer.close();
		}
	}

}
