package com.keypointforensics.videotriage.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import com.keypointforensics.videotriage.blob.context.BlobContextList;
import com.keypointforensics.videotriage.gui.gallery.ChartBackgroundSelectorImageGallery;
import com.keypointforensics.videotriage.util.MapUtils;
import com.keypointforensics.videotriage.util.ThreadUtils;
import com.keypointforensics.videotriage.util.Utils;

public class HistogramContourBackgroundChartBuilder extends VideoTriageChartBuilder {
	
	private final String JAVASCRIPT_FILENAME_SHORT    = "histogram_contour_background.js";
	private final String UNIQUE_DIV_NAME              = "HistogramContourBackground";
	//private final int    MAXIMUM_MARKER_SIZE          = 50;
	private final int    MAXIMUM_MARKER_SIZE_ADJUSTED = 45;
	private final int    MINIMUM_MARKER_SIZE          = 8;
	private final int    MAXIMUM_COORDINATE_COUNT     = 10000;
	//private final double WEIGHT_NORMALIZATION_MINIMUM = 0.0;
	//private final double WEIGHT_NORMALIZATION_MAXIMUM = 1.0;
	
	private ArrayList<Integer> mCoordXList;
	private ArrayList<Integer> mCoordYList;
	
	private ArrayList<Integer> mFrameWidthList;
	private ArrayList<Integer> mFrameHeightList;
	
	private StringBuilder mChartBuilder;
	private StringBuilder mHtmlBuilder;
	
	private boolean mFirstTimeEvent;
	private boolean mFirstTimeValue;
	
	private HashMap<Integer, Double> mYCoordWeightMap;
	
	private ArrayList<String> mProcessedVideoList;
	
	public HistogramContourBackgroundChartBuilder(final String reportRoot, final String reportPageHeader, final ArrayList<String> listOfProcessedVideos) {
		super(reportRoot, reportPageHeader);
		
		mCoordXList      = new ArrayList<Integer>();
		mCoordYList      = new ArrayList<Integer>();
		mChartBuilder    = new StringBuilder(1000);
		mHtmlBuilder     = new StringBuilder(2000);
		mFrameWidthList  = new ArrayList<Integer>();
		mFrameHeightList = new ArrayList<Integer>();
		
		mYCoordWeightMap = new HashMap<Integer, Double>();
		
		mFirstTimeEvent = true;
		mFirstTimeValue = true;
		
		mProcessedVideoList = listOfProcessedVideos;
		
		prepareNewHistogramContourChart();
	}
	
	public void setJavaScriptFilenameShort() {
		mJavaScriptFilenameShort = JAVASCRIPT_FILENAME_SHORT;
	}

	private void prepareNewHistogramContourChart() {
		ChartBackgroundSelectorImageGallery chartBackgroundSelectImageGallery = new ChartBackgroundSelectorImageGallery(mProcessedVideoList);
		chartBackgroundSelectImageGallery.build();

		Utils.displayMessageDialog("Select Background", "Please select a background image for your histogram contour chart.");
		
		Thread selectBackgroundImageThread = new Thread() {
			@Override
			public void run() {
				ThreadUtils.addThreadToHandleList("ImageGallery Exit", this);
				
				while(chartBackgroundSelectImageGallery.isVisible()) {
					Thread.yield();
				}
				
				ThreadUtils.removeThreadFromHandleList(this);
			}
		};
		
		selectBackgroundImageThread.start();
		
		try {
			selectBackgroundImageThread.join();
		} catch (InterruptedException interruptedException) {
			//interruptedException.printStackTrace();
		}
		
		final String backgroundImageBase64Data = chartBackgroundSelectImageGallery.getBase64ImageData();
		
		if(backgroundImageBase64Data != null) {
			mChartBuilder.append("var layout={images:[{\r\n\"source\": \"data:image/png;base64,");
			mChartBuilder.append(backgroundImageBase64Data);
			mChartBuilder.append("\",\r\n\"xref\": \"x\",\r\n\"yref\": \"y\",\r\n\"x\": 0,\r\n\"y\": ");
			mChartBuilder.append(chartBackgroundSelectImageGallery.getSelectedImageHeight());
			mChartBuilder.append(",\r\n\"sizex\": ");
			mChartBuilder.append(chartBackgroundSelectImageGallery.getSelectedImageWidth());
			mChartBuilder.append(",\r\n\"sizey\": ");
			mChartBuilder.append(chartBackgroundSelectImageGallery.getSelectedImageHeight());
			mChartBuilder.append(",\r\n\"sizing\": \"stretch\",");
			//"\"opacity\": 0.4,\r\n" + 
			mChartBuilder.append("\"layer\": \"below\"}],margin:{r:180,t:40,b:140,l:100},title:'Histogram Contour Plot',xaxis:{title:'X Coordinate',tickangle:45},yaxis:{title:'Y Coordinate'}};var data=[{type:'histogram2dcontour',mode:'markers',x:[");
		} else {
			mChartBuilder.append("var layout={margin:{r:180,t:40,b:140,l:100},title:'Histogram Contour Plot',xaxis:{title:'X Coordinate',tickangle:45},yaxis:{title:'Y Coordinate'}};var data=[{type:'histogram2dcontour',mode:'markers',x:[");
		}
	}
	
