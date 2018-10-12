package main.embl.rieslab.mm.uidevint.ui.uiparameters;

import main.embl.rieslab.mm.uidevint.ui.PropertyPanel;
import main.embl.rieslab.mm.uidevint.utils.utils;

public class IntUIParameter extends UIParameter<Integer>{

	public IntUIParameter(PropertyPanel owner, String name, String description, int value) {
		super(owner, name, description);
		
		setValue(value);
	}

	@Override
	public void setType() {
		type_ = UIParameterType.INTEGER;
	}

	@Override
	public boolean isSuitable(String val) {
		if(utils.isInteger(val)){
			return true;
		}
		return false;
	}

	@Override
	protected Integer convertValue(String val) {
		return Integer.parseInt(val);
	}

	@Override
	public String getStringValue() {
		return String.valueOf(getValue());
	}

}