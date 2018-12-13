package org.tloss.compiler.soot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

public abstract class TransformHelper extends soot.BodyTransformer {

	private static Logger logger = Logger.getLogger(TransformHelper.class.getName());
	public static Map<String, Path> classNameMapping = new HashMap<String, Path>();

	protected abstract void internalTransform(OutputStream outputStream, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options)
			throws IOException;

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		SootMethod method = b.getMethod();
		SootClass sootClass = method.getDeclaringClass();
		try {
			Path dir;
			synchronized (classNameMapping) {
				if (!classNameMapping.containsKey(sootClass.getType().getClassName())) {
					dir = Files.createTempDirectory(null);
					classNameMapping.put(sootClass.getType().getClassName(), dir);
				} else {
					dir = classNameMapping.get(sootClass.getType().getClassName());
				}
			}

			StringBuffer buffer = new StringBuffer();
			buffer.append("-------------------------------\n");
			buffer.append("sootClass:").append(sootClass.getType().getClassName()).append("\n");
			buffer.append("methodName:").append(method.getName()).append("\n");
			buffer.append("isDeclared:").append(method.isDeclared()).append("\n");
			buffer.append("Modifiers:").append(method.getModifiers()).append("\n");
			buffer.append("ReturnType:").append(method.getReturnType()).append("\n");
			int count = method.getParameterCount();
			buffer.append("ParameterCount: ").append(count).append("\n");
			buffer.append("Parameter: ");
			for (int i = 0; i < count; i++) {
				if (i == 0) {
					buffer.append(method.getParameterType(i));
				} else {
					buffer.append(",").append(method.getParameterType(i));
				}
			}
			buffer.append("\n");
			buffer.append("directory:").append(dir.toUri()).append("\n");
			File f = Files.createTempFile(dir, null, null).toFile();
			buffer.append("file:").append(f.getAbsolutePath()).append("\n");
			FileOutputStream fileOutputStream = new FileOutputStream(f);

			Chain<Local> locals = b.getLocals();
			buffer.append("+++++++++++++++++++++++++++++++\n");
			buffer.append("locals:").append("\n");
			for (Local l : locals.getElementsUnsorted()) {
				buffer.append(l.getName()).append(":").append(l.getType()).append("\n");
			}

			PatchingChain<Unit> patchingChain = b.getUnits();
			buffer.append("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
			buffer.append("patchingChain:").append("\n");
			Unit unit = patchingChain.getFirst();
			do {
				buffer.append(unit).append("//").append(unit.getClass()).append("\n");
				unit = patchingChain.getSuccOf(unit);
			} while (unit != patchingChain.getLast());

			logger.info(buffer.toString());
			internalTransform(fileOutputStream, sootClass, method, b, locals, patchingChain, phaseName, options);
			fileOutputStream.flush();
			fileOutputStream.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error", e);
		}
	}
}
