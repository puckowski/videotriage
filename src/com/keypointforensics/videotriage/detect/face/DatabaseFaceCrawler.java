package com.keypointforensics.videotriage.detect.face;

import java.io.File;
import java.util.ArrayList;

import com.keypointforensics.videotriage.detect.DetectionModule;
import com.keypointforensics.videotriage.detect.HaarDetector;
import com.keypointforensics.videotriage.detect.SimpleFalsePositiveRemover;
import com.keypointforensics.videotriage.gui.main.GuiMain;
import com.keypointforensics.videotriage.legacy.FileUtilsLegacy;
import com.keypointforensics.videotriage.progress.ProgressBundle;
import com.keypointforensics.videotriage.util.CascadeUtils;
import com.keypointforensics.videotriage.util.CursorUtils;
import com.keypointforensics.videotriage.util.FileUtils;

public class DatabaseFaceCrawler extends DetectionModule {

	/*
	 * Author: Daniel Puckowski
	 */
						
	public DatabaseFaceCrawler(final GuiMain guiMain, final String databaseName) {		
		super(guiMain, databaseName);
	}

	protected void createDetectionDatabaseFolderIfNecessary() {
		String databaseNameShort = FileUtils.getShortFilename(DATABASE_NAME);
		
		File databaseFolder = new File(FileUtils.FACES_DIRECTORY + databaseNameShort);
		
		if(databaseFolder.exists() == false) {
			databaseFolder.mkdir();
		}
		
		mDatabasePath = databaseFolder.getAbsolutePath() + File.separator;
	}
	
	public void performDatabaseCrawlAction() {
		CursorUtils.setBusyCursor(GUI_MAIN);
		
		String capturesDirectory = getCaptureDirectoryFromDatabasePath();
		
		ArrayList<String> allCapturePaths = (ArrayList<String>) FileUtilsLegacy.parseDirectoryRecursiveForAll(capturesDirectory);
	
		ProgressBundle searchProgressBundle = null;
		
		mHaarDetector = new HaarDetector(mDatabasePath, mNumberOfCpuCores, mMinimumFaceArea);
		
		if(mExhaustiveSearch == false) {
			searchProgressBundle = mHaarDetector.performStandardSearch(CascadeUtils.HAAR_CASCADE_FACE_DEFAULT, searchProgressBundle, allCapturePaths);
		} else {
			searchProgressBundle = mHaarDetector.performExhaustiveSearch(CascadeUtils.getCompleteFaceHaarCascadeList(), searchProgressBundle, allCapturePaths);
		}
	     
		searchProgressBundle.progressBar.setIndeterminate(true);
		searchProgressBundle.progressBar.repaint();
		
		try {
			Thread.sleep(DEFAULT_SEARCH_SLEEP_MILLISECONDS);
		} catch (InterruptedException interruptedException) {
			interruptedException.printStackTrace();
		}

		if(mPreAnalyzeResults == true && mCustomSimilarityPercent != NO_AUTOMATIC_GENTLE_FILTER) {
			ArrayList<String> allFacePaths = (ArrayList<String>) FileUtilsLegacy.parseDirectoryRecursiveForAll(mDatabasePath);

			searchProgressBundle.progressBar.setIndeterminate(false);
			searchProgressBundle.frame.setTitle("Reviewing Results...");
			searchProgressBundle.progressBar.setValue(0);
			searchProgressBundle.progressBar.setMaximum(allFacePaths.size() + 1);
			searchProgressBundle.progressBar.repaint();
			
			SimpleFalsePositiveRemover simpleFalsePositiveRemover = new SimpleFalsePositiveRemover();
			simpleFalsePositiveRemover.removeFalsePositives(mDatabasePath, mCustomSimilarityPercent, 100, searchProgressBundle);
		}

		searchProgressBundle.progressBar.setValue(searchProgressBundle.progressBar.getValue() + 1);
		searchProgressBundle.progressBar.repaint();
		searchProgressBundle.frame.dispose();
		
		//allFacePaths = (ArrayList<String>) FileUtilsLegacy.parseDirectoryRecursiveForAll(mDatabasePath);
		
		CursorUtils.setDefaultCursor(GUI_MAIN);
		
		//SearchImageGallery searchImageGallery = new SearchImageGallery(allFacePaths);
		//searchImageGallery.build();
	}

}
