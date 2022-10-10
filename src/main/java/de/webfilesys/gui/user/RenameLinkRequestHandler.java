package de.webfilesys.gui.user;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.Constants;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.xsl.XslFileListHandler;
import de.webfilesys.gui.xsl.XslThumbnailHandler;
import de.webfilesys.gui.xsl.XslVideoListHandler;
import de.webfilesys.gui.xsl.mobile.MobileFolderFileListHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class RenameLinkRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(RenameLinkRequestHandler.class);
	boolean clientIsLocal = false;
	
	public RenameLinkRequestHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid,
	        boolean clientIsLocal)
	{
        super(req, resp, session, output, uid);

		this.clientIsLocal = clientIsLocal;
	}

        @Override
	protected void process()
	{
		if (!checkWriteAccess())
		{
			return;
		}

		String actPath = getCwd();

		if (!checkAccess(actPath))
		{
			return;
		}
		
		String newLinkName = getParameter("newLinkName");

		if (newLinkName == null)
		{
			logger.fatal("missing parameter newLinkName");
			return;
		}

		String oldLinkName = getParameter("oldLinkName");
		
		if (oldLinkName == null)
		{
			logger.fatal("missing parameter oldLinkName");
			return;
		}

		MetaInfManager.getInstance().renameLink(actPath, oldLinkName, newLinkName);

        int viewMode = Constants.VIEW_MODE_LIST;		
		
	    Integer sessionViewMode = (Integer) session.getAttribute("viewMode");
	    
	    if (sessionViewMode != null)
	    {
	    	viewMode = sessionViewMode;
	    }
	    
        String mobile = (String) session.getAttribute("mobile");
        
        if (mobile != null) {
            (new MobileFolderFileListHandler(req, resp, session, output, uid)).handleRequest(); 
        } else {
            if (viewMode == Constants.VIEW_MODE_THUMBS) {
                (new XslThumbnailHandler(req, resp, session, output, uid, clientIsLocal)).handleRequest(); 
            } else if (viewMode == Constants.VIEW_MODE_VIDEO) {
                (new XslVideoListHandler(req, resp, session, output, uid, clientIsLocal)).handleRequest(); 
            } else {
                (new XslFileListHandler(req, resp, session, output, uid)).handleRequest();
            }
        }	    
	}
}
