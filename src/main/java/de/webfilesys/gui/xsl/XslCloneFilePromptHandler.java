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
public class XslCloneFilePromptHandler extends XslRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XslCloneFilePromptHandler.class);
	public XslCloneFilePromptHandler(
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

		String sourceFileName = req.getParameter("fileName");
		
		if ((sourceFileName == null) || (sourceFileName.trim().length() == 0))
		{
			logger.error("required parameter fileName missing");
			
			return;
		}
		
		Element cloneFileElement = doc.createElement("cloneFile");
			
		doc.appendChild(cloneFileElement);
			
		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\""+ req.getContextPath() +"/xsl/cloneFile.xsl\"");

		doc.insertBefore(xslRef, cloneFileElement);

		XmlUtil.setChildText(cloneFileElement, "sourceFileName", sourceFileName, false);

		XmlUtil.setChildText(cloneFileElement, "shortFileName", CommonUtils.shortName(sourceFileName, 36), false);
		
		resp.setContentType("text/xml");
		
		BufferedWriter xmlOutFile = new BufferedWriter(output);
            
		XmlUtil.writeToStream(doc, xmlOutFile);

		output.flush();
	}
}