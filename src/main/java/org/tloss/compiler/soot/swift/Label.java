package org.tloss.compiler.soot.swift;

import soot.Unit;

public class Label {
	Unit unit;
	String label;
	public Label(String label, Unit target) {
		this.label = label;
		this.unit = target;
	}
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
}
