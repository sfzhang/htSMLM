package main.embl.rieslab.htSMLM.ui.uiproperties;

import main.embl.rieslab.htSMLM.ui.MicroscopeControlUI.PropertyPanel;


public class TwoStateUIProperty extends UIProperty{
	
	public static String ON = "On value";
	public static String OFF = "Off value";
	private String onstate_ = "";
	private String offstate_ = "";
	
	public TwoStateUIProperty(PropertyPanel owner, String name, String description, PropertyFlag flag) {
		super(owner, name, description, flag);
	}

	@Override
	public boolean isTwoState(){
		return true;
	}
	
	public static String getOnStateName(){
		return " "+ON;
	}
	
	public static String getOffStateName(){
		return " "+OFF;
	}

	public void setOnStateValue(String on){
		onstate_ = on;
	}
	
	public void setOffStateValue(String off){
		offstate_ = off;
	}
	
	@Override
	public void setPropertyValue(String val) {
		if(isAllocated()) {
			if (val.equals(ON)) {
				getMMPoperty().setStringValue(onstate_, this);
			} else {
				getMMPoperty().setStringValue(offstate_, this);
			}
		}
	}

	public String[] getStates(){
		String[] s = {offstate_,onstate_};
		return s;
	}
	
	public String[] getStatesName(){
		String[] s = {OFF,ON};
		return s;
	}
}
