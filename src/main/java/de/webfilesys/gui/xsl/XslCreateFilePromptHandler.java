package de.webfilesys.gui.xsl;

import java.io.BufferedWriter;
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
 * @author Frank Hoehnel
 */
public class XslCreateFilePromptHandler extends XslRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XslCreateFilePromptHandler.class);
	public XslCreateFilePromptHandler(
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
		if (!checkWriteAccess())
		{
			return;
		}
 
		String relPath = null;
		
		String currentPath = getParameter("path");

        if (isMobile()) 
        {
            relPath = currentPath;
            currentPath = getAbsolutePath(currentPath);
        }
		
		if ((currentPath == null) || (currentPath.trim().length() == 0))
		{
			logger.error("required parameter path missing");
			
			return;
		}
		
		if (!checkAccess(currentPath))
		{
			return;
		}

		Element createFileElement = doc.createElement("createFile");
			
		doc.appendChild(createFileElement);
			
		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\""+ req.getContextPath() +"/xsl/createFile.xsl\"");

		doc.insertBefore(xslRef, createFileElement);

		XmlUtil.setChildText(createFileElement, "baseFolder", currentPath, false);

        String shortPath = null;
        
        if (relPath != null) 
        {
            shortPath = CommonUtils.shortName(relPath, 32);
        }
        else
        {
            shortPath = CommonUtils.shortName(getHeadlinePath(currentPath), 32);
        }
		
		XmlUtil.setChildText(createFileElement, "baseFolderShort", shortPath, false);
		
		resp.setContentType("text/xml");
		
		BufferedWriter xmlOutFile = new BufferedWriter(output);
            
		XmlUtil.writeToStream(doc, xmlOutFile);

		output.flush();
	}
}