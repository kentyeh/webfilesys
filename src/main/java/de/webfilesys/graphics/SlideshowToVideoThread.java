package de.webfilesys.graphics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.webfilesys.WebFileSys;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlideshowToVideoThread extends Thread {
private static final Logger logger = LogManager.getLogger(SlideshowToVideoThread.class);	
	private static final String TARGET_VIDEO_FILENAME = "slideshow";
	private static final String TARGET_VIDEO_FILE_EXT = ".mp4";
	
    String pictureListFilePath;
    
    String targetPath;
    
    int videoResolutionWidth;
    int videoResolutionHeight;
    
    int duration;
    
    public SlideshowToVideoThread(String pictureListFilePath, String targetPath, int videoResolutionWidth, int videoResolutionHeight, int duration) {
    	this.pictureListFilePath = pictureListFilePath;
    	this.targetPath = targetPath;
    	this.videoResolutionWidth = videoResolutionWidth;
    	this.videoResolutionHeight = videoResolutionHeight;
    	this.duration = duration;
    }

    public void run() {
        logger.debug("starting picture slideshow video creation thread for pictures in " + pictureListFilePath);
        
        Thread.currentThread().setPriority(1);
    	
        File targetDirFile = new File(targetPath);
        if (!targetDirFile.exists()) {
            if (!targetDirFile.mkdir()) {
                logger.error("failed to create target folder for slideshow video: " + targetPath);
                return;
            }
        }
        
        String targetFilePath = getTargetVideoFilePath();
		
        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
		
        ArrayList<String> progNameAndParams = new ArrayList<String>();
        progNameAndParams.add(ffmpegExePath);
        
        progNameAndParams.add("-f");
        progNameAndParams.add("concat");
        progNameAndParams.add("-safe");
        progNameAndParams.add("0");
        progNameAndParams.add("-y");
        progNameAndParams.add("-i");
        progNameAndParams.add(pictureListFilePath);

        progNameAndParams.add("-vf");

        // with fade in/out but does not keep aspect ratio
        // progNameAndParams.add("zoompan=d=" + (duration + 1) + ":s=" + videoResolutionWidth + "x" + videoResolutionHeight + ":fps=1,framerate=25:interp_start=0:interp_end=255:scene=100");
        
        // scaled with correct aspect ratio but no fade in/out
        // progNameAndParams.add("scale=" + videoResolutionWidth + ":" + videoResolutionHeight + ":force_original_aspect_ratio=decrease,pad=" + videoResolutionWidth + ":" + videoResolutionHeight + ":(ow-iw)/2:(oh-ih)/2,setsar=1");

        // with fade in/fade out and keeps aspect ratio
        progNameAndParams.add("scale=" + videoResolutionWidth + ":" + videoResolutionHeight + ":force_original_aspect_ratio=decrease,pad=" + videoResolutionWidth + ":" + videoResolutionHeight + ":(ow-iw)/2:(oh-ih)/2,zoompan=d=" + (duration + 1) + ":s=" + videoResolutionWidth + "x" + videoResolutionHeight + ":fps=1,framerate=25:interp_start=0:interp_end=255:scene=100");
        
        progNameAndParams.add("-c:v");
        progNameAndParams.add("h264");

        // required to run on Samsung TV
        progNameAndParams.add("-profile:v");
        progNameAndParams.add("high");
        progNameAndParams.add("-level:v");
        progNameAndParams.add("4.0");
        progNameAndParams.add("-pix_fmt");
        progNameAndParams.add("yuv420p");
        
        progNameAndParams.add(targetFilePath);
        
        StringBuilder buff = new StringBuilder();
        for (String cmdToken : progNameAndParams) {
            buff.append(cmdToken);
            buff.append(' ');
        }
            logger.debug("ffmpeg call with params: " + buff.toString());
        
		try {
			Process convertProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
			
	        BufferedReader  convertProcessOut = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));
	        
	        String outLine = null;
	        
	        while ((outLine = convertProcessOut.readLine()) != null) {
	                logger.debug("ffmpeg output: " + outLine);
	        }
			
			int convertResult = convertProcess.waitFor();
			
			if (convertResult == 0) {
				File resultFile = new File(targetFilePath);
				
				if (!resultFile.exists()) {
                    logger.error("result file from ffmpeg video creation not found: " + targetFilePath);
				}
			} else {
				logger.warn("ffmpeg returned error " + convertResult);
			}
		} catch (IOException ioex) {
			logger.error("failed to create slideshow video", ioex);
		} catch (InterruptedException iex) {
			logger.error("failed to create slideshow video", iex);
		}
		
    	File fileListFile = new File(pictureListFilePath);
    	if (fileListFile.exists()) {
    		fileListFile.delete();
    	}
    }
   
    private String getTargetVideoFilePath() {
        boolean fileNameConflict = false;
        String targetFilePath = null;        
        int i = 0;
        do {
        	fileNameConflict = false;
            targetFilePath = targetPath + File.separator + TARGET_VIDEO_FILENAME + "_" + i + TARGET_VIDEO_FILE_EXT;
            File testConflictFile = new File(targetFilePath);
            if (testConflictFile.exists()) {
            	fileNameConflict = true;
            }
            i++;
    	} while (fileNameConflict);
        
    	return targetFilePath;
    }
}

