package main.java.de.embl.rieslab.htsmlm.updaters;

import javax.swing.JTextField;

import main.java.de.embl.rieslab.emu.ui.uiproperties.UIProperty;

public class JTextFieldUpdater extends ComponentUpdater<JTextField> {

	public JTextFieldUpdater(JTextField component, UIProperty prop, int idletime) {
		super(component, prop, idletime);
	}

	@Override
	public boolean sanityCheck(UIProperty prop) {
		return true;
	}

	@Override
	public void updateComponent(String val) {
		component_.setText(val);
	}

}