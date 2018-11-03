package org.tloss.compiler.soot.swift;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import soot.ValueBox;
import soot.jimple.IdentityStmt;
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
				writer.write("let " + (l.getName().replace('$', '_')) + ":" + fromJavaType(l.getType()) + "\n");
		}
	}

	public boolean compileValueBox(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, ValueBox box, String phaseName, Map<String, String> options)
			throws IOException {
		logger.info(box.toString());
		return true;
	}

	public void compileIdentityStmt(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, IdentityStmt identityStmt, String phaseName, Map<String, String> options)
			throws IOException {
		ValueBox left = identityStmt.getLeftOpBox();
		ValueBox right = identityStmt.getRightOpBox();
		boolean skip = compileValueBox(writer, sootClass, method, b, locals, patchingChain, left, phaseName, options);
		if (!skip) {
			writer.write("=");
			compileValueBox(writer, sootClass, method, b, locals, patchingChain, right, phaseName, options);
		}
	}

	public void compilePatchingChain(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options) throws IOException {
		Unit unit = patchingChain.getFirst();
		do {
			unit = patchingChain.getSuccOf(unit);
			if (unit instanceof IdentityStmt) {
				IdentityStmt identityStmt = (IdentityStmt) unit;
				// System.out.println("----"+identityStmt.getLeftOpBox() + ": " +
				// identityStmt.getRightOpBox()+unit);
				compileIdentityStmt(writer, sootClass, method, b, locals, patchingChain, identityStmt, phaseName,
						options);
			}
		} while (unit != patchingChain.getLast());
	}

	public void compileBody(Writer writer, SootClass sootClass, SootMethod method, Body b, Chain<Local> locals,
			PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options) throws IOException {
		defineLocals(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
		compilePatchingChain(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
	}

	@Override
	protected void internalTransform(OutputStream outputStream, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options)
			throws IOException {
		if ("<init>".equals(method.getName())) {
			// ham khoi tao
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "utf-8");
			int count = method.getParameterCount();
			writer.write("init(");
			if (count > 0) {
				for (int i = 0; i < count; i++) {
					if (i == 0) {
						writer.write("_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					} else {
						writer.write(",_ _parameter" + i + ":" + fromJavaType(method.getParameterType(i)));
					}
				}
			}
			writer.write(") {\n");
			compileBody(writer, sootClass, method, b, locals, patchingChain, phaseName, options);
			writer.write("}\n");
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
