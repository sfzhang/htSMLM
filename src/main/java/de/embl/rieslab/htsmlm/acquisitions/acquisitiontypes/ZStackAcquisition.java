package de.embl.rieslab.htsmlm.acquisitions.acquisitiontypes;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import de.embl.rieslab.emu.ui.uiproperties.TwoStateUIProperty;
import de.embl.rieslab.emu.utils.utils;
import de.embl.rieslab.htsmlm.acquisitions.acquisitiontypes.AcquisitionFactory.AcquisitionType;
import de.embl.rieslab.htsmlm.filters.NoPropertyFilter;
import de.embl.rieslab.htsmlm.filters.PropertyFilter;
import de.embl.rieslab.htsmlm.filters.SinglePropertyFilter;

import org.micromanager.Studio;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.acquisition.internal.DefaultAcquisitionManager;
import org.micromanager.data.Datastore;

public class ZStackAcquisition implements Acquisition {
	
	// Convenience constants		
	private final static String PANE_NAME = "Autofocus panel";
	private final static String LABEL_EXPOSURE = "Exposure (ms):";
	private final static String LABEL_PAUSE = "Pause (s):";
	private final static String LABEL_ZSTART = "Z start / Z end / Z step:";
	private final static String LABEL_ZEND = "Z end (um):";
	private final static String LABEL_ZSTEP = "Z step (um):";
	private final static String LABEL_ZDEVICE = "Z stage:";
	private final static String LABEL_CHECK = "Focus-lock";
		
	public final static String KEY_ZSTART = "Z start";
	public final static String KEY_ZEND = "Z end";
	public final static String KEY_ZSTEP = "Z step";
	public final static String KEY_ZDEVICE = "Z stage";
	public final static String KEY_USEFL = "Use focus-lock";
	
	// UI property
	private TwoStateUIProperty stabprop_;

	private String zdevice_;
	private String[] zdevices_;
	private double zstart, zend, zstep;
	private boolean zstab_, zstabuse_; 
	private GenericAcquisitionParameters params_;
	private volatile boolean stopAcq_, running_;
	
	public ZStackAcquisition(double exposure, String[] zdevices, String defaultzdevice, TwoStateUIProperty zStabilizationProperty) {

		if(zStabilizationProperty == null) {
			stabprop_ = null;
			zstab_ = false;
			zstabuse_ = false;
		} else {		
			if(zStabilizationProperty.isAssigned()){
				stabprop_ = zStabilizationProperty;
				zstab_ = true;
				zstabuse_ = true;
			} else {
				stabprop_ = null;
				zstab_ = false;
				zstabuse_ = false;
			}
		}
		
		zstart=-2;
		zend=2;
		zstep=0.05;

		zdevice_ = defaultzdevice;
		zdevices_ = zdevices;
		
		stopAcq_ = false;
		running_ = false;
		
		params_ = new GenericAcquisitionParameters(AcquisitionType.ZSTACK, 
				exposure, 0, 3, 1, new HashMap<String,String>(), new HashMap<String,String>(), getSlices(zstart, zend, zstep));
	}
	
	public ArrayList<Double> getSlices(double zstart, double zend, double zstep){
		ArrayList<Double> slices = new ArrayList<Double>();
		
		boolean invert;
		if(zstart < zend) {
			invert = false;
		} else {
			invert = true;
		}
		slices.add(zstart);

		double z = zstart;
		if(invert) {
			while (z>=utils.round(zend+zstep,2)){
				z -= zstep;
				slices.add(utils.round(z,2));
			}
		} else {
			while (z<=utils.round(zend-zstep,2)){
				z += zstep;
				slices.add(utils.round(z,2));
			}
		}

		return slices;
	}
	
	private void setSlices(double zstart, double zend, double zstep){
		params_.setZSlices(getSlices(zstart,zend,zstep));
	}
	
	@Override
	public GenericAcquisitionParameters getAcquisitionParameters() {
		return params_;
	}

