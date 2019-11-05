package com.keypointforensics.videotriage.gui.wizard.preview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.keypointforensics.videotriage.gui.imagepanel.ScalableSimpleImagePanel;
import com.keypointforensics.videotriage.gui.localfile.wizard.FileDropList;
import com.keypointforensics.videotriage.gui.main.FileSelectVideoPreviewAccessory;
import com.keypointforensics.videotriage.legacy.FileUtilsLegacy;
import com.keypointforensics.videotriage.thread.CreatePreviewVideoThread;
import com.keypointforensics.videotriage.util.BorderUtils;
import com.keypointforensics.videotriage.util.CursorUtils;
import com.keypointforensics.videotriage.util.ImageUtils;
import com.keypointforensics.videotriage.util.LocalFileWizardUtils;
import com.keypointforensics.videotriage.util.ThreadUtils;
import com.keypointforensics.videotriage.util.Utils;
import com.keypointforensics.videotriage.util.WebUtils;
import com.keypointforensics.videotriage.util.WindowUtils;
import com.keypointforensics.videotriage.window.WindowRegistry;

public class PreviewLocalFileWizardWindow extends JFrame implements ActionListener, ItemListener {

	/*
	 * Author: Daniel Puckowski
	 */
	
	private static final long serialVersionUID = 4271388619852805707L;
	
	private static final int CONTROL_GRID_LAYOUT_ROWS     = 10;
	private static final int CONTROL_GRID_LAYOUT_COLUMNS  = 1;
	
	private FileDropList mFileDropList;
	private JButton mSelectFilesButton;
	private JButton mStartButton;
	private ScalableSimpleImagePanel mPreviewPanel;
	private JButton mRemoveSelectedButton;
	private JCheckBox mShowPreviewsCheckBox;
	
	//
	private final String[] PREVIEW_RESOLUTION_LIST =  { 
		"320 x 240  4:3",
		"480 x 320  3:2",
		"640 x 360  16:9",
		"960 x 540  16:9",
		"960 x 640  3:2",
		"960 x 720  4:3",
		"1024 x 768 4:3",
		"1280 x 720 16:9"
	};
	
	private JComboBox<String> mResolutionComboBox = new JComboBox<String>(PREVIEW_RESOLUTION_LIST);
	
	private final JFrame PARENT_FRAME;
	
	public PreviewLocalFileWizardWindow(final JFrame parentFrame) {
		PARENT_FRAME = parentFrame;
		
		buildFrame();
		
		WindowRegistry.getInstance().registerFrame(this, "PreviewLocalFileWizard");
	}
	
	private int getSelectedPreviewHeight() {
		String selection = (String) mResolutionComboBox.getSelectedItem();
		
		selection = selection.substring(selection.indexOf("x") + 2, selection.length());
		selection = selection.substring(0, selection.indexOf(" "));
		
		return Integer.parseInt(selection);
	}
	
	private int getSelectedPreviewWidth() {
		String selection = (String) mResolutionComboBox.getSelectedItem();
		
		selection = selection.substring(0, selection.indexOf(" "));
		
		return Integer.parseInt(selection);
	}
	
	private void buildFrame() {		
		buildMenuBar();
		
		this.setLayout(new BorderLayout());
		
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridLayout(CONTROL_GRID_LAYOUT_ROWS, CONTROL_GRID_LAYOUT_COLUMNS));
		contentPanel.setBorder(BorderUtils.getEmptyBorder());				
		
		JScrollPane contentScrollPane = new JScrollPane(contentPanel);
		contentScrollPane.setPreferredSize(new Dimension(440, 720));
		WindowUtils.setScrollBarIncrement(contentScrollPane);
		
		JTabbedPane settingsTabPane = new JTabbedPane();
		settingsTabPane.addTab("Local File Settings", contentScrollPane);
		
		JPanel previewContentPanel = new JPanel();
		previewContentPanel.setLayout(new BorderLayout());
		previewContentPanel.setBorder(BorderUtils.getEmptyBorder()); 
		
