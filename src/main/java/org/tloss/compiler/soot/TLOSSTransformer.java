package org.tloss.compiler.soot;

import java.util.Map;

import soot.Body;

public class TLOSSTransformer extends soot.BodyTransformer {

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		System.out.println(b.getMethod().getDeclaringClass());
	}

//	@Override
//	protected void internalTransform(String phaseName, Map<String, String> options) {
//		SootClass sootClass = Scene.v().getSootClass("org.Test4");
//		List<SootMethod> methods = sootClass.getMethods();
//		for (SootMethod method : methods) {
//			System.out.println(method);
//		}
//	}

}
