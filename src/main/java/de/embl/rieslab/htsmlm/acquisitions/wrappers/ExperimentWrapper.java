package de.embl.rieslab.htsmlm.acquisitions.wrappers;

import java.util.ArrayList;


public class ExperimentWrapper {

	public String name;
	public String path;
	public int pauseTime;
	public int numberPositions;
	public String savemode;
	public ArrayList<AcquisitionWrapper> acquisitionList;

	public ExperimentWrapper(){
		// necessary for JSON deserialization
	}
	
	public ExperimentWrapper(String name, String path, int pausetime, int numberpositions, String savemode, ArrayList<AcquisitionWrapper> acqwlist){
		this.name = name;
		this.path = path;
		this.pauseTime = pausetime;
		this.savemode=  savemode;
		this.numberPositions = numberpositions;
		this.acquisitionList = acqwlist;
	}

	public ExperimentWrapper(String name, String path, Experiment e){
		this.name = name;
		this.path = path;
		this.pauseTime = e.getPauseTime();
		this.savemode = e.getSaveModeAsString();
		this.numberPositions = e.getNumberPositions();
		this.acquisitionList = e.getAcquisitionWrapperList();
	}
}
