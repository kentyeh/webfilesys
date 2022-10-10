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

public class TextOnVideoThread extends Thread {
    private static final Logger logger = LogManager.getLogger(TextOnVideoThread.class);
	public static final String TARGET_SUBDIR = "_text";
	
	public static final int TEXT_POSITION_TOP = 1;
	public static final int TEXT_POSITION_CENTER = 2;
	public static final int TEXT_POSITION_BOTTOM = 3;
	
    private String videoFilePath;
    
    private String text;
    
    private String textSize;
    
    private String textColor;
    
    private int textPosition;
    
    public TextOnVideoThread(String videoPath) {
    	videoFilePath = videoPath;
    }

    public void setText(String newVal) {
    	text = newVal;
    }

    public void setTextSize(String newVal) {
    	textSize = newVal;
    }
    
    public void setTextColor(String newVal) {
    	textColor = newVal;
    }
    
    public void setTextPosition(int newVal) {
    	textPosition = newVal;
    }
    
    @Override
    public void run() {
        logger.debug("starting text on video thread for video file " + videoFilePath);
        
        Thread.currentThread().setPriority(1);

        String ffmpegExePath = WebFileSys.getInstance().getFfmpegExePath();
        
        if (!CommonUtils.isEmpty(ffmpegExePath)) {

        	String[] partsOfPath = CommonUtils.splitPath(videoFilePath);
        	
        	String sourcePath = partsOfPath[0];
        	String sourceFileName = partsOfPath[1];

            String targetPath = sourcePath + File.separator + TARGET_SUBDIR;
        	
            File targetDirFile = new File(targetPath);
            if (!targetDirFile.exists()) {
                if (!targetDirFile.mkdir()) {
                    logger.error("failed to create target folder for video conversion: " + targetPath);
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
            
            String fontFilePath = WebFileSys.getInstance().getWebAppRootDir();
            if (!fontFilePath.endsWith(File.separator)) {
            	fontFilePath = fontFilePath + File.separator;
            }
            fontFilePath = fontFilePath + "fonts" + File.separator + "GoogleKiteOne.woff";
            
            String textYPositionParam = "(h-text_h)/2";
            if (textPosition == TEXT_POSITION_TOP) {
            	textYPositionParam = "60";
            } else if (textPosition == TEXT_POSITION_BOTTOM) {
            	textYPositionParam = "(h-text_h-60)";
            }
            
            // ffmpeg -i /tmp/video/DSCF8796.MOV -vf drawtext="fontfile=" + fontFilePath + ": text='Argentinien 2019': fontcolor=orange: fontsize=48: box=1: boxcolor=black@0: boxborderw=5: x=(w-text_w)/2: y=(h-text_h)/2" -codec:a copy /tmp/video/output.mov
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffmpegExePath);
            progNameAndParams.add("-i");
            progNameAndParams.add(videoFilePath);
            
            progNameAndParams.add("-vf");
            
            progNameAndParams.add("drawtext='fontfile=" + fontFilePath + ": text=" + text + ": fontcolor=" + textColor + ": fontsize=" + textSize + ": box=1: boxcolor=black@0: boxborderw=5: x=(w-text_w)/2: y=" + textYPositionParam + "'");
            
            progNameAndParams.add("-codec:a");
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
					SubdirExistCache.getInstance().setExistsSubdir(sourcePath, 1);
				} else {
					logger.warn("ffmpeg returned error " + convertResult);
				}
			} catch (IOException | InterruptedException ioex) {
				logger.error("failed to add text to video " + videoFilePath, ioex);
			}
        }
    }
    
}

