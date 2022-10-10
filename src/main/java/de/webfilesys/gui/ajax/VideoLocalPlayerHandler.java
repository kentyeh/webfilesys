package de.webfilesys.gui.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.WebFileSys;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class VideoLocalPlayerHandler extends XmlRequestHandlerBase {
	private static final Logger logger = LogManager.getLogger(VideoLocalPlayerHandler.class);
	private boolean clientIsLocal = false;
	
	public VideoLocalPlayerHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			PrintWriter output, String uid, boolean requestIsLocal) {
		super(req, resp, session, output, uid);
		
		clientIsLocal = requestIsLocal;
	}

        @Override
	protected void process() {
		if (!clientIsLocal) {
			logger.warn("remote user tried to start local video player");
			return;
		}
		
        String videoFilePath = getParameter("videoPath");
        
		if (!checkAccess(videoFilePath)) {
			return;
		}

		int rc = playVideoInLocalPlayer(videoFilePath);
		
		Element resultElement = doc.createElement("result");

		XmlUtil.setChildText(resultElement, "success", Boolean.toString(rc == 0));

		doc.appendChild(resultElement);

		this.processResponse();
	}

	private int playVideoInLocalPlayer(String videoFilePath) {
        String videoPlayerExePath = WebFileSys.getInstance().getVideoPlayerExePath();
        
        if (CommonUtils.isEmpty(videoPlayerExePath)) {
            return -1;
        }

        try {
            ArrayList<String> progNameAndParams = new ArrayList<>();
            progNameAndParams.add(videoPlayerExePath);
        	
        	String addParams = WebFileSys.getInstance().getVideoPlayerAddParams();
            if (addParams != null) {
            	String[] params = addParams.split(" ");
                progNameAndParams.addAll(Arrays.asList(params));
            }
            
            progNameAndParams.add(videoFilePath);

            StringBuilder buff = new StringBuilder();
            for (String cmdToken : progNameAndParams) {
                	buff.append(cmdToken);
                	buff.append(' ');
             }
             logger.debug("ffplay call with params: " + buff.toString());
        	
			Process ffplayProcess = Runtime.getRuntime().exec(progNameAndParams.toArray(new String[0]));
			
	        BufferedReader ffplayOut = new BufferedReader(new InputStreamReader(ffplayProcess.getErrorStream()));
	        
	        String outLine = null;
	        
	        while ((outLine = ffplayOut.readLine()) != null) {
	            logger.debug("video player output: " + outLine);
	        }
			
			int ffplayResult = ffplayProcess.waitFor();
			return ffplayResult;
		} catch (IOException | InterruptedException ioex) {
			logger.error("failed to play video " + videoFilePath, ioex);
		}

        return -1;
	}
}
