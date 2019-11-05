package com.keypointforensics.videotriage.chart;

import java.util.HashMap;
import java.util.Map.Entry;

import com.keypointforensics.videotriage.util.MapUtils;

public class HourOfDayBarChartBuilder extends VideoTriageChartBuilder {
	
	private final String JAVASCRIPT_FILENAME_SHORT = "hour_day_bar.js";
	private final String UNIQUE_DIV_NAME           = "HourBar";
	
	private HashMap<String, Integer> mEventMap;
	
	private StringBuilder mChartBuilder;
	private StringBuilder mHtmlBuilder;
	
	private boolean mFirstTimeEvent;
	private boolean mFirstTimeValue;
	
	public HourOfDayBarChartBuilder(final String reportRoot, final String reportPageHeader) {
		super(reportRoot, reportPageHeader);
		
		mEventMap      = new HashMap<String, Integer>();
		mChartBuilder  = new StringBuilder(1000);
		mHtmlBuilder   = new StringBuilder(2000);
		
		mFirstTimeEvent = true;
		mFirstTimeValue = true;
		
		prepareNewHourOfDayBarChart();
	}
	
	public void setJavaScriptFilenameShort() {
		mJavaScriptFilenameShort = JAVASCRIPT_FILENAME_SHORT;
	}
	
	private void prepareNewHourOfDayBarChart() {
		mChartBuilder.append("var layout={margin:{r:180,t:40,b:140,l:100},title:'Hour of Day Bar Chart',xaxis:{title:'Hour of Day',tickangle:45},yaxis:{title:'Number of Keypoints'}};var data=[{type:'bar',x:[");
	
		for(int i = 0; i < 24; ++i) {
			mEventMap.put(String.format("%02d", i), 0);
		}
	}
	
	public void addEvent(final String eventTime) {		
		final int indexOfSpace = eventTime.indexOf(" ");
		String hourOfDay = eventTime.substring(indexOfSpace + 1).replace("-", ":");
		hourOfDay = hourOfDay.substring(0, hourOfDay.indexOf(":"));		
		
		mEventMap.replace(hourOfDay, mEventMap.get(hourOfDay) + 1);
	}
	
	private void appendTimeEvent(final String event) {
		if(mFirstTimeEvent == false) {
			mChartBuilder.append(",'");
		} else {
			mChartBuilder.append("'");
			mFirstTimeEvent = false;
		}
			
		mChartBuilder.append(event);
		mChartBuilder.append("'");
	}
	
	private void closeTimeEvents() {
		mChartBuilder.append("],y: [");
	}
	
	private void appendTimeValue(final int value) {
		if(mFirstTimeValue == false) {
			mChartBuilder.append(",");
		} else {
			mFirstTimeValue = false;
		}
			
		mChartBuilder.append(value);
	}
	
	private void closeTimeValues() {
		mChartBuilder.append("]");
	}
	
	private void closeTimeSeriesChart(boolean isReportPaginated) {
		if(isReportPaginated == true) {
			mChartBuilder.append("}]; Plotly.newPlot('myDiv', data, layout);");
		} else {
			mChartBuilder.append("}]; Plotly.newPlot('myDiv" + UNIQUE_DIV_NAME + "', data, layout);");
		}
	}
	
	private void buildHtmlFileContents(boolean isReportPaginated) {
		if(isReportPaginated == true) {
			final int indexOfHtmlHead = REPORT_PAGE_HEADER.indexOf("<head>") + 6;
			String startOfHeader = REPORT_PAGE_HEADER.substring(0, indexOfHtmlHead);
			String restOfHeader = REPORT_PAGE_HEADER.substring(indexOfHtmlHead, REPORT_PAGE_HEADER.length());
				
			mHtmlBuilder.append(startOfHeader);
			mHtmlBuilder.append("<script src=\"chart.js\"></script>");
			mHtmlBuilder.append(restOfHeader);
			mHtmlBuilder.append("<div id=\"myDiv\"></div><script src=\"");
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
	
	public void build(boolean isReportPaginated) {	
		mEventMap = (HashMap<String, Integer>) MapUtils.sortByKey(mEventMap);
		
		for(Entry<String, Integer> hourEntry : mEventMap.entrySet()) {
			appendTimeEvent(hourEntry.getKey());
		}
		
		closeTimeEvents();

		for(Entry<String, Integer> hourEntry : mEventMap.entrySet()) {
			appendTimeValue(hourEntry.getValue());
		}
		
		closeTimeValues();
		
		closeTimeSeriesChart(isReportPaginated);
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
