package org.tloss.compiler.soot;

import soot.Transformer;
import soot.plugins.SootPhasePlugin;
import soot.plugins.model.PhasePluginDescription;

public class SootPlugin implements SootPhasePlugin{

	public String[] getDeclaredOptions() {
		return  new String [] {"opt"};
	}

	public String[] getDefaultOptions() {
		
		return new String [] {ENABLED_BY_DEFAULT, "opt:false"};
	}

	public Transformer getTransformer() {
		return new TLOSSTransformer();
	}

	public void setDescription(PhasePluginDescription pluginDescription) {
		
	}

}
