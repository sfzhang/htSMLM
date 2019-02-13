package main.java.embl.rieslab.emu.ui;

import java.util.ArrayList;
import java.util.HashMap;

import main.java.embl.rieslab.emu.tasks.TaskHolder;
import main.java.embl.rieslab.emu.ui.uiparameters.UIParameter;
import main.java.embl.rieslab.emu.ui.uiproperties.UIProperty;

public interface ConfigurableFrame {

	
	public ArrayList<ConfigurablePanel> getPropertyPanels();
	
	public HashMap<String, UIProperty> getUIProperties();
	
	@SuppressWarnings("rawtypes")
	public HashMap<String, UIParameter> getUIParameters();
	
	@SuppressWarnings("rawtypes")
	public HashMap<String, TaskHolder> getUITaskHolders();
}