	public void addEvent(final String coordEvent) {		
		final int indexOfSpace = coordEvent.indexOf(" ");
		int x = Integer.parseInt(coordEvent.substring(0, indexOfSpace));
		int y = Integer.parseInt(coordEvent.substring(indexOfSpace + 1, coordEvent.length()));
				
		mCoordXList.add(x);
		mCoordYList.add(y);
		
		if(mYCoordWeightMap.containsKey(x) == true) {
			mYCoordWeightMap.replace(x, mYCoordWeightMap.get(x) + 1);
		} else {
			mYCoordWeightMap.put(x, 1.0);
		}
	}
	
	public void addFrameBound(String frameBound) {
		frameBound = frameBound.substring(BlobContextList.FRAME_BOUNDARY_TAG.length(), frameBound.length());
		
		final int frameWidth = Integer.parseInt(frameBound.substring(0, frameBound.indexOf(" ")));
		final int frameHeight = Integer.parseInt(frameBound.substring(frameBound.indexOf(" ") + 1, frameBound.length()));
		
		mFrameWidthList.add(frameWidth);
		mFrameHeightList.add(frameHeight);
	}
	
	private void fixYCoordEvents() {
		//final int maximumYCoord = Collections.max(mCoordYList);
		final int maximumYBound = Collections.max(mFrameHeightList);

		for(int i = 0; i < mCoordYList.size(); ++i) {
			mCoordYList.set(i, maximumYBound - mCoordYList.get(i));
		}
	}
	
	private void appendXCoordEvent(final int xCoord) {
		if(mFirstTimeEvent == false) {
			mChartBuilder.append(",'");
		} else {
			mChartBuilder.append("'");
			mFirstTimeEvent = false;
		}
			
		mChartBuilder.append(xCoord);
		mChartBuilder.append("'");
	}
	
	private void closeXCoordEvents() {
		mChartBuilder.append("],y: [");
	}
	
	private void appendYCoordEvent(final int yCoord) {
		if(mFirstTimeValue == false) {
			mChartBuilder.append(",");
		} else {
			mFirstTimeValue = false;
		}
			
		mChartBuilder.append(yCoord);
	}
	
	private void normalizeWeightMap(final double normalizationMinimum, final double normalizationMaximum) {
		final double maximumMinusMinimum = normalizationMaximum - normalizationMinimum;
		
		for(Entry<Integer, Double> weightEntry : mYCoordWeightMap.entrySet()) {
			weightEntry.setValue(normalizeWeight(weightEntry.getValue(), maximumMinusMinimum, normalizationMinimum, normalizationMaximum));//WEIGHT_NORMALIZATION_MINIMUM, WEIGHT_NORMALIZATION_MAXIMUM));
		}
	}
	
	private double normalizeWeight(double value, double maximumMinusMinimum, double minimum, double maximum) {
	    return 1.0 / (maximumMinusMinimum) * (value - maximum) + 1.0; 
	}
	
	private void appendMarkerSize(final double value) {
		if(mFirstTimeValue == false) {
			mChartBuilder.append(",");
		} else {
			mFirstTimeValue = false;
		}
			
		int proposedSize = (int) Math.round(((value * MAXIMUM_MARKER_SIZE_ADJUSTED) + MINIMUM_MARKER_SIZE));
		
		/*
		if(proposedSize > 0) {
			proposedSize += MINIMUM_MARKER_SIZE;
		}
		
		if(proposedSize > MAXIMUM_MARKER_SIZE) {
			proposedSize = MAXIMUM_MARKER_SIZE;
		}
		*/
		
		mChartBuilder.append(proposedSize);
	}
	
