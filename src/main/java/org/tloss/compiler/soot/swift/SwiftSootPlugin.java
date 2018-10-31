package org.tloss.compiler.soot.swift;

import org.tloss.compiler.soot.SootPlugin;
import org.tloss.compiler.soot.TransformHelper;

public class SwiftSootPlugin extends SootPlugin {

	@Override
	public TransformHelper getTransformHelper() {
		return new SwiftTransformHelper();
	}

}
