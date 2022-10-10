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

public class VideoAudioMixerThread extends Thread {
    private static final Logger logger = LogManager.getLogger(VideoAudioMixerThread.class);
    private String videoFilePath;
    
    ArrayList<String> audioFiles;
    
    public VideoAudioMixerThread(String videoPath) {
    	videoFilePath = videoPath;
    }

    public void setAudioFiles(ArrayList<String> newList) {
    	audioFiles = newList;
    }
    
    @Override
    public void run() {
        logger.debug("starting video/audio mixer thread for video file " + videoFilePath);
        
        Thread.currentThread().setPriority(1);
        
    	String[] partsOfPath = CommonUtils.splitPath(videoFilePath);
    	
    	String sourcePath = partsOfPath[0];
    	String sourceFileName = partsOfPath[1];

        String targetPath = sourcePath + File.separator + "_audio";
    	
        File targetDirFile = new File(targetPath);
        if (!targetDirFile.exists()) {
            if (!targetDirFile.mkdir()) {
                logger.error("failed to create target folder for video conversion: " + targetPath);
                return;
            }
        }
        
        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
        
        String audioFilePath = null;
        if (audioFiles.size() == 1) {
            audioFilePath = audioFiles.get(0);
        } else {
        	audioFilePath = concatenateAudioFiles(audioFiles, targetPath, ffmpegExePath);
        	if (audioFilePath == null) {
        		return;
        	}
        }
        
        String targetFilePath = targetPath + File.separator + sourceFileName;
        
        if (!CommonUtils.isEmpty(ffmpegExePath)) {
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-y");
            progNameAndParams.add("-i");
            progNameAndParams.add(videoFilePath);
            progNameAndParams.add("-i");
            progNameAndParams.add(audioFilePath);
            progNameAndParams.add("-map");
            progNameAndParams.add("0:v");
            progNameAndParams.add("-map");
            progNameAndParams.add("1:a");
            progNameAndParams.add("-strict");
            progNameAndParams.add("-1");
            progNameAndParams.add("-c");
            progNameAndParams.add("copy");
            progNameAndParams.add("-shortest");
            progNameAndParams.add(targetFilePath);
            
            StringBuilder buff = new StringBuilder();
            for (String cmdToken : progNameAndParams) {
                buff.append(cmdToken);
                buff.append(' ');
            }
            logger.debug("ffmpeg call with params: " + buff.toString());
            
			try {
				Process convertProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
				
		        BufferedReader convertProcessOut = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));
		        
		        String outLine = null;
		        
		        while ((outLine = convertProcessOut.readLine()) != null) {
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
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to convert video " + videoFilePath, ioex);
			}
        }
        
        if (audioFiles.size() > 1) {
        	File combinedAudioFile = new File(audioFilePath);
        	if (!combinedAudioFile.delete()) {
        		logger.warn("failed to delete temporary combined audio file " + audioFilePath);
        	}
        }
    }
   
    private String concatenateAudioFiles(ArrayList<String> audioFiles, String targetPath, String ffmpegExePath) {
    	
        String combinedAudioFilePath = targetPath + File.separator + "combinedAudio.mp3";
        
        ArrayList<String> progNameAndParams = new ArrayList<>();
        progNameAndParams.add(ffmpegExePath);
        progNameAndParams.add("-y");
        
    	for (String audioFile: audioFiles) {
        	progNameAndParams.add("-i");
        	progNameAndParams.add(audioFile);
    	}
        progNameAndParams.add("-filter_complex");
        
        StringBuilder buff = new StringBuilder();
        if (File.separatorChar == '\\') {
            buff.append("\"");
        }
        int i = 0;
    	for (String audioFile: audioFiles) {
    		buff.append("[").append(i).append(":a:0]");
    		i++;
    	}
    	buff.append("concat=n=");
    	buff.append(audioFiles.size());
    	buff.append(":v=0:a=1[outa]");
        if (File.separatorChar == '\\') {
            buff.append("\"");
        }
    	progNameAndParams.add(buff.toString());       
        
        progNameAndParams.add("-map");
        
        if (File.separatorChar == '\\') {
            progNameAndParams.add("\"[outa]\"");
        } else {
            progNameAndParams.add("[outa]");
        }
        
        progNameAndParams.add(combinedAudioFilePath);
        
        buff = new StringBuilder();
        for (String cmdToken : progNameAndParams) {
            buff.append(cmdToken);
            buff.append(' ');
        }
        logger.debug("ffmpeg call with params: " + buff.toString());
        
		try {
			Process convertProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
			
	        BufferedReader convertProcessOut = new BufferedReader(new InputStreamReader(convertProcess.getErrorStream()));
	        
	        String outLine = null;
	        
	        while ((outLine = convertProcessOut.readLine()) != null) {
	            logger.debug("ffmpeg output: " + outLine);
	        }
			
			int convertResult = convertProcess.waitFor();
			
			if (convertResult == 0) {
				File resultFile = new File(combinedAudioFilePath);
				
				if (!resultFile.exists()) {
                    logger.error("result file from ffmpeg audio concatenation not found: " + combinedAudioFilePath);
				} else {
					return combinedAudioFilePath;
				}
			} else {
				logger.warn("ffmpeg returned error " + convertResult);
			}
		} catch (IOException | InterruptedException ioex) {
			logger.error("failed to concatenate audio files", ioex);
		}
		
		return null;
    }
}

