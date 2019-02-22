package main.java.embl.rieslab.emu.ui.uiparameters;

import main.java.embl.rieslab.emu.ui.ConfigurablePanel;
import main.java.embl.rieslab.emu.ui.uiparameters.UIParameter;
import main.java.embl.rieslab.emu.utils.utils;

/**
 * UIParameter holding a double value.
 * 
 * @author Joran Deschamps
 *
 */
public class DoubleUIParameter extends UIParameter<Double> {

	public DoubleUIParameter(ConfigurablePanel owner, String name, String description, double val) {
		super(owner, name, description);

		setValue(val);
	}

	
	/**
	 * @inheritDoc
	 */
	@Override
	public UIParameterType getType() {
		return UIParameterType.DOUBLE;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isSuitable(String val) {
		if(utils.isNumeric(val)){
			return true;
		}
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	protected Double convertValue(String val) {
		return Double.parseDouble(val);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public String getStringValue() {
		return String.valueOf(getValue());
	}

}
