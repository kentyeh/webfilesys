package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.SubdirExistCache;
import de.webfilesys.graphics.ThumbnailThread;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class TestSubdirExistHandler extends XmlRequestHandlerBase {
	
    private static final Logger logger = LogManager.getLogger(TestSubdirExistHandler.class);
	
	public TestSubdirExistHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}
	
        @Override
	protected void process() {
        String path = getParameter("path");

        if (CommonUtils.isEmpty(path)) {
        	logger.error("parameter path missing");
        	return;
        }
        
		if (!checkAccess(path)) {
			return;
		}
        
		boolean subdirExists = false;
		
        File folder = new File(path);
        
        if (folder.exists() && folder.isDirectory() && folder.canRead()) {
        	File[] files = folder.listFiles();
        	if (files != null) {
            	for (int i = 0; (!subdirExists) && (i < files.length); i++) {
            	    if (files[i].isDirectory()) {
						if (!files[i].getName().equals(ThumbnailThread.THUMBNAIL_SUBDIR)) {
            	    	    subdirExists = true;
						}
            	    }
            	}
        	}
        } else {
        	logger.warn("folder to check for subdirs is not a readable directory: " + path);
        }

        if (subdirExists) {
    	    SubdirExistCache.getInstance().setExistsSubdir(folder.getAbsolutePath(), 1);
        } else {
    	    SubdirExistCache.getInstance().setExistsSubdir(folder.getAbsolutePath(), 0);
        }
        
        Element resultElement = doc.createElement("result");
        
        XmlUtil.setElementText(resultElement, Boolean.toString(subdirExists));
        
        doc.appendChild(resultElement);
		
		processResponse();
	}
}
