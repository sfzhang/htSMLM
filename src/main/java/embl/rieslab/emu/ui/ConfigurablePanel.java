package main.java.embl.rieslab.emu.ui;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import main.java.embl.rieslab.emu.ui.internalproperty.InternalProperty;
import main.java.embl.rieslab.emu.ui.uiparameters.UIParameter;
import main.java.embl.rieslab.emu.ui.uiproperties.UIProperty;

/////////////////////
//
// need to maybe modify the API to not expose the UIProperties to the property panel is order to prevent EDT from lengthy calls?


@SuppressWarnings("rawtypes")
public abstract class ConfigurablePanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7664471329228929184L;

	private HashMap<String, UIProperty> properties_; 
	private HashMap<String, UIParameter> parameters_;
	private HashMap<String, InternalProperty> internalprops_;

	private String label_;
	
	private boolean propertychange_ = true;
	
	public ConfigurablePanel(String label){
		label_ = label;
		
		properties_ = new HashMap<String,UIProperty>();
		parameters_ = new HashMap<String,UIParameter>();
		internalprops_ = new HashMap<String, InternalProperty>();
		
		initializeProperties();
		initializeParameters();
		initializeInternalProperties();
		setupPanel();
	}

	public HashMap<String, UIProperty> getUIProperties(){
		return properties_;
	}
	
	public HashMap<String, InternalProperty> getInternalProperties(){
		return internalprops_;
	}
	
	public HashMap<String,UIParameter> getUIParameters(){
		return parameters_;
	}	
	
	public InternalProperty getInternalProperty(String name){
		if(internalprops_.containsKey(getLabel()+" "+name)){
			return internalprops_.get(getLabel()+" "+name);
		}
		return null;
	}	
	
	public void setUIPropertyFriendlyName(String name, String friendlyname){
		if(properties_.containsKey(name)){
			properties_.get(name).setFriendlyName(friendlyname);
		}
	}
	
	// maybe this should not be exposed to avoid modifying the property value on the EDT
	public UIProperty getUIProperty(String name){
		if(properties_.containsKey(name)){
			return properties_.get(name);
		}
		return null;
	}
	
	public String getUIPropertyValue(String name){
		if(properties_.containsKey(name)){
			return properties_.get(name).getPropertyValue();
		}
		return null;
	}
	
	public void setUIPropertyValue(String name, String value){
		// makes sure the call does NOT run on EDT
		Thread t = new Thread("Property change: " + name) {
			public void run() {
				if (properties_.containsKey(name)) {
					properties_.get(name).setPropertyValue(value);
				}
			}
		};
		t.start();
	}
	
	public UIParameter getUIParameter(String name){
		Iterator<String> it = parameters_.keySet().iterator();
		UIParameter param;
		while(it.hasNext()){
			param = parameters_.get(it.next());
			if(param.getLabel().equals(name)){
				return param;
			}
		}
		return null;
	}
	
	protected void addUIProperty(UIProperty p){
		properties_.put(p.getName(),p);
	}	

	protected void addUIParameter(UIParameter p){
		parameters_.put(p.getHash(),p);
	}
	
	protected void addInternalProperty(InternalProperty p){
		internalprops_.put(p.getHash(),p);
	}
	
	public void updateAllProperties(){
		Iterator<String> it = properties_.keySet().iterator();
		String prop;
		while(it.hasNext()){
			prop = it.next();
			triggerPropertyHasChanged(prop,properties_.get(prop).getPropertyValue());
		}
	}	
	
	public void updateAllParameters(){
		Iterator<String> it = parameters_.keySet().iterator();
		while(it.hasNext()){
			triggerParameterHasChanged(parameters_.get(it.next()).getLabel());
		}
	}
	
	public String getLabel(){
		return label_;
	}

	public void substituteParameter(String param, UIParameter uiParameter) {
		parameters_.remove(param);
		parameters_.put(param, uiParameter);
	}
	
	public boolean isPropertyChangeAllowed(){
		return propertychange_;
	}

	/**
	 * Upon loading the UI, the properties are updated according to the value of the linked MM property.
	 * Turning off the property change in the beginning of propertyhasChanged() allows changing the state
	 * of the UI components without triggering their own actionListeners. Note that it requires the UI
	 * components to call isPropertyChangeAllowed in their actionListeners. Before returning, propertyhasChanged
	 * need to turn on the property change again. 
	 */
	public void turnOffPropertyChange(){
		propertychange_ = false;
	}

	public void turnOnPropertyChange(){
		propertychange_ = true;
	}

	public void triggerPropertyHasChanged(final String name, final String newvalue){
		// Makes sure that the updating runs on EDT
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				propertyhasChanged(name, newvalue);
			}
		});
	}
	
	public void triggerParameterHasChanged(final String name){
		// Makes sure that the updating runs on EDT
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				parameterhasChanged(name);
			}
		});
	}
	
	protected abstract void initializeProperties();
	protected abstract void initializeInternalProperties();
	protected abstract void initializeParameters();
	public abstract void setupPanel();
	protected abstract void changeProperty(String name, String value);
	protected abstract void changeInternalProperty(String name, String value);
	
	/**
	 * Notifies the PropertyPanel that one of its UIProperty has changed. This function is called on the EDT.
	 * 
	 * @param name
	 * @param newvalue
	 */
	protected abstract void propertyhasChanged(String name, String newvalue);
	
	/**
	 * runs on edt
	 * 
	 * @param label
	 */
	protected abstract void parameterhasChanged(String label);
	public abstract void internalpropertyhasChanged(String label);
	public abstract void shutDown();
	public abstract String getDescription();

}