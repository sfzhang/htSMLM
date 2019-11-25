package de.embl.rieslab.htsmlm.acquisitions.acquisitiontypes;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.micromanager.Studio;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.acquisition.internal.DefaultAcquisitionManager;
import org.micromanager.data.Datastore;

import de.embl.rieslab.emu.ui.uiproperties.TwoStateUIProperty;
import de.embl.rieslab.htsmlm.acquisitions.acquisitiontypes.AcquisitionFactory.AcquisitionType;
import de.embl.rieslab.htsmlm.acquisitions.uipropertyfilters.NoPropertyFilter;
import de.embl.rieslab.htsmlm.acquisitions.uipropertyfilters.PropertyFilter;
import de.embl.rieslab.htsmlm.tasks.TaskHolder;
import mmcorej.CMMCore;

public class MultiSliceAcquisition implements Acquisition {
	
	private GenericAcquisitionParameters params_;
	
	private final static String PANE_NAME = "Localization panel";
	private final static String LABEL_EXPOSURE = "Exposure (ms):";
	private final static String LABEL_PAUSE = "Pause (s):";
	private final static String LABEL_NUMFRAME = "Number of frames:";
	private final static String LABEL_INTERVAL = "Interval (ms):";
	private final static String LABEL_USEACTIVATION = "Use activation";
	private final static String LABEL_USESTOPONMAXUV = "Stop on max";
	private final static String LABEL_MAXUVTIME = "Stop on max delay (s):";
	private final static String LABEL_NLOOPS = "Number of loops";
	private final static String LABEL_NSLICES = "Number of slices";
	private final static String LABEL_DELTAZ = "Z difference (um)";
	private final static String LABEL_NUMB = "N loops / N slices / \u0394Z (um)";
	private final static String LABEL_ZDEVICE = "Moving device:";
	private final static String LABEL_ZTARGET = "Target device";
	private final static String LABEL_DISABLEFL = "disable focus-lock";
	private final static String LABEL_USETARGET = "use target";
	
	public final static String KEY_USEACT = "Use activation?";
	public final static String KEY_STOPONMAX = "Stop on max?";
	public final static String KEY_STOPDELAY = "Stop on max delay";
	public final static String KEY_NLOOPS = "N loops";
	public final static String KEY_NSLICES = "N slices";
	public final static String KEY_DELTAZ = "Delta z";
	public final static String KEY_ZDEVICE = "Z stage";
	public final static String KEY_ZTARGET = "Z target";
	public final static String KEY_DISABLEFL = "Disable focus-lock";
	public final static String KEY_USETARGET = "Use target device";
	
	private final static String NONE = "None";
	
	@SuppressWarnings("rawtypes")
	private TaskHolder activationTask_;
	private boolean useactivation_, stoponmax_, nullActivation_;
	private volatile boolean stopAcq_, running_;
	private int stoponmaxdelay_;
	private boolean interruptionRequested_;

	// UI property
	private TwoStateUIProperty zstabProperty_;
	private String zdevice_, targetdevice_;
	private String[] zdevices_;
	private double deltaZ;
	private int nSlices, nLoops;
	private boolean targetOtherZStage_, disableFocusLock_; 	
	
	@SuppressWarnings("rawtypes")
	public MultiSliceAcquisition(TaskHolder activationtask, double exposure, String[] zdevices, String defaultzdevice, TwoStateUIProperty zStabilizationProperty) {
		
		if(activationtask == null){
			nullActivation_ = true;
			useactivation_ = false;
		} else {
			nullActivation_ = false;
			useactivation_ = true;
			activationTask_ = activationtask;
		}
		
		stopAcq_ = false;
		running_ = false;
		interruptionRequested_ = false;
		stoponmax_ = true;
		stoponmaxdelay_ = 5;

		if(zStabilizationProperty != null && zStabilizationProperty.isAssigned()) {
			zstabProperty_ = zStabilizationProperty;
			disableFocusLock_ = true;
		} else {
			zstabProperty_ = null;
			disableFocusLock_ = false;
		}
		targetOtherZStage_ = false;
		targetdevice_ = NONE;
		deltaZ=2;
		nSlices=4;
		nLoops=5;

		zdevice_ = defaultzdevice;
		zdevices_ = zdevices;
		
		params_ = new GenericAcquisitionParameters(AcquisitionType.MULTISLICELOC, 
				exposure, 0, 3, 5000, new HashMap<String,String>(), new HashMap<String,String>());
	}

