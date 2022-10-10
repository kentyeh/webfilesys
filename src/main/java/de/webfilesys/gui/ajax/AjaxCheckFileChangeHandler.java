package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class AjaxCheckFileChangeHandler extends XmlRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(AjaxCheckFileChangeHandler.class);
	public AjaxCheckFileChangeHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}
	
	protected void process()
	{
        String filePath = getParameter("filePath");
        
        if (filePath == null) {
            return;
        }
        
        if (!checkAccess(filePath))
        {
            return;
        }
        
        String lastModifiedParam = getParameter("lastModified");
        
        if (lastModifiedParam == null)
        {
            return;
        }
        
        long lastModifiedOld;
        
        try
        {
            lastModifiedOld = Long.parseLong(lastModifiedParam);
        }
        catch (NumberFormatException ex)
        {
            logger.warn(ex);
            return;
        }

        String sizeParam = getParameter("size");
        
        if (sizeParam == null)
        {
            return;
        }
        
        long sizeOld;
        
        try
        {
            sizeOld = Long.parseLong(sizeParam);
        }
        catch (NumberFormatException ex)
        {
            logger.warn(ex);
            return;
        }
        
        File fileToCheck = new File(filePath);
        
        if ((!fileToCheck.exists()) || (!fileToCheck.canRead()))
        {
            logger.warn("{} is not a readable file", filePath);
            return;
        }
        
        Element resultElement = doc.createElement("result");
        
        boolean fileChanged = (fileToCheck.lastModified() > lastModifiedOld) || (fileToCheck.length() != sizeOld);
        
        XmlUtil.setElementText(resultElement, Boolean.toString(fileChanged));
        
        doc.appendChild(resultElement);
		
		processResponse();
	}
}
