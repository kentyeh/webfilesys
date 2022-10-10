package de.webfilesys.graphics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.webfilesys.WebFileSys;
import de.webfilesys.util.CommonUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VideoFrameExtractor extends Thread
{
    private static final Logger logger = LogManager.getLogger(VideoFrameExtractor.class);

    public static final String FRAME_TARGET_DIR = "_frames"; 
	
    String videoFilePath;
    String frameGrabTime;
    String frameSize;
    int width;
    int height;
    
    public VideoFrameExtractor(String videoPath, String frameExtractTime, int videoWidth, int videoHeight, String grabFrameSize) {
    	videoFilePath = videoPath;
    	frameGrabTime = frameExtractTime;
    	frameSize = grabFrameSize;
    	width = videoWidth;
    	height = videoHeight;
    }

        @Override
    public void run() {
        logger.debug("starting video frame extractor thread for video file " + videoFilePath);
        
        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
        
        if (!CommonUtils.isEmpty(ffmpegExePath)) {
            
            String videoFramePath = getFramePath(videoFilePath);
            File frameDirFile = new File(videoFramePath);
            if (!frameDirFile.exists()) {
                if (!frameDirFile.mkdir()) {
                    logger.error("failed to create video frame directory " + videoFramePath);
                }
            }
            
            String scaleFilter = null;
            
            if (width >= height) {
            	scaleFilter = "scale=" + frameSize + ":-1";
            } else {
            	scaleFilter = "scale=-1:" + frameSize;
            }
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-i");
            progNameAndParams.add(videoFilePath);
            progNameAndParams.add("-ss");
            progNameAndParams.add(frameGrabTime);
            progNameAndParams.add("-filter:v");
            progNameAndParams.add(scaleFilter);
            progNameAndParams.add("-vframes");
            progNameAndParams.add("1");
            progNameAndParams.add(getFfmpegOutputFileSpec(videoFilePath));
            
        	// String progNameAndParams = ffmpegExePath + " -i " + videoFilePath + " -ss " + frameGrabTime + " -filter:v " + scaleFilter + " -vframes 1 " + getFfmpegOutputFileSpec(videoFilePath);

            StringBuilder buff = new StringBuilder();
            for (String cmdToken : progNameAndParams) {
                buff.append(cmdToken);
                buff.append(' ');
            }
            logger.debug("ffmpeg call with params: " + buff.toString());
        	
			try {
				Process grabProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
				
		        BufferedReader grabProcessOut = new BufferedReader(new InputStreamReader(grabProcess.getErrorStream()));
		        
		        String outLine = null;
		        
		        while ((outLine = grabProcessOut.readLine()) != null) {
		            logger.debug("ffmpeg output: " + outLine);
		        }
				
				int grabResult = grabProcess.waitFor();
				
				if (grabResult == 0) {
					File resultFile = new File(getFfmpegResultFilePath(videoFilePath));
					
					if (resultFile.exists()) {
					    
					    File frameFile = new File(getFrameTargetPath(videoFilePath));
					    
					    if (!resultFile.renameTo(frameFile)) {
			                logger.error("failed to rename result file for video frame grabbing from video " + videoFilePath);
					    }
					} else {
	                    logger.error("result file from ffmpeg video frame grabbing not found: " + getFfmpegResultFilePath(videoFilePath));
					}
				} else {
					logger.warn("ffmpeg returned error " + grabResult);
				}
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to grab frame from video " + videoFilePath, ioex);
			}
        }
    }

    public static String getFfmpegOutputFileSpec(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + FRAME_TARGET_DIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1);
        // imgFileName = imgFileName.replace(' ', '_');

        return(thumbPath + "%01d"+ imgFileName + ".jpg");
    }

    public static String getFfmpegResultFilePath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + FRAME_TARGET_DIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1);

        return(thumbPath + "1"+ imgFileName + ".jpg");
    }
    
    private static String getFramePath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        if (sepIdx < 0) {
            logger.error("incorrect video file path: " + videoPath);
            return(null); 
        }

        String basePath = videoPath.substring(0, sepIdx + 1);

        return basePath + FRAME_TARGET_DIR;
    }
    
    public static String getFrameTargetPath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        if (sepIdx < 0) {
            logger.error("incorrect video file path: " + videoPath);
            return(null); 
        }

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + FRAME_TARGET_DIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1) + ".jpg";

        return(thumbPath + imgFileName);
    }
    
}