	@Override
	public JPanel getPanel() {
		JPanel pane = new JPanel();
		
		pane.setName(getPanelName());
		
		JLabel exposurelab, waitinglab, numframelab, intervallab, waitonmaxlab;
		JLabel zdevicelabel, ztargetlabel, numbLabel;
		JSpinner numberslice, deltaz, numberloops;
		JSpinner exposurespin, waitingspin, numframespin, intervalspin, waitonmaxspin;
		JCheckBox activatecheck, stoponmaxcheck, disablefocuslock, usetarget;
		
		exposurelab = new JLabel(LABEL_EXPOSURE);
		waitinglab = new JLabel(LABEL_PAUSE);
		numframelab = new JLabel(LABEL_NUMFRAME);
		intervallab = new JLabel(LABEL_INTERVAL);
		waitonmaxlab = new JLabel(LABEL_MAXUVTIME);
		
		exposurespin = new JSpinner(new SpinnerNumberModel(Math.max(params_.getExposureTime(),1), 1, 10000000, 1));
		exposurespin.setName(LABEL_EXPOSURE);
		waitingspin = new JSpinner(new SpinnerNumberModel(params_.getWaitingTime(), 0, 10000000, 1)); 
		waitingspin.setName(LABEL_PAUSE);
		numframespin = new JSpinner(new SpinnerNumberModel(params_.getNumberFrames(), 1, 10000000, 1)); 
		numframespin.setName(LABEL_NUMFRAME);
		intervalspin = new JSpinner(new SpinnerNumberModel(params_.getIntervalMs(), 0, 10000000, 1));
		intervalspin.setName(LABEL_INTERVAL);
		waitonmaxspin = new JSpinner(new SpinnerNumberModel(stoponmaxdelay_, 0, 10000, 1));
		waitonmaxspin.setName(LABEL_MAXUVTIME);

		activatecheck = new JCheckBox(LABEL_USEACTIVATION);
		activatecheck.setSelected(useactivation_);
		activatecheck.setEnabled(!nullActivation_);
		stoponmaxcheck = new JCheckBox(LABEL_USESTOPONMAXUV);
		stoponmaxcheck.setSelected(stoponmax_);
		stoponmaxcheck.setEnabled(!nullActivation_);
		activatecheck.setName(LABEL_USEACTIVATION);
		stoponmaxcheck.setName(LABEL_USESTOPONMAXUV);
		
		
		//// z part
		zdevicelabel = new JLabel(LABEL_ZDEVICE);
		ztargetlabel = new JLabel(LABEL_ZTARGET);
		numbLabel = new JLabel(LABEL_NUMB);


		numberloops = new JSpinner(new SpinnerNumberModel(nLoops, 1, 100, 1)); 
		numberloops.setName(LABEL_NLOOPS);
		numberslice = new JSpinner(new SpinnerNumberModel(nSlices, 2, 100, 1)); 
		numberslice.setName(LABEL_NSLICES);
		deltaz = new JSpinner(new SpinnerNumberModel(deltaZ, -1000, 1000, 0.5)); 
		deltaz.setName(LABEL_DELTAZ);
		
		JComboBox<String> zdevices = new JComboBox<String>(zdevices_);
		zdevices.setSelectedItem(zdevice_);
		zdevices.setName(LABEL_ZDEVICE);
		
		String[] ztargets_list = new String[zdevices_.length+1];
		ztargets_list[0] = NONE;
		for(int i=0;i<zdevices_.length;i++) {
			ztargets_list[i+1] = zdevices_[i];
		}
		JComboBox<String> ztargets = new JComboBox<String>(ztargets_list);
		if(zdevices_.length <= 1) {
			ztargets.setSelectedItem(NONE);
			ztargets.setName(LABEL_ZTARGET);
			ztargets.setEnabled(false);
		} else {
			ztargets.setSelectedItem(targetdevice_);
			ztargets.setName(LABEL_ZTARGET);
			ztargets.setEnabled(targetOtherZStage_);
		}
		
		disablefocuslock = new JCheckBox(LABEL_DISABLEFL);
		disablefocuslock.setSelected(disableFocusLock_);
		disablefocuslock.setName(LABEL_DISABLEFL);
		disablefocuslock.setEnabled(zstabProperty_ != null);
		
		usetarget = new JCheckBox(LABEL_USETARGET);
		usetarget.setName(LABEL_USETARGET);
		usetarget.setSelected(targetOtherZStage_);
		if(zdevices_.length <= 1) {
			usetarget.setEnabled(false);
		}

		usetarget.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
				boolean selected = abstractButton.getModel().isSelected();
				if (selected) {
					ztargets.setSelectedItem(NONE);
					ztargets.setEnabled(true);
				} else {
					ztargets.setSelectedItem(NONE);
					ztargets.setEnabled(false);
				}
			}
		});

		int nrow = 6;
		int ncol = 4;
		JPanel[][] panelHolder = new JPanel[nrow][ncol];    
		pane.setLayout(new GridLayout(nrow,ncol));
	
		for(int m = 0; m < nrow; m++) {
		   for(int n = 0; n < ncol; n++) {
		      panelHolder[m][n] = new JPanel();
		      pane.add(panelHolder[m][n]);
		   }
		}

		panelHolder[0][0].add(exposurelab);
		panelHolder[0][1].add(exposurespin);
		panelHolder[0][2].add(numframelab);
		panelHolder[0][3].add(numframespin);
		
		panelHolder[1][0].add(waitinglab);
		panelHolder[1][1].add(waitingspin);
		panelHolder[1][2].add(intervallab);
		panelHolder[1][3].add(intervalspin);

		panelHolder[2][0].add(waitonmaxlab);
		panelHolder[2][1].add(waitonmaxspin);
		panelHolder[2][2].add(stoponmaxcheck);
		panelHolder[2][3].add(activatecheck);

		panelHolder[3][0].add(zdevicelabel);
		panelHolder[3][1].add(zdevices);
		panelHolder[3][2].add(disablefocuslock);

		panelHolder[4][0].add(ztargetlabel);
		panelHolder[4][1].add(ztargets);
		panelHolder[4][2].add(usetarget);
		
		panelHolder[5][0].add(numbLabel);
		panelHolder[5][1].add(numberloops);
		panelHolder[5][2].add(numberslice);
		panelHolder[5][3].add(deltaz);
		
		return pane;
	}

	public void setUseActivation(boolean b){
		if(!nullActivation_){
			useactivation_  = b;
		} else {
			useactivation_  = false;
		}
	}

	public void setUseStopOnMaxUV(boolean b){
		stoponmax_ = b;
	}
	
	public void setUseStopOnMaxUVDelay(int delay){
		stoponmaxdelay_ = delay;
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
							} else if(comp[i].getName().equals(LABEL_NUMFRAME) && comp[i] instanceof JSpinner){
								params_.setNumberFrames((Integer) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_INTERVAL) && comp[i] instanceof JSpinner){
								params_.setIntervalMs((Double) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_USEACTIVATION) && comp[i] instanceof JCheckBox){
								this.setUseActivation(((JCheckBox) comp[i]).isSelected());
							} else if(comp[i].getName().equals(LABEL_USESTOPONMAXUV) && comp[i] instanceof JCheckBox){
								this.setUseStopOnMaxUV(((JCheckBox) comp[i]).isSelected());
							} else if(comp[i].getName().equals(LABEL_MAXUVTIME) && comp[i] instanceof JSpinner){
								this.setUseStopOnMaxUVDelay((Integer) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_ZDEVICE) && comp[i] instanceof JComboBox){
								zdevice_ = ((String) ((JComboBox) comp[i]).getSelectedItem());
							} else if(comp[i].getName().equals(LABEL_USETARGET) && comp[i] instanceof JCheckBox){
								targetOtherZStage_ = ((JCheckBox) comp[i]).isSelected();
							} else if(comp[i].getName().equals(LABEL_ZTARGET) && comp[i] instanceof JComboBox){
								targetdevice_ = ((String) ((JComboBox) comp[i]).getSelectedItem());
							} else if(comp[i].getName().equals(LABEL_DISABLEFL) && comp[i] instanceof JCheckBox){
								disableFocusLock_ = ((JCheckBox) comp[i]).isSelected();
							} else if(comp[i].getName().equals(LABEL_NSLICES) && comp[i] instanceof JSpinner){
								nSlices = ((Integer) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_NLOOPS) && comp[i] instanceof JSpinner){
								nLoops = ((Integer) ((JSpinner) comp[i]).getValue());
							} else if(comp[i].getName().equals(LABEL_DELTAZ) && comp[i] instanceof JSpinner){
								deltaZ = ((Double) ((JSpinner) comp[i]).getValue());
							}
						}
					}
				}
			}	
		}
		
		if(targetdevice_.equals(zdevice_)) {
			targetdevice_ = NONE;
			targetOtherZStage_ = false;
		}
	}

	@Override
	public PropertyFilter getPropertyFilter() {
		return new NoPropertyFilter();
	}

	@Override
	public String[] getHumanReadableSettings() {
		String[] s = new String[13];
		s[0] = "Exposure = "+params_.getExposureTime()+" ms";
		s[1] = "Interval = "+params_.getIntervalMs()+" ms";
		s[2] = "Number of frames = "+params_.getNumberFrames();
		s[3] = "Use activation = "+useactivation_;
		s[4] = "Stop on max UV = "+stoponmax_;
		s[5] = "Stop on max delay = "+stoponmaxdelay_+" s";
		s[6] = "Focus stage = "+zdevice_;
		s[7] = "Use target stage = "+targetOtherZStage_;
		s[8] = "Target stage = "+targetdevice_;
		s[9] = "Disable focus-lock = "+disableFocusLock_;
		s[10] = "Number of loops = "+nLoops;
		s[11] = "Number of slices = "+nSlices;
		s[12] = "Z difference = "+deltaZ+" um";
		return s;
	}

	@Override
	public String getPanelName() {
		return PANE_NAME;
	}
	
	@Override
	public String[][] getAdditionalParameters() {
		String[][] parameters = new String[10][2];

		parameters[0][0] = KEY_USEACT;
		parameters[0][1] = String.valueOf(useactivation_);
		parameters[1][0] = KEY_STOPONMAX;
		parameters[1][1] = String.valueOf(stoponmax_);
		parameters[2][0] = KEY_STOPDELAY;
		parameters[2][1] = String.valueOf(stoponmaxdelay_);
		parameters[3][0] = KEY_ZDEVICE;
		parameters[3][1] = zdevice_;
		parameters[4][0] = KEY_USETARGET;
		parameters[4][1] = String.valueOf(targetOtherZStage_);
		parameters[5][0] = KEY_ZTARGET;
		parameters[5][1] = targetdevice_;
		parameters[6][0] = KEY_DISABLEFL;
		parameters[6][1] = String.valueOf(disableFocusLock_);
		parameters[7][0] = KEY_NLOOPS;
		parameters[7][1] = String.valueOf(nLoops);
		parameters[8][0] = KEY_NSLICES;
		parameters[8][1] = String.valueOf(nSlices);
		parameters[9][0] = KEY_DELTAZ;
		parameters[9][1] = String.valueOf(deltaZ);

		return parameters;
	}
	
	@Override
	public void setAdditionalParameters(String[][] parameters) {
		if(parameters.length != 10 || parameters[0].length != 2) {
			throw new IllegalArgumentException("The parameters array has the wrong size: expected (10,2), got ("
					+ parameters.length + "," + parameters[0].length + ")");
		}

		useactivation_ = Boolean.parseBoolean(parameters[0][1]);
		stoponmax_ = Boolean.parseBoolean(parameters[1][1]);
		stoponmaxdelay_ = Integer.parseInt(parameters[2][1]);
		zdevice_ = parameters[3][1];
		targetOtherZStage_ = Boolean.parseBoolean(parameters[4][1]);
		targetdevice_ = parameters[5][1];
		disableFocusLock_ = Boolean.parseBoolean(parameters[6][1]);
		nLoops = Integer.parseInt(parameters[7][1]);
		nSlices = Integer.parseInt(parameters[8][1]);
		deltaZ = Double.parseDouble(parameters[9][1]);
	}

	@Override
	public GenericAcquisitionParameters getAcquisitionParameters() {
		return params_;
	}

	@Override
	public boolean performAcquisition(Studio studio, String name, String path) {
		
		CMMCore core  = studio.core();

		if(useactivation_){			
			activationTask_.initializeTask();
			activationTask_.resumeTask();
		}
		
		stopAcq_ = false;
		interruptionRequested_ = false;
		running_ = true;
		
		SequenceSettings settings = new SequenceSettings();
		settings.save = true;
		settings.timeFirst = true;
		settings.usePositionList = false;
		settings.root = path;
		settings.numFrames = params_.getNumberFrames();
		settings.intervalMs = 0;
		settings.shouldDisplayImages = true;
		
		// retrieve current z
		double z0 = 0, zt = 0, slope = 1.;
		try {
			z0 = core.getPosition(zdevice_);
		} catch (Exception e) {
			running_ = false;
			e.printStackTrace();
			return false;
		} // retrieves main stage position
		if(targetOtherZStage_) {
			try {
				zt = core.getPosition(targetdevice_);
		
				// assumes a linear relationship, so we quickly determine the slope
				double dz = Math.abs(0.1*z0) < 100 ? 10 : 100; 
				double z = z0 + dz;
				
				// set position
				core.setPosition(zdevice_, z);
				double zt2 = core.getPosition(targetdevice_);
				slope = (zt2-zt)/dz;
				
			} catch (Exception e) {
				running_ = false;
				e.printStackTrace();
				return false;
			}
		}
		
		if(running_) {
			for(int i=0;i<nLoops;i++) {
				for(int j=0;j<nSlices;j++) {
					// set z
					double z;
					if(targetOtherZStage_) {
						// we assume a linear relationship between the two stages
						z = z0 + j*deltaZ / slope;						
					} else {
						z  = z0 + j*deltaZ;
					}
					
					try {
						// move the stage
						core.setPosition(zdevice_, z);
						
						// set-up name
						settings.prefix = "L"+i+"S"+j+"_"+name;
						
						// run acquisition
						Datastore store = studio.acquisitions().runAcquisitionWithSettings(settings, false);

						// loop to check if needs to be stopped or not
						while(studio.acquisitions().isAcquisitionRunning()) {
							
							// check if reached stop criterion
							if(useactivation_ && stoponmax_ && activationTask_.isCriterionReached()){
								try {
									Thread.sleep(1000*stoponmaxdelay_);
								} catch (InterruptedException e) {
									e.printStackTrace();
									return false;
								}
												
								interruptAcquisition(studio);
								interruptionRequested_ = true;
							}
									
							// checks if exit
							if(stopAcq_){
								interruptAcquisition(studio);
								interruptionRequested_ = true;
							}
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
								return false;
							}
						}

						// close store
						studio.displays().closeDisplaysFor(store);
						try {
							store.close();
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}

						// pause activation
						if(useactivation_){			
							activationTask_.pauseTask();
						}
						
						
					} catch (Exception e) {
						running_ = false;
						e.printStackTrace();
					}
				}
			}
		}
		
		
		running_ = false;
		
		return true;
	}

	private void interruptAcquisition(Studio studio) {
		if(interruptionRequested_ == false) {
			try {
				// not pretty but I could not find any other way to stop the acquisition without getting a JDialog popping up and requesting user input
				((DefaultAcquisitionManager) studio.acquisitions()).getAcquisitionEngine().stop(true);
				
				//((DefaultAcquisitionManager) studio.acquisitions()).getAcquisitionEngine().abortRequested();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	public AcquisitionType getType() {
		return AcquisitionType.MULTISLICELOC;
	}

	@Override
	public String getShortName() {
		return "MultiSliceLoc";
	}
}
