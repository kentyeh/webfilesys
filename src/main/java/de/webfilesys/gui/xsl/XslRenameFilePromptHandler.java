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
public class XslRenameFilePromptHandler extends XslRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XslRenameFilePromptHandler.class);
	public XslRenameFilePromptHandler(
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

		String oldFileName = req.getParameter("fileName");
		
		if ((oldFileName == null) || (oldFileName.trim().length() == 0))
		{
			logger.error("required parameter fileName missing");
			
			return;
		}
		
		Element renameFileElement = doc.createElement("renameFile");
			
		doc.appendChild(renameFileElement);
			
		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\""+ req.getContextPath() +"/xsl/renameFile.xsl\"");

		doc.insertBefore(xslRef, renameFileElement);

		XmlUtil.setChildText(renameFileElement, "oldFileName", oldFileName, false);
		XmlUtil.setChildText(renameFileElement, "oldFileNameForScript", escapeForJavascript(oldFileName), false);

		XmlUtil.setChildText(renameFileElement, "shortFileName", CommonUtils.shortName(oldFileName, 36), false);
		
        String mobile = getParameter("mobile");
        
        if (mobile != null)
        {
            XmlUtil.setChildText(renameFileElement, "mobile", "true", false);
        }
		
		resp.setContentType("text/xml");
		
		BufferedWriter xmlOutFile = new BufferedWriter(output);
            
		XmlUtil.writeToStream(doc, xmlOutFile);

		output.flush();
	}
}