package de.embl.rieslab.htsmlm.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class TimeChart {

	String name_, nameX_, nameY_;
	int width_, height_, maxN_, minY_ = 0, maxY_ = 100;
    private XYSeries series; 
    private double zero_ = 0;
    ChartPanel cp;
    int	time_counter = 0;
    boolean ranged_ = false;
    boolean zeroincluded_ = false;
    double lastpoint_=0;
    
	public TimeChart(String name, String nameX, String nameY, int maxN, int minY, int maxY, int width, int height){
		name_ = name;
		nameX_ = nameX;
		nameY_ = nameY;
		width_ = width;
		height_ = height;
		maxN_ = maxN;
		minY_ = minY;
		maxY_ = maxY;
		
		ranged_ = true;
		
		initialize();
	}

	public TimeChart(String name, String nameX, String nameY, int maxN, int width, int height, boolean zeroincluded){
		name_ = name;
		nameX_ = nameX;
		nameY_ = nameY;
		width_ = width;
		height_ = height;
		maxN_ = maxN;
		
		zeroincluded_ = zeroincluded; // autorange always include zero or not

		ranged_ = false;
		
		initialize();
	}
	
	public void initialize(){	
	    series = new XYSeries(name_);
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(null, null,
            null, dataset, PlotOrientation.VERTICAL, false, false, false);
        cp = new ChartPanel(chart) {

			private static final long serialVersionUID = 1447408262029551263L;

			@Override
            public Dimension getPreferredSize() {
                return new Dimension(width_, height_);
            }
        };

        chart.setBackgroundPaint(new Color(240,240,240));
        
    	XYPlot plot = (XYPlot) chart.getPlot();
    	plot.setBackgroundPaint(new Color(230,230,230));
    	plot.setDomainGridlinePaint(new Color(100,100,100));
    	plot.setRangeGridlinePaint(new Color(100,100,100));

    	XYItemRenderer renderer = plot.getRenderer();  
    	renderer.setSeriesPaint(0, new Color(255,91,91));    	
    	renderer.setSeriesStroke(0, new BasicStroke(2));
    	
    	ValueAxis xAxis = plot.getDomainAxis();
    	xAxis.setRange(0,maxN_);
    	xAxis.setVisible(false);
    	
    	plot.setDomainGridlinesVisible(false);
    	
    	NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    	
    	// Text string of the decimal format
    	NumberFormat formatter = new DecimalFormat("#0.00");
    	rangeAxis.setNumberFormatOverride(formatter);
    	
    	ValueAxis yAxis = plot.getRangeAxis();
        if(ranged_){
        	yAxis.setRange(minY_,maxY_);
        } else {
        	yAxis.setAutoRange(true);
        	rangeAxis.setAutoRangeIncludesZero(zeroincluded_);
        }
	}
	
	public ChartPanel getChart(){
		return cp;  
	}
	
	public void clearChart(){
		time_counter = 0;
		series.clear();
	}
	
	public void addPoint(double point){	
		point = point-zero_;

		time_counter++;
		if(time_counter == maxN_) {
			((XYPlot) cp.getChart().getPlot()).getDomainAxis().setAutoRange(true);
			series.remove(0);
		} else if(time_counter >= maxN_){
			series.remove(0);
		}
		series.add(time_counter, point);
		
		lastpoint_ = point;
	}
	
	public double getLastPoint(){
		return lastpoint_;
	}

	public void setZero(double z){
		zero_ = z;
	}
	
	public double getZero(){
		return zero_;
	}
}
