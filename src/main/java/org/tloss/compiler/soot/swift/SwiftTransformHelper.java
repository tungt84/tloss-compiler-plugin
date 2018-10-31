package org.tloss.compiler.soot.swift;

import java.io.OutputStream;
import java.util.Map;

import org.tloss.compiler.soot.TransformHelper;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

public class SwiftTransformHelper extends TransformHelper {

	public String fromJavaType(RefType type) {
		return null;
	}

	@Override
	protected void internalTransform(OutputStream outputStream, SootClass sootClass, SootMethod method, Body b,
			Chain<Local> locals, PatchingChain<Unit> patchingChain, String phaseName, Map<String, String> options) {
		if ("<init>".equals(method.getName())) {
			//ham khoi tao
		}
	}

}
