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

public class VideoThumbnailCreator extends Thread
{
    private static final Logger logger = LogManager.getLogger(VideoThumbnailCreator.class);
    public static final String THUMBNAIL_SUBDIR = "_thumbnails";

    String videoFilePath;
    
    public VideoThumbnailCreator(String videoPath) {
    	videoFilePath = videoPath;
    }

    @Override
    public void run() {
        logger.debug("starting video thumbnail creator thread for video file " + videoFilePath);
        
        WebFileSys.getInstance().setThumbThreadRunning(true);

        Thread.currentThread().setPriority(1);

        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
        
        if (!CommonUtils.isEmpty(ffmpegExePath)) {
            
            String videoThumbPath = getThumbPath(videoFilePath);
            File thumbDirFile = new File(videoThumbPath);
            if (!thumbDirFile.exists()) {
                if (!thumbDirFile.mkdir()) {
                    logger.error("failed to create video thumbnail directory " + videoThumbPath);
                }
            }
            
            File videoFile = new File(videoFilePath);
            long fileSize = videoFile.length();
            
            String frameGrabTime = "00:00:01.00";
            if (fileSize > 50 * 1000000) {
                frameGrabTime = "00:00:05.00";
            } else if (fileSize > 10 * 1000000) {
                frameGrabTime = "00:00:03.00";
            } else if (fileSize > 5 * 1000000) {
                frameGrabTime = "00:00:02.00";
            }
            
        	// String progNameAndParams = ffmpegExePath + " -i " + videoFilePath + " -ss " + frameGrabTime + " -filter:v scale=160:-1" + " -vframes 1 " + getFfmpegOutputFileSpec(videoFilePath);
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-i");
            progNameAndParams.add(videoFilePath);
            progNameAndParams.add("-ss");
            progNameAndParams.add(frameGrabTime);
            progNameAndParams.add("-filter:v");
            progNameAndParams.add("scale=160:-1");
            progNameAndParams.add("-vframes");
            progNameAndParams.add("1");
            progNameAndParams.add(getFfmpegOutputFileSpec(videoFilePath));
            
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
					    
					    File thumbnailFile = new File(getThumbnailPath(videoFilePath));
					    
					    if (!resultFile.renameTo(thumbnailFile)) {
			                logger.error("failed to rename result file for video frame grabbing from video " + videoFilePath);
					    }
					} else {
	                    logger.error("result file from ffmpeg video frame grabbing not found: " + getFfmpegResultFilePath(videoFilePath));
					}
				} else {
					logger.warn("ffmpeg returned error " + grabResult);
				}
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to grab frame for thumbnail from video " + videoFilePath, ioex);
			}
        }
        
        WebFileSys.getInstance().setThumbThreadRunning(false);
    }

    public static String getFfmpegOutputFileSpec(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + THUMBNAIL_SUBDIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1);

        return(thumbPath + "%01d"+ imgFileName + ".jpg");
    }

    public static String getFfmpegResultFilePath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + THUMBNAIL_SUBDIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1);

        return(thumbPath + "1"+ imgFileName + ".jpg");
    }
    
    public static String getThumbnailPath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        if (sepIdx < 0) {
            logger.error("incorrect video file path: " + videoPath);
            return(null); 
        }

        String basePath = videoPath.substring(0, sepIdx + 1);

        String thumbPath = basePath + THUMBNAIL_SUBDIR + File.separator;

        String imgFileName = videoPath.substring(sepIdx + 1) + ".jpg";

        return(thumbPath + imgFileName);
    }
    
    private static String getThumbPath(String videoPath) {
        int sepIdx = videoPath.lastIndexOf(File.separator);

        if (sepIdx < 0) {
            logger.error("incorrect video file path: " + videoPath);
            return(null); 
        }

        String basePath = videoPath.substring(0, sepIdx + 1);

        return basePath + THUMBNAIL_SUBDIR;
    }
    
}