	@Override
	public boolean performAcquisition(Studio studio, String name, String path) {
		stopAcq_ = false;
		running_ = true;
		
		if(zstab_ && zstabuse_){
			stabprop_.setPropertyValue(TwoStateUIProperty.getOffStateName());
		}
		
			
		SequenceSettings settings = new SequenceSettings();
		settings.save = true;
		settings.slicesFirst = true;
		settings.relativeZSlice = true;
		settings.slices = params_.getZSlices();
		
		double z0;
		try {
			z0 = studio.getCMMCore().getPosition(zdevice_);
			settings.zReference = z0;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		settings.usePositionList = false;
		settings.root = path;
		settings.prefix = name;
		settings.numFrames = 1;
		settings.intervalMs = 0;
		settings.shouldDisplayImages = true;

		// run acquisition
		Datastore store = studio.acquisitions().runAcquisitionWithSettings(settings, false);

		// loop to check if needs to be stopped or not
		while(studio.acquisitions().isAcquisitionRunning()) {
			// check if exit
			if(stopAcq_){
				interruptAcquisition(studio);
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		studio.displays().closeDisplaysFor(store);
		
		try {
			store.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		if(zstab_ && zstabuse_){
			stabprop_.setPropertyValue(TwoStateUIProperty.getOnStateName());
		}
		
		running_ = false;
		return true;
			
	}
	
	private void interruptAcquisition(Studio studio) {
		try {
			// not pretty but I could not find any other way to stop the acquisition without getting a JDialog popping up and requesting user input
			((DefaultAcquisitionManager) studio.acquisitions()).getAcquisitionEngine().stop(true);;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopAcquisition() {
		stopAcq_ = true;
	}

	@Override
	public boolean isRunning() {
		return running_;
	}

	@Override
	public boolean skipPosition() {
		return false;
	}

	@Override
	public JPanel getPanel() {
		JPanel pane = new JPanel();
		
		pane.setName(getPanelName());
		
		JLabel exposurelab, waitinglab, zstartlab, zdevicelabel;
		JSpinner exposurespin, waitingspin, zstartspin, zendspin, zstepspin;
		
		JCheckBox usefocuslocking = new JCheckBox(LABEL_CHECK);
		usefocuslocking.setName(LABEL_CHECK);
		usefocuslocking.setEnabled(zstab_);
		
		exposurelab = new JLabel(LABEL_EXPOSURE);
		waitinglab = new JLabel(LABEL_PAUSE);
		zstartlab = new JLabel(LABEL_ZSTART);
		zdevicelabel = new JLabel(LABEL_ZDEVICE);
		
		exposurespin = new JSpinner(new SpinnerNumberModel(Math.max(params_.getExposureTime(),1), 1, 10000, 1));
		exposurespin.setName(LABEL_EXPOSURE);
		waitingspin = new JSpinner(new SpinnerNumberModel(params_.getWaitingTime(), 0, 10000, 1)); 
		waitingspin.setName(LABEL_PAUSE);
		zstartspin = new JSpinner(new SpinnerNumberModel(zstart, -1000, 1000, 0.05)); 
		zstartspin.setName(LABEL_ZSTART);
		zendspin = new JSpinner(new SpinnerNumberModel(zend, -1000, 1000, 1)); 
		zendspin.setName(LABEL_ZEND);
		zstepspin = new JSpinner(new SpinnerNumberModel(zstep, -1000, 1000, 0.01));
		zstepspin.setName(LABEL_ZSTEP);
		
		JComboBox<String> zdevices = new JComboBox<String>(zdevices_);
		zdevices.setSelectedItem(zdevice_);
		zdevices.setName(LABEL_ZDEVICE);

		int nrow = 3;
		int ncol = 4;
		JPanel[][] panelHolder = new JPanel[nrow][ncol];    
		pane.setLayout(new GridLayout(nrow,ncol));

		for(int m = 0; m < nrow; m++) {
		   for(int n = 0; n < ncol; n++) {
		      panelHolder[m][n] = new JPanel();
		      pane.add(panelHolder[m][n]);
		   }
		}

		panelHolder[0][0].add(zdevicelabel);
		panelHolder[0][1].add(zdevices);
		panelHolder[0][2].add(usefocuslocking);
		
		panelHolder[1][0].add(exposurelab);
		panelHolder[2][0].add(zstartlab);
		
		panelHolder[1][1].add(exposurespin);
		panelHolder[2][1].add(zstartspin);
		
		panelHolder[1][2].add(waitinglab);
		panelHolder[2][2].add(zendspin);
		
		panelHolder[1][3].add(waitingspin);
		panelHolder[2][3].add(zstepspin);

		return pane;
	}

	@Override
	public void readOutAcquisitionParameters(JPanel pane) {
		if(pane.getName().equals(getPanelName())){
			Component[] pancomp = pane.getComponents();

			for(int j=0;j<pancomp.length;j++){
				if(pancomp[j] instanceof JPanel){
					Component[] comp = ((JPanel) pancomp[j]).getComponents();
					for(int i=0;i<comp.length;i++){
						if(!(comp[i] instanceof JLabel) && comp[i].getName() != null){
							if(comp[i].getName().equals(LABEL_EXPOSURE) && comp[i] instanceof JSpinner){
								params_.setExposureTime((Double) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_PAUSE) && comp[i] instanceof JSpinner){
								params_.setWaitingTime((Integer) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_ZSTART) && comp[i] instanceof JSpinner){
								zstart = ((Double) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_ZEND) && comp[i] instanceof JSpinner){
								zend = ((Double) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_ZSTEP) && comp[i] instanceof JSpinner){
								zstep = ((Double) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_ZDEVICE) && comp[i] instanceof JComboBox){
								zdevice_ = ((String) ((JComboBox) comp[i]).getSelectedItem());
							}else if(comp[i].getName().equals(LABEL_CHECK) && comp[i] instanceof JCheckBox){
								zstabuse_ = ((JCheckBox) comp[i]).isSelected();
							}
						}
					}
				}
			}	
			
			this.setSlices(zstart, zend, zstep);
		}
	}

	@Override
	public PropertyFilter getPropertyFilter() {
		if(stabprop_ == null){
			return new NoPropertyFilter();
		}
		return new SinglePropertyFilter(stabprop_.getPropertyLabel());
	}

	@Override
	public String[] getSpecialSettings() {
		String[] s = new String[6];
		s[0] = "Exposure = "+params_.getExposureTime()+" ms";
		s[1] = "Stage = "+zdevice_;
		s[2] = "Zstart = "+zstart+" um";
		s[3] = "Zend = "+zend+" um";
		s[4] = "Zstep = "+zstep+" um";
		s[5] = "Use focus-lock = "+String.valueOf(zstabuse_);
		return s;
	}

	@Override
	public String getPanelName() {
		return PANE_NAME;
	}
	
	@Override
	public String[][] getAdditionalJSONParameters() {
		String[][] s = new String[5][2];

		s[0][0] = KEY_ZSTART;
		s[0][1] = String.valueOf(zstart);
		s[1][0] = KEY_ZEND;
		s[1][1] = String.valueOf(zend);
		s[2][0] = KEY_ZSTEP;
		s[2][1] = String.valueOf(zstep);
		s[3][0] = KEY_ZDEVICE;
		s[3][1] = zdevice_;
		s[4][0] = KEY_USEFL;
		s[4][1] = String.valueOf(zstabuse_);
		
		return s;
	}

	public void setZRange(double zstart, double zend, double zstep){
		this.zstart = zstart;
		this.zend = zend;
		this.zstep = zstep;
		
		this.setSlices(zstart,zend,zstep);
	}

	public void setZDevice(String zdevice){
		zdevice_ = zdevice;
	}

	public void setUseFocusLock(boolean zstabuse){
		zstabuse_ = zstabuse;
	}
	
	@Override
	public AcquisitionType getType() {
		return AcquisitionType.ZSTACK;
	}

	@Override
	public String getShortName() {
		return "Z";
	}
}