		mPreviewPanel = new ScalableSimpleImagePanel(true);//DeprecatedTitlePanel("", true);
		mPreviewPanel.update(ImageUtils.NO_PREVIEW_AVAILABLE_IMAGE);
		mPreviewPanel.setPreferredSize(new Dimension(320, 320));
		mPreviewPanel.setBorder(BorderUtils.getEmptyBorder());
		
		JPanel textAreaPanel = new JPanel();
		textAreaPanel.setLayout(new BorderLayout());
		
		JTextArea videoInformationTextArea = new JTextArea(10, 60);
		videoInformationTextArea.setEditable(false);
		JScrollPane videoInformationTextAreaScrollPane = new JScrollPane(videoInformationTextArea);
		WindowUtils.setScrollBarIncrement(videoInformationTextAreaScrollPane);
		
		textAreaPanel.add(new JLabel("Video File Information"), BorderLayout.NORTH);
		textAreaPanel.add(videoInformationTextAreaScrollPane, BorderLayout.CENTER);
		
		previewContentPanel.add(new JLabel("Video Preview"), BorderLayout.NORTH);
		previewContentPanel.add(mPreviewPanel, BorderLayout.CENTER);
		previewContentPanel.add(textAreaPanel, BorderLayout.SOUTH);
		
		JLabel fileSelectionLabel = new JLabel("List of Files to Process");
		contentPanel.add(fileSelectionLabel);
		
		mFileDropList = new FileDropList(mPreviewPanel, videoInformationTextArea);
		mFileDropList.setPreferredSize(new Dimension(600, 300));
		
		mSelectFilesButton = new JButton("Select Files");
		mSelectFilesButton.addActionListener(this);
		contentPanel.add(mSelectFilesButton);
		
		mRemoveSelectedButton = new JButton("Remove Selected Video");
		mRemoveSelectedButton.addActionListener(this);
		contentPanel.add(mRemoveSelectedButton);
		
		JLabel resolutionComboBoxLabel = new JLabel("Video Preview Resolution");
		contentPanel.add(resolutionComboBoxLabel);
		
		mResolutionComboBox.setSelectedIndex(-1);
		contentPanel.add(mResolutionComboBox);
		
		mShowPreviewsCheckBox = new JCheckBox("Show Icon Previews", true);
		mShowPreviewsCheckBox.addItemListener(this);
		contentPanel.add(mShowPreviewsCheckBox);
		
		mStartButton = new JButton("Start Processing");
		mStartButton.addActionListener(this);
		contentPanel.add(mStartButton);
		
		JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, settingsTabPane, mFileDropList);
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, previewContentPanel);
		
		this.add(mainSplitPane, BorderLayout.CENTER);
		
		this.setTitle("Local File Preview Wizard");
		this.pack();
		WindowUtils.setFrameIcon(this);
		WindowUtils.maximize(this);
		this.setVisible(true);
		//WindowUtils.center(this);
	}
	
	@Override
	public void itemStateChanged(ItemEvent event) {
		if (event.getSource() instanceof JCheckBox) {
			JCheckBox checkBox = (JCheckBox) event.getSource();

			if(checkBox == mShowPreviewsCheckBox) {
				mFileDropList.setShowIconPreviews(mShowPreviewsCheckBox.isSelected());
			}
		}
	}
	
	private void performSelectFilesAction() {		
		Boolean old = UIManager.getBoolean("FileChooser.readOnly");  
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);  		
		
		final JFileChooser fileChooser = new JFileChooser(); 
		fileChooser.setMultiSelectionEnabled(true);
		final FileSelectVideoPreviewAccessory imagePreviewAccessory = new FileSelectVideoPreviewAccessory();
		fileChooser.setAccessory(imagePreviewAccessory);
		fileChooser.addPropertyChangeListener(imagePreviewAccessory);
		
		final int fileChooserResult = fileChooser.showOpenDialog(this);
				
		if(fileChooserResult == JFileChooser.APPROVE_OPTION) {
			CursorUtils.setBusyCursor(this);
			
			final File[] selectedFiles = fileChooser.getSelectedFiles();
			//File selectedFile = null;
			
			for(int i = 0; i < selectedFiles.length; ++i) {
				final File selectedFile = selectedFiles[i];
				
				if(selectedFile.isDirectory()) {
					ArrayList<String> allFilesInFolderAndSubfolders = (ArrayList<String>) FileUtilsLegacy.parseDirectoryRecursiveForAll(selectedFile.getAbsolutePath());
												
					for(String pathOfFileInFolder : allFilesInFolderAndSubfolders) {
						EventQueue.invokeLater(new Runnable() { public void run() {
								mFileDropList.addFile(pathOfFileInFolder);
								mFileDropList.revalidate();
							}
						});
					}
				}
				else {
					EventQueue.invokeLater(new Runnable() { public void run() {
							mFileDropList.addFile(selectedFile.getAbsolutePath());
							mFileDropList.revalidate();
						}
					});
				}
			}
			
			CursorUtils.setDefaultCursor(this);
		}
		
		//mFileDropList.revalidate();
		
		UIManager.put("FileChooser.readOnly", old); 
	}
	
	private void performStartAction() {
		if(mResolutionComboBox.getSelectedIndex() == -1) {
			Utils.displayMessageDialog("No Resolution", "Please select a preview video resolution from the drop-down menu.");
			
			return;
		}
		
		PreviewLocalFileWizardWindow.this.dispose();
		
		ArrayList<String> localFilesToProcess = LocalFileWizardUtils.getLocalWizardListOfFiles(mFileDropList.getFileList());

		CreatePreviewVideoThread createPreviewVideoThread = new CreatePreviewVideoThread(PARENT_FRAME, localFilesToProcess, getSelectedPreviewWidth(), getSelectedPreviewHeight());
		createPreviewVideoThread.start();
		 
		try {
			createPreviewVideoThread.join();
		} catch (InterruptedException interruptedException) {
			//interruptedException.printStackTrace();
		}
	}
	
	private void performRemoveSelectedVideoAction() {
		CursorUtils.setBusyCursor(this);
		
		mFileDropList.removeSelectedIndex();
		
		CursorUtils.setDefaultCursor(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() instanceof JButton) {
			JButton button = (JButton) event.getSource();

			if(button == mSelectFilesButton) {
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow SelectFiles", this);
							
						performSelectFilesAction();
							
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			else if(button == mStartButton) {
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow PerformStart", this);
							
						performStartAction();
							
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			else if(button == mRemoveSelectedButton) {
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow RemoveSelected", this);
							
						performRemoveSelectedVideoAction();
							
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
		}
	}

	private void buildMenuBar() {		
		JMenuBar menuBar;
		JMenu menu;

		menuBar = new JMenuBar();

		menu = new JMenu("File");
		menuBar.add(menu);
		
		JMenuItem menuItem = new JMenuItem("Exit");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow Exit", this);
						
						PreviewLocalFileWizardWindow.this.dispose();
						
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		menu = new JMenu("Review");
		menuBar.add(menu);
		
		menuItem = new JMenuItem("Select Files");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow Select", this);
						
						performSelectFilesAction();
						
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Remove Selected Video");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow RemoveSelected", this);
							
						performRemoveSelectedVideoAction();
							
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Start Processing");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow Start", this);
						
						performStartAction();
						
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		menu = new JMenu("Help");
		menuBar.add(menu);
		
		menuItem = new JMenuItem("Open Documentation");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow OpenDoc", this);
						
						try {
							WebUtils.openWebpage(new URL(WebUtils.URL_STRING_DOCUMENTATION));
						} catch (MalformedURLException malformedUrlException) {
							//malformedUrlException.printStackTrace();
						}
					
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("About");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {	
				new Thread() {
					@Override
					public void run() {
						ThreadUtils.addThreadToHandleList("PrevLocFileWizWindow About", this);
						
						Utils.displayMessageDialog("About", Utils.SOFTWARE_NAME + " \nVersion: " + Utils.SOFTWARE_VERSION + "\n� " + Utils.AUTHOR_NAME);
						
						ThreadUtils.removeThreadFromHandleList(this);
					}
				}.start();
			}
			
		});
		menu.add(menuItem);
		
		this.setJMenuBar(menuBar);
	}
}
