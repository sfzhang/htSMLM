package main.embl.rieslab.htSMLM.threads;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.List;

import javax.swing.SwingWorker;

import main.embl.rieslab.htSMLM.algorithms.NMS;
import main.embl.rieslab.htSMLM.controller.SystemConstants;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

public class ActivationTask implements Task {

	public static int PARAM_SDCOEFF = 0;
	public static int PARAM_FEEDBACK = 1;
	public static int PARAM_CUTOFF = 2;
	public static int PARAM_AUTOCUTOFF = 3;
	public static int PARAM_dT = 4;
	public static int PARAM_N0 = 5;
	public static int PARAM_PULSE = 6;
	public static int PARAM_MAXPULSE = 7;
	public static int PARAM_ACTIVATE = 8;
	public static int OUTPUT_NEWCUTOFF = 0;
	public static int OUTPUT_N = 1;
	public static int OUTPUT_NEWPULSE = 2;
	public static int NUM_PARAMETERS = 9;
	public static int NUM_OUTPUTS = 3;
	
	private CMMCore core_;
	private TaskHolder holder_;
	private int idletime_;
	private AutomatedActivation worker_;
	private boolean running_ = false;  
	private Double[] output_;
	private ImageProcessor ip_;
	
	public ActivationTask(TaskHolder holder, CMMCore core, int idle){
		core_ = core;
		idletime_ = idle;
		
		registerHolder(holder);
		
		output_ = new Double[3];
		output_[0] = 0.;
		output_[1] = 0.;
		output_[2] = 0.;
	}
	
	@Override
	public void registerHolder(TaskHolder holder) {
		holder_ = holder;
	}

	@Override
	public void startTask() {
		worker_ = new AutomatedActivation();
		worker_.execute();
		running_ = true;
	}

	@Override
	public void stopTask() {
		running_ = false;
	}

	@Override
	public boolean isRunning() {
		return running_;
	}
	
	public void setIdleTime(int idle){
		idletime_ = idle;
	}

	private void getN(double sdcoeff, double cutoff, double dT,boolean autocutoff) {
		
		if (core_.isSequenceRunning() && core_.getBytesPerPixel() == 2) {
			int width, height;
			double tempcutoff;

			TaggedImage tagged1 = null, tagged2 = null;
			ShortProcessor ip, ip2;
			ImagePlus imp, imp2;
			ImageCalculator calcul = new ImageCalculator();
			ImagePlus imp3;
			GaussianBlur gau = new GaussianBlur();
			NMS NMSuppr = new NMS();

			width = (int) core_.getImageWidth();
			height = (int) core_.getImageHeight();

			int buffsize = core_.getImageBufferSize();
			System.out.println("[buffer] buffer size is: " + buffsize);

			if (buffsize > SystemConstants.FPGA_BIT_DEPTH) {
				try {
					tagged1 = core_.getLastTaggedImage();
					tagged2 = core_
							.getNBeforeLastTaggedImage(SystemConstants.FPGA_BIT_DEPTH);
				} catch (Exception e) {
					// exit?
				}

				ip = new ShortProcessor(width, height);
				ip2 = new ShortProcessor(width, height);

				ip.setPixels(tagged1.pix);
				ip2.setPixels(tagged2.pix);

				imp = new ImagePlus("", ip);
				imp2 = new ImagePlus("", ip2);

				// Subtraction
				imp3 = calcul.run("Substract create", imp, imp2);

				// Gaussian filter
				gau.blurGaussian(imp3.getProcessor(),
						SystemConstants.gaussianMaskSize,
						SystemConstants.gaussianMaskSize,
						SystemConstants.gaussianMaskPrecision);

				try {
					tempcutoff = imp3.getStatistics().mean + sdcoeff
							* imp3.getStatistics().stdDev;
				} catch (Exception e) {
					tempcutoff = cutoff;
				}

				double newcutoff;
				if (autocutoff) {
					newcutoff = (1 - 1 / dT) * cutoff + tempcutoff / dT;
				} else {
					newcutoff = cutoff;
					if (newcutoff == 0) {
						newcutoff = tempcutoff;
					}
				}

				ip_ = NMSuppr.run(imp3, SystemConstants.nmsMaskSize, newcutoff);
				output_[OUTPUT_NEWCUTOFF] = newcutoff;
				output_[OUTPUT_N] = (double) NMSuppr.getN();
			}
		}
	}
	
	private void getPulse(double feedback, double N0, double pulse, double maxpulse){
		double N = output_[OUTPUT_N];
		double temppulse=0;
		double min = 0.4;
		double npulse = 0;
		
		if(core_.isSequenceRunning()){			
			if(pulse < min){
				npulse = min;			
			} else {
				// avoid getting stuck between 0 and 1 (otherwise newp=0.4+0.4*1.99*coeff < 1 unless coeff ~> 0.7 
				// which is not good for higher values of the pulse)
				npulse = pulse;
			}

			// calculate new pulse
			if(N0 != 0){
				temppulse = npulse*(1+feedback*(1-N/N0));
			} else {
				output_[OUTPUT_NEWPULSE] = 0.;
				return;
			}

			if(temppulse < min){
				temppulse = min;
			}
	
			// if new pulse is higher than camera exposure
			double exp;
			try {
				exp = 1000*core_.getExposure();
				if(temppulse > exp) {
					temppulse = exp; 
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			npulse = temppulse;

		} else {
			npulse = pulse;
		}
		
		if(npulse > maxpulse){
			npulse = maxpulse;
		}
		
		output_[OUTPUT_NEWPULSE] = Math.floor(npulse);
	}
	
	
	@Override
	public void notifyHolder(Double[] outputs) {
		holder_.update(outputs);
	}
	
	public ImageProcessor getNMSResult(){
		return ip_;
	}
	
	private class AutomatedActivation extends SwingWorker<Integer, Double[]> {
		
		@Override
		protected Integer doInBackground() throws Exception {
			double[] params;
			
			while(running_){
				params = holder_.retrieveAllParameters();
				
				System.out.println("Max pulse is: "+params[PARAM_MAXPULSE]);
				
				// sanity checks here?
				if(params[PARAM_AUTOCUTOFF] == 1){
					getN(params[PARAM_SDCOEFF],params[PARAM_CUTOFF],params[PARAM_dT],true);
				} else {
					getN(params[PARAM_SDCOEFF],params[PARAM_CUTOFF],params[PARAM_dT],false);
				}
				
				if(params[PARAM_ACTIVATE] == 1){
					getPulse(params[PARAM_FEEDBACK],params[PARAM_N0],params[PARAM_PULSE],params[PARAM_MAXPULSE]);
				} else {
					output_[OUTPUT_NEWPULSE] = params[PARAM_PULSE];
				}
				
				publish(output_);
				
				Thread.sleep(idletime_);
			}
			return 1;
		}

		@Override
		protected void process(List<Double[]> chunks) {
			for(Double[] result : chunks){
				notifyHolder(result);
			}
		}
	}

	@Override
	public boolean isPausable() {
		return false;
	}

	@Override
	public void pauseTask() {
		// do nothing
	}

	@Override
	public void resumeTask() {
		// do nothing
	}
}