	private void closeYCoordEvents() {
		mChartBuilder.append("]");
	}
	
	private void closeHistogramContourChart(boolean isReportPaginated) {
		if(isReportPaginated == true) {
			mChartBuilder.append(",colorscale: [[0, 'rgba(255, 255,0,0)'], [1, 'rgb(227,26,28)']]}]; Plotly.newPlot('myDiv', data, layout);");
		} else {
			mChartBuilder.append(",colorscale: [[0, 'rgba(255, 255,0,0)'], [1, 'rgb(227,26,28)']]}]; Plotly.newPlot('myDiv" + UNIQUE_DIV_NAME + "', data, layout);");
		}
	}
	
	private void buildHtmlFileContents(boolean isReportPaginated) {
		if(isReportPaginated == true) {
			final int indexOfHtmlHead = REPORT_PAGE_HEADER.indexOf("<head>") + 6;
			String startOfHeader = REPORT_PAGE_HEADER.substring(0, indexOfHtmlHead);
			String restOfHeader = REPORT_PAGE_HEADER.substring(indexOfHtmlHead, REPORT_PAGE_HEADER.length());
			// height 75% only for paginated
			mHtmlBuilder.append(startOfHeader);
			mHtmlBuilder.append("<script src=\"chart.js\"></script>");
			mHtmlBuilder.append(restOfHeader);
			mHtmlBuilder.append("<div id=\"myDiv\" style=\"height: 75%;\"></div><script src=\"");
			mHtmlBuilder.append(JAVASCRIPT_FILENAME_SHORT);
			mHtmlBuilder.append("\"></script>");//</body><html>");
			
			mHtmlBuilder.append("<br><div align=\"center\"><a href=\"");
			mHtmlBuilder.append(REPORT_ROOT);
			mHtmlBuilder.append("\">Home</a></div></body></html>");
		} else {
			mHtmlBuilder.append("<div id=\"myDiv" + UNIQUE_DIV_NAME + "\" style=\"height: 75%;\"></div><script src=\"");
			mHtmlBuilder.append(JAVASCRIPT_FILENAME_SHORT);
			mHtmlBuilder.append("\"></script>");//</body><html>");
			
			mHtmlBuilder.append("<br>");
		}
	}
	
	private void trimCoordEventsToMaximum() {
		int randomNum;
		
		while(mCoordXList.size() > MAXIMUM_COORDINATE_COUNT) {
			 randomNum = ThreadLocalRandom.current().nextInt(0, MAXIMUM_COORDINATE_COUNT + 1);
			 
			 mCoordXList.remove(randomNum);
			 mCoordYList.remove(randomNum);
		}
	}
	
	public void build(boolean isReportPaginated) {
		trimCoordEventsToMaximum();
		
		fixYCoordEvents();
		
		for(int i = 0; i < mCoordXList.size(); ++i) {
			appendXCoordEvent(mCoordXList.get(i));
		}
		
		closeXCoordEvents();

		for(int i = 0; i < mCoordYList.size(); ++i) {
			appendYCoordEvent(mCoordYList.get(i));
		}
		
		closeYCoordEvents();
		
		final double maximumWeightValue = MapUtils.getMaximum(mYCoordWeightMap);
		final double minimumWeightValue = MapUtils.getMinimum(mYCoordWeightMap);
		
		normalizeWeightMap(minimumWeightValue, maximumWeightValue);
		
		mFirstTimeValue = true;
		mChartBuilder.append(",marker:{size:[");
		for(int i = 0; i < mCoordXList.size(); ++i) {
			appendMarkerSize(mYCoordWeightMap.get(mCoordXList.get(i)));
		}
		mChartBuilder.append("]}");
		
		closeHistogramContourChart(isReportPaginated);
		buildHtmlFileContents(isReportPaginated);
	}
	
	public String getChart() {
		return mChartBuilder.toString();
	}

	@Override
	public String getHtmlFile() {		
		return mHtmlBuilder.toString();
	}
}
