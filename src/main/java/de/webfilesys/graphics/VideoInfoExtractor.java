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

public class VideoInfoExtractor {

    private static final Logger logger= LogManager.getLogger(VideoInfoExtractor.class);

    private final ArrayList<String> cmdOutput = new ArrayList<>();
    
    public VideoInfo getVideoInfo(String videoFilePath) 
    throws IllegalArgumentException {

        VideoInfo videoInfo = new VideoInfo();
    	
        videoInfo.setFfprobeResult(readPprobeOutput(videoFilePath));

        if (cmdOutput.isEmpty()) {
        	videoInfo.setFfprobeEmptyOutput(true);
        } else {
            String numberOfVideoStream = getNumberOfVideoStream();
            if (numberOfVideoStream == null) {
            	logger.debug("no video stream info found in ffprobe result for file " + videoFilePath);
            	videoInfo.setFfprobeEmptyOutput(true);
            } else {
                String videoWidth = null;
                String videoHeight = null;
                String codec = null;
                String duration = null;
                String averageFrameRate = null;
                String audioCodec = null;
 
                String streamPrefix = "stream_" + numberOfVideoStream + "_";
                
                for (String outLine : cmdOutput) {
                    if ((videoWidth == null) && outLine.contains(streamPrefix + "width")) {
                        String[] tokens = outLine.split("=");
                        videoWidth = tokens[1];
                        try {
                            videoInfo.setWidth(Integer.parseInt(videoWidth));
                        } catch (Exception ex) {
                            logger.warn("invalid video width: " + videoWidth);
                        }
                    } else if ((videoHeight == null) && outLine.contains(streamPrefix + "height")) {
                        String[] tokens = outLine.split("=");
                        videoHeight = tokens[1];
                        try {
                            videoInfo.setHeight(Integer.parseInt(videoHeight));
                        } catch (Exception ex) {
                            logger.warn("invalid video height: " + videoHeight);
                        }
                    } else if ((codec == null) && outLine.contains(streamPrefix + "codec")) {
                        String[] tokens = outLine.split("=");
                        codec = tokens[1].substring(1, tokens[1].length() - 1);
                        videoInfo.setCodec(codec);
                    } else if ((duration == null) && outLine.contains(streamPrefix + "duration")) {
                        // streams_stream_0_duration="0:04:36.400000"
                        String[] tokens = outLine.split("=");
                        if (tokens[1].length() > 6) {
                            duration = tokens[1].substring(1, 8);
                            videoInfo.setDuration(duration);
                            
                            String[] partsOfDuration = duration.split(":");
                            if (partsOfDuration.length == 3) {
                                try {
                                    int durationSeconds = (Integer.parseInt(partsOfDuration[0]) * 3600) + (Integer.parseInt(partsOfDuration[1]) * 60) + Integer.parseInt(partsOfDuration[2]);
                                    videoInfo.setDurationSeconds(durationSeconds);
                                } catch (Exception ex) {
                                    logger.warn("invalid video duration: " + duration);
                                }
                            }
                        }
                    } else if ((averageFrameRate == null) && outLine.contains(streamPrefix + "avg_frame_rate")) {
                        String[] tokens = outLine.split("=");
                        averageFrameRate = tokens[1].substring(1, tokens[1].length() - 1);
                        tokens =  averageFrameRate.split("/");
                        if (tokens.length == 2) {
                            try {
                                int frameRatePart1 = Integer.parseInt(tokens[0]);
                                int frameRatePart2 = Integer.parseInt(tokens[1]);
                                videoInfo.setFrameRate(frameRatePart1 / frameRatePart2);
                            } catch (Exception ex) {
                                logger.warn("invalid frame rate for " + videoFilePath + ": " + averageFrameRate);
                            }
                        }
                    } else if ((audioCodec == null) && outLine.contains("_codec")) {
                        String[] tokens = outLine.split("=");
                        audioCodec = tokens[1].substring(1, tokens[1].length() - 1);
                        videoInfo.setAudioCodec(audioCodec);
                    }   
            	}
            }
        }
        
        return videoInfo;
    }
    
    private int readPprobeOutput(String videoFilePath) 
    throws IllegalArgumentException {

        File videoFile = new File(videoFilePath);

        if ((!videoFile.exists()) || (!videoFile.isFile()) || (!videoFile.canRead())) {
            logger.warn("not a readable file: " + videoFilePath);
            throw new IllegalArgumentException("video file is not a readable file: " + videoFilePath);
        }        

        String ffprobeExePath = WebFileSys.getInstance().getFfprobeExePath();
        
        if (CommonUtils.isEmpty(ffprobeExePath)) {
            throw new IllegalArgumentException("ffprobe executable path not configured");
        }
        
        try {
            // String progNameAndParams = ffprobeExePath +  " -v error -of flat=s=_ -select_streams v:0 -show_entries stream=height,width,codec_name,duration,avg_frame_rate -sexagesimal " + videoFile.getAbsolutePath();
            
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(ffprobeExePath);
            progNameAndParams.add("-v");
            progNameAndParams.add("error");
            progNameAndParams.add("-of");
            progNameAndParams.add("flat=s=_");
            // progNameAndParams.add("-select_streams");
            // progNameAndParams.add("v:0");
            progNameAndParams.add("-show_entries");
            progNameAndParams.add("stream=height,width,codec_name,duration,avg_frame_rate");
            progNameAndParams.add("-sexagesimal");
            progNameAndParams.add(videoFile.getAbsolutePath());
            
            StringBuilder buff = new StringBuilder();
            for (String cmdToken : progNameAndParams) {
                buff.append(cmdToken);
                buff.append(' ');
            }
            logger.debug("ffprobe call with params: " + buff.toString());
            
            Process ffprobeProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
            
            BufferedReader ffprobeOut = new BufferedReader(new InputStreamReader(ffprobeProcess.getInputStream()));
            
            String outLine = null;
            
            while ((outLine = ffprobeOut.readLine()) != null) {
                logger.debug("ffprobe output: " + outLine);
                cmdOutput.add(outLine);
            }
            
            int ffprobeResult = ffprobeProcess.waitFor();
            if (ffprobeResult != 0) {
                logger.warn("ffprobe returned error " + ffprobeResult);
            }
            
            return ffprobeResult;
        } catch (IOException | InterruptedException ioex) {
            logger.error("failed to get video info for file " + videoFile, ioex);
        }
        
        return -1;
    }
    
    private String getNumberOfVideoStream() {
    	for (String outLine : cmdOutput) {
    		if (outLine.contains("_width")) {
    			// streams_stream_1_width=1280
    			String[] partsOfLine = outLine.split("_");
    			if (partsOfLine.length > 2) {
    				return partsOfLine[2];
    			}
    		}
    	}
    	return null;
    }
    
}
