package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.VideoInfo;
import de.webfilesys.graphics.VideoInfoExtractor;
import de.webfilesys.util.XmlUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiVideoConcatHandler extends MultiVideoHandlerBase {
	
	private static final Logger logger = LogManager.getLogger(MultiVideoConcatHandler.class);
	
	private static final String FFMPEG_INPUT_LIST_FILE_NAME = "ffmpegInputFileList.txt";
	
	private static final String TARGET_FOLDER = "_converted";
	
	private static final int ERROR_CODE_FRAMERATE_MISSMATCH = 1;
	private static final int ERROR_CODE_CODEC_MISSMATCH = 2;
	private static final int ERROR_CODE_RESOLUTION_MISSMATCH = 3;
	private static final int ERROR_CODE_CONVERSION_FAILED = 4;
	
	public MultiVideoConcatHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			PrintWriter output, String uid) {
		super(req, resp, session, output, uid);
	}

        @Override
	protected void process() {
		if (!checkWriteAccess()) {
			return;
		}
		
		String currentPath = getCwd();

		List<String> selectedFiles = getSelectedFiles();

		boolean videoParameterMissmatch = false;
		
		int errorCode = 0;
		
		String codec = null;
		int frameRate = 0;
		int videoWidth = 0;
		int videoHeight = 0;
		
		File ffmpegFileListFile = new File(currentPath, FFMPEG_INPUT_LIST_FILE_NAME);
		
		PrintWriter ffmpegInputFileListFile = null;
		
		try {
	        ffmpegInputFileListFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ffmpegFileListFile), "UTF-8"));
	        
                    for (String selectedFile : selectedFiles) {
                        String filePath = null;
                        if (currentPath.endsWith(File.separator)) {
                            filePath = currentPath + selectedFile;
                        } else {
                            filePath = currentPath + File.separator + selectedFile;
                        }
                        VideoInfoExtractor videoInfoExtractor = new VideoInfoExtractor();
                        VideoInfo videoInfo = videoInfoExtractor.getVideoInfo(filePath);
                        if (codec == null) {
                            codec = videoInfo.getCodec();
                        } else {
                            if (!videoInfo.getCodec().equals(codec)) {
                                videoParameterMissmatch = true;
                                errorCode = ERROR_CODE_CODEC_MISSMATCH;
                            }
                        }   if (frameRate == 0) {
                            frameRate = videoInfo.getFrameRate();
                        } else {
                            if (videoInfo.getFrameRate() != frameRate) {
                                videoParameterMissmatch = true;
                                errorCode = ERROR_CODE_FRAMERATE_MISSMATCH;
                            }
                        }   if (videoWidth == 0) {
                            videoWidth = videoInfo.getWidth();
                        } else {
                            if (videoInfo.getWidth() != videoWidth) {
                                videoParameterMissmatch = true;
                                errorCode = ERROR_CODE_RESOLUTION_MISSMATCH;
                            }
                        }   if (videoHeight == 0) {
                            videoHeight = videoInfo.getHeight();
	            } else {
	            	if (videoInfo.getHeight() != videoHeight) {
	            		videoParameterMissmatch = true;
                                errorCode = ERROR_CODE_RESOLUTION_MISSMATCH;
                        }
                        }   logger.debug("video file to concatenate: " + filePath + ": codec=" + videoInfo.getCodec() + " width: " + videoInfo.getWidth() + " height: " + videoInfo.getHeight() + " fps=" + videoInfo.getFrameRate() + " duration=" + videoInfo.getDuration());
                        ffmpegInputFileListFile.println("file " + '\'' +  filePath + '\'');
                    }
		} catch (IOException ioex) {
		    logger.error("failed to write ffmpeg input list file for video concatenation", ioex);
		} finally {
		    if (ffmpegInputFileListFile != null) {
		        try {
		            ffmpegInputFileListFile.close();
		            
			        if (videoParameterMissmatch) {
			        	File fileListFile = new File("ffmpegInputFileListPath");
			        	if (fileListFile.exists()) {
			        		fileListFile.delete();
			        	}
			        }
		        } catch (Exception ex) {
		        }
		    }
		}

        String targetPath = null;
		
		if (!videoParameterMissmatch) {
            targetPath = currentPath + File.separator + TARGET_FOLDER;
        	
            File targetDirFile = new File(targetPath);
            if (!targetDirFile.exists()) {
                if (!targetDirFile.mkdir()) {
                    logger.error("failed to create target folder for video conversion: " + targetPath);
                    return;
                }
            }
            
            String firstFileName = selectedFiles.get(0);
            
            String fileNameOnly = firstFileName.substring(0,  firstFileName.lastIndexOf('.'));
            String ext = firstFileName.substring(firstFileName.lastIndexOf('.') + 1);
            String targetFileName = fileNameOnly + "_concat." + ext;
            String targetFilePath = targetPath + File.separator + targetFileName;
            
            boolean targetFileNameOk = true;
            do {
                File existingTargetFile = new File(targetFilePath);
                if (existingTargetFile.exists()) {
                    targetFileNameOk = false;
                    int dotIdx = targetFilePath.lastIndexOf(".");
                    targetFilePath = targetFilePath.substring(0, dotIdx) + "-1" + targetFilePath.substring(dotIdx);
                } else {
                    targetFileNameOk = true;
                }
            } while (!targetFileNameOk);
            
	        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
			
        	// String progNameAndParams = ffmpegExePath + " -f concat -safe 0 -i " + ffmpegFileListFile.getAbsolutePath() + " -c copy " + targetFilePath;
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-f");
            progNameAndParams.add("concat");
            progNameAndParams.add("-safe");
            progNameAndParams.add("0");
            progNameAndParams.add("-i");
            progNameAndParams.add(ffmpegFileListFile.getAbsolutePath());
            progNameAndParams.add("-c");
            progNameAndParams.add("copy");
            progNameAndParams.add(targetFilePath);
            
            StringBuilder buff = new StringBuilder();
            for (String cmdToken : progNameAndParams) {
                buff.append(cmdToken);
                buff.append(' ');
            }
            logger.debug("ffmpeg call with params: " + buff.toString());
            
			try {
				Process convertProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
				
		        BufferedReader grabProcessOut = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));
		        
		        String outLine = null;
		        
		        while ((outLine = grabProcessOut.readLine()) != null) {
		            logger.debug("ffmpeg output: " + outLine);
		        }
				
				int convertResult = convertProcess.waitFor();
				
				if (convertResult == 0) {
					File resultFile = new File(targetFilePath);
					
					if (!resultFile.exists()) {
	                    logger.error("result file from ffmpeg video conversion not found: " + targetFilePath);
					}
				} else {
					logger.warn("ffmpeg returned error " + convertResult);
				}
				
				if (!ffmpegFileListFile.delete()) {
					logger.warn("failed to delete ffmpeg input file list file");
				}
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to concatente videos", ioex);
				errorCode = ERROR_CODE_CONVERSION_FAILED;
			}
		}
		
		Element resultElement = doc.createElement("result");

		if (errorCode != 0) {
			XmlUtil.setChildText(resultElement, "errorCode", Integer.toString(errorCode));
		} else {
			XmlUtil.setChildText(resultElement, "success", Boolean.toString(true));
			XmlUtil.setChildText(resultElement, "targetFolder", TARGET_FOLDER);
			XmlUtil.setChildText(resultElement, "targetPath", targetPath);
		}
		
		doc.appendChild(resultElement);

		processResponse();
	}

}
