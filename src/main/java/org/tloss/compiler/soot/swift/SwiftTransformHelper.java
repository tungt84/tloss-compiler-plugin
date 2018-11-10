package org.tloss.compiler.soot.swift;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

import org.tloss.compiler.soot.TransformHelper;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.internal.AbstractInstanceFieldRef;
import soot.jimple.internal.IdentityRefBox;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.RValueBox;
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
			return className.replace('.', '_');
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
				writer.write("let " + (l.getName().replace('$', '_')) + ":" + fromJavaType(l.getType()) + ";\n");
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
			writer.append(abstractInstanceFieldRef.getBase().toString() + "."
					+ abstractInstanceFieldRef.getField().getName());
			return false;
		}
		writer.append(box.getValue().toString().replace('$', '_'));
		return false;
	}

	public boolean compileRValueBox(StringBuffer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, Unit unit, ValueBox box, boolean skipThis,
			String phaseName, Map<String, String> options) throws IOException {

		if (box instanceof IdentityRefBox) {
			IdentityRefBox identityRefBox = (IdentityRefBox) box;
			Value value = identityRefBox.getValue();
			if (value instanceof soot.jimple.ParameterRef) {
				writer.append("_parameter" + ((soot.jimple.ParameterRef) value).getIndex());
			} else {
				throw new IOException("Unsupport IdentityRefBox - Value Class:" + value.getClass() + ",Unit:" + unit);
			}
			return false;
		} else if (box instanceof RValueBox) {
			Value value = box.getValue();
			logger.info("compileRValueBox - value:" + value + ",valueClass:" + value.getClass());
			if (value instanceof Constant) {
				writer.append(box.getValue().toString());
				return false;
			} else if (value instanceof soot.jimple.internal.JNewExpr) {
				return true;
			} else if (value instanceof soot.jimple.internal.JimpleLocal) {
				writer.append(box.getValue().toString().replace('$', '_'));
				return false;
			} else if (value instanceof AbstractInstanceFieldRef) {
				AbstractInstanceFieldRef abstractInstanceFieldRef = (AbstractInstanceFieldRef) value;
				writer.append(abstractInstanceFieldRef.getBase().toString() + "."
						+ abstractInstanceFieldRef.getField().getName());
				return false;
			} else if (value instanceof soot.jimple.StaticFieldRef) {
				soot.jimple.StaticFieldRef staticFieldRef = (soot.jimple.StaticFieldRef) value;
				writer.append(
						fromJavaType(staticFieldRef.getField().getType()) + "." + staticFieldRef.getField().getName());
				return false;
			} else if (value instanceof soot.jimple.InstanceInvokeExpr) {
				soot.jimple.InstanceInvokeExpr invokeExpr = (soot.jimple.InstanceInvokeExpr) value;
				writer.append(invokeExpr.getBase().toString()).append(".")
						.append(invokeExpr.getMethodRef().name() + "(");
				int n = invokeExpr.getArgCount();
				if (n > 0) {
					for (int i = 0; i < n; i++) {
						if (i == 0) {
							writer.append(invokeExpr.getArg(i).toString().replace('$', '_'));
						} else {
							writer.append("," + invokeExpr.getArg(i).toString().replace('$', '_'));
						}
					}
				}
				writer.append(")");
				return false;
			} else if (value instanceof soot.jimple.internal.JCastExpr) {
				//TODO 
				return false;
			}else if (value instanceof soot.jimple.internal.JInstanceOfExpr) {
				//TODO 
				return false;
			}else {
				throw new IOException("Unsupport Value: " + value.getClass() + ",Unit:" + unit);
			}
		} else {
			throw new IOException("Unsupport ValueBox: " + box.getClass() + ",Unit:" + unit);
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
					invokeExpr.getBase().toString().replace('$', '_') + "." + invokeExpr.getMethodRef().name() + "(");
			int n = invokeExpr.getArgCount();
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					if (i == 0) {
						writer.write(invokeExpr.getArg(i).toString().replace('$', '_'));
					} else {
						writer.write("," + invokeExpr.getArg(i).toString().replace('$', '_'));
					}
				}
			}
			writer.write(");\n");
			return false;
		} else if (expr instanceof soot.jimple.internal.JSpecialInvokeExpr) {
			soot.jimple.internal.JSpecialInvokeExpr invokeExpr = (soot.jimple.internal.JSpecialInvokeExpr) expr;
			if (invokeExpr.getMethodRef().name().equals("<init>")) {
				if (invokeExpr.getBase().toString().equals("this")) {
					writer.write(invokeExpr.getBase().toString().replace('$', '_') + "= new "
							+ fromJavaType(invokeExpr.getMethodRef().declaringClass().toString().replace('$', '.'))
							+ "(");
					int n = invokeExpr.getArgCount();
					if (n > 0) {
						for (int i = 0; i < n; i++) {
							if (i == 0) {
								writer.write(invokeExpr.getArg(i).toString().replace('$', '_'));
							} else {
								writer.write("," + invokeExpr.getArg(i).toString().replace('$', '_'));
							}
						}
					}
					writer.write(");\n");
					return true;
				} else {
					writer.write(invokeExpr.getBase().toString().replace('$', '_') + "= new "
							+ fromJavaType(invokeExpr.getMethodRef().declaringClass().toString().replace('$', '.'))
							+ "(");
					int n = invokeExpr.getArgCount();
					if (n > 0) {
						for (int i = 0; i < n; i++) {
							if (i == 0) {
								writer.write(invokeExpr.getArg(i).toString().replace('$', '_'));
							} else {
								writer.write("," + invokeExpr.getArg(i).toString().replace('$', '_'));
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
							writer.write(invokeExpr.getArg(i).toString().replace('$', '_'));
						} else {
							writer.write("," + invokeExpr.getArg(i).toString().replace('$', '_'));
						}
					}
				}
				writer.write(");\n");
				return false;
			}
		} else {
			throw new IOException("Unsupport InvokeExpr: " + expr.getClass() + ",Unit:" + invokeStmt);
		}
	}

	public void compileIfStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, IfStmt ifStmt, String phaseName, Map<String, String> options)
			throws IOException {
		// TODO

	}

	public void compileGotoStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, GotoStmt ifStmt, String phaseName, Map<String, String> options)
			throws IOException {
		// TODO

	}

	public void compileReturnVoidStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, ReturnVoidStmt ifStmt, String phaseName, Map<String, String> options)
			throws IOException {
		// TODO

	}

	public boolean compilePatchingChain(Writer writer, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options)
			throws IOException {
		Unit unit = patchingChain.getFirst();
		boolean override = false;
		do {
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
				compileIfStmt(writer, sootClass, method, b, locals, patchingChain, ifStmt, phaseName, options);
			} else if (unit instanceof soot.jimple.internal.JGotoStmt) {
				soot.jimple.internal.JGotoStmt gotoStmt = (soot.jimple.internal.JGotoStmt) unit;
				compileGotoStmt(writer, sootClass, method, b, locals, patchingChain, gotoStmt, phaseName, options);
			} else if (unit instanceof ReturnVoidStmt) {
				ReturnVoidStmt returnStmt = (ReturnVoidStmt) unit;
				compileReturnVoidStmt(writer, sootClass, method, b, locals, patchingChain, returnStmt, phaseName, options);
			} else {
				throw new IOException("Unsupport Unit: " + unit.getClass() + ",Unit:" + unit);
			}
			unit = patchingChain.getSuccOf(unit);
		} while (unit != patchingChain.getLast());
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
		return compilePatchingChain(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
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
			int count = method.getParameterCount();
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
