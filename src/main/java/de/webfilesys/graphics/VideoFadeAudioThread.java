package de.webfilesys.graphics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.webfilesys.SubdirExistCache;
import de.webfilesys.WebFileSys;
import de.webfilesys.util.CommonUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VideoFadeAudioThread extends Thread {
    private static final Logger logger = LogManager.getLogger(VideoFadeAudioThread.class);

    public static final String TARGET_SUBDIR = "_fadeAudio";
	
    private String videoFilePath;

    private int fadeInDuration;
    
    private int fadeOutDuration;
    
    public VideoFadeAudioThread(String videoPath) {
    	videoFilePath = videoPath;
    }

    public void setFadeInDuration(int newVal) {
    	fadeInDuration = newVal;
    }

    public void setFadeOutDuration(int newVal) {
    	fadeOutDuration = newVal;
    }
    
    @Override
    public void run() {
        logger.debug("starting fade audio thread for video file " + videoFilePath);
        
        Thread.currentThread().setPriority(1);

        int fadeOutStart = -1;
        
        if (fadeOutDuration > 0) {
            VideoInfoExtractor videoInfoExtractor = new VideoInfoExtractor();
            VideoInfo videoInfo = videoInfoExtractor.getVideoInfo(videoFilePath);
            if (videoInfo.getFfprobeResult() == 0) {
                if (videoInfo.getDuration().length() > 0) {
                    int durationSeconds = videoInfo.getDurationSeconds();
                    if (fadeOutDuration < durationSeconds) {
                        fadeOutStart = durationSeconds - fadeOutDuration;
                    } else {
                    	throw new IllegalArgumentException("fade out duration must be less than video duration");
                    }
                }
    		}
            if (fadeOutStart < 0) {
                logger.error("failed to determine fade out start point");
                return;
            }
        }
        
        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
        
        if (!CommonUtils.isEmpty(ffmpegExePath)) {

        	String[] partsOfPath = CommonUtils.splitPath(videoFilePath);
        	
        	String sourcePath = partsOfPath[0];
        	String sourceFileName = partsOfPath[1];

            String targetPath = sourcePath + File.separator + TARGET_SUBDIR;
        	
            File targetDirFile = new File(targetPath);
            if (!targetDirFile.exists()) {
                if (!targetDirFile.mkdir()) {
                    logger.error("failed to create target folder for video audio fade: " + targetPath);
                }
            }
            
            String targetFilePath = targetPath + File.separator + sourceFileName;
            
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
            
            // ffmpeg -i testvideo.mp4 -filter:a "afade=in:st=0:d=1, afade=out:st=30:d=6" -c:v libx264 -c:a aac testvideo-fade.mp4
            
            StringBuilder fadeParams = new StringBuilder();
            if (fadeInDuration > 0) {
            	fadeParams.append("afade=in:st=0:d=");
            	fadeParams.append(fadeInDuration);
            	fadeParams.append(", ");
            }
            if (fadeOutStart > 0) {
            	fadeParams.append("afade=out:st=");
            	fadeParams.append(fadeOutStart);
            	fadeParams.append(":d=");
            	fadeParams.append(fadeOutDuration);
            }
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-i");
            progNameAndParams.add(videoFilePath);
            
            progNameAndParams.add("-filter:a");
            
            progNameAndParams.add(fadeParams.toString());
            
            progNameAndParams.add("-c:v");
            progNameAndParams.add("copy");
            
            progNameAndParams.add("-c:a");
            progNameAndParams.add("aac");
            
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
	                    logger.error("result file from ffmpeg fade audio not found: " + targetFilePath);
					}
					SubdirExistCache.getInstance().setExistsSubdir(sourcePath, 1);
				} else {
					logger.warn("ffmpeg returned error " + convertResult);
				}
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to fade audio in video " + videoFilePath, ioex);
			}
        }
    }
    
}

