package de.webfilesys.gui.xsl;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Show geographic location of all pictures of the directory in open street map.
 * @author Frank Hoehnel
 */
public class XslOpenStreetMapFilesHandler extends XslRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XslOpenStreetMapFilesHandler.class);
	public XslOpenStreetMapFilesHandler(
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
		String path = getParameter("path");

		if (!accessAllowed(path))
		{
			logger.warn("user " + uid + " tried to access folder outside of his document root: " + path);
			
			return;
		}
		
		File folder = new File(path);
		
		if (!folder.exists())
		{
			logger.error("folder not found: " + path);
			
			return;
		}
		
        String shortPath = CommonUtils.shortName(getHeadlinePath(path), 50);
		
		Element geoTagElement = doc.createElement("geoTag");
			
		doc.appendChild(geoTagElement);

		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/webfilesys/xsl/openStreetMapFiles.xsl\"");

		doc.insertBefore(xslRef, geoTagElement);

		XmlUtil.setChildText(geoTagElement, "path", path, false);
		XmlUtil.setChildText(geoTagElement, "pathForScript", insertDoubleBackslash(path), false);
		XmlUtil.setChildText(geoTagElement, "shortPath", shortPath, false);
		
		processResponse("openStreetMapFiles.xsl", true);
    }
	
}