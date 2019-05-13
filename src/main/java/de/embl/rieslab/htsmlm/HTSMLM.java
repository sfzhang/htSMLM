package main.java.de.embl.rieslab.htsmlm;

import main.java.de.embl.rieslab.emu.controller.SystemController;
import main.java.de.embl.rieslab.emu.plugin.UIPlugin;
import main.java.de.embl.rieslab.emu.ui.ConfigurableMainFrame;

public class HTSMLM implements UIPlugin{

	@Override
	public ConfigurableMainFrame getMainFrame(SystemController controller) {
		return new MainFrame("ht-SMLM control center", controller);
	}

	@Override
	public String getName() {
		return "ht-SMLM";
	}

}