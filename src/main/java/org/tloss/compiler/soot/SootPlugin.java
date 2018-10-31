package org.tloss.compiler.soot;

import soot.Transformer;
import soot.plugins.SootPhasePlugin;
import soot.plugins.model.PhasePluginDescription;

public abstract class SootPlugin implements SootPhasePlugin{

	public String[] getDeclaredOptions() {
		return  new String [] {"opt"};
	}

	public String[] getDefaultOptions() {
		
		return new String [] {ENABLED_BY_DEFAULT, "opt:false"};
	}
	
	public abstract TransformHelper getTransformHelper();
	
	public Transformer getTransformer() {
		return getTransformHelper();
	}

	public void setDescription(PhasePluginDescription pluginDescription) {
		
	}

}
