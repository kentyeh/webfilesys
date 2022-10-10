package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.Constants;
import de.webfilesys.FileComparator;
import de.webfilesys.FileContainer;
import de.webfilesys.FileLinkSelector;
import de.webfilesys.FileSelectionStatus;
import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.xsl.XslFileListHandler;
import de.webfilesys.gui.xsl.XslThumbnailHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Replace all file links in the current directory by a copy of the linked original file.
 * 
 * @author Frank Hoehnel
 */
public class CopyLinkRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(CopyLinkRequestHandler.class);
	protected HttpServletRequest req = null;

	protected HttpServletResponse resp = null;
	
	protected boolean clientIsLocal = false;

	public CopyLinkRequestHandler(
			HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid,
            boolean clientIsLocal)
	{
        super(req, resp, session, output, uid);

        this.req = req;
        
        this.resp = resp;
        
        this.clientIsLocal = clientIsLocal;
	}

        @Override
	protected void process()
	{
		if (!checkWriteAccess())
		{
			return;
		}

		String actPath = getParameter("actPath");

		if ((actPath == null) || (actPath.length() == 0))
		{
			actPath = getCwd();
		}

		if (!checkAccess(actPath))
		{
			return;
		}

		String[] fileMasks = null;
		
		Integer viewMode = (Integer) session.getAttribute("viewMode");
		
		if ((viewMode != null) && (viewMode == Constants.VIEW_MODE_THUMBS))
		{
		    fileMasks = Constants.imgFileMasks;
		}
		else
		{
		    fileMasks = new String[1];
		    fileMasks[0] = "*";
		}
		
		FileLinkSelector fileSelector = new FileLinkSelector(actPath, FileComparator.SORT_BY_FILENAME, true);

		FileSelectionStatus selectionStatus = fileSelector.selectFiles(Constants.imgFileMasks, Constants.MAX_FILE_NUM, 0);

		ArrayList<FileContainer> selectedFiles = selectionStatus.getSelectedFiles();

		if (selectedFiles != null)
		{
            MetaInfManager metaInfMgr = MetaInfManager.getInstance();
			
                    for (FileContainer fileCont : selectedFiles) {
                        if (fileCont.isLink())
                        {
                            String linkName = fileCont.getName();
                            
                            String targetPath = null;
                            
                            if (actPath.endsWith(File.separator))
                            {
                                targetPath = actPath + linkName;
                            }
                            else
                            {
                                targetPath = actPath + File.separatorChar + linkName;
                            }
                            
                            if (copyFile(fileCont.getRealFile().getAbsolutePath(), targetPath))
                            {
                                metaInfMgr.removeLink(actPath, linkName);
                                logger.debug("link " + linkName + " replaced by a copy of original file " + fileCont.getRealFile().getAbsolutePath());
                            }
                            else
                            {
                                logger.error("failed to replace link " + linkName + " by a copy of original file " + fileCont.getRealFile().getAbsolutePath());
                            }
                        }
                    }
		}

        if ((viewMode != null) && (viewMode == Constants.VIEW_MODE_THUMBS))
        {
            (new XslThumbnailHandler(req, resp, session, output, uid, clientIsLocal)).handleRequest(); 
        }
        else
        {
            (new XslFileListHandler(req, resp, session, output, uid)).handleRequest(); 
        }
	}
}
