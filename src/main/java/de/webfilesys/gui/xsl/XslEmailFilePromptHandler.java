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
public class XslEmailFilePromptHandler extends XslRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XslEmailFilePromptHandler.class);
	public XslEmailFilePromptHandler(
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
		String fileToSend = req.getParameter("fileName");

        // filePath is used from file link context menu
        String filePath = req.getParameter("filePath");

        if ((fileToSend == null) || (fileToSend.trim().length() == 0))
		{
            if ((filePath == null) || (filePath.trim().length() == 0))
            {
                logger.error("required parameter fileName missing");
                return;
            }
            
            fileToSend = CommonUtils.extractFileName(filePath);
		}
		
		Element emailFileElement = doc.createElement("emailFile");
			
		doc.appendChild(emailFileElement);
			
		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/webfilesys/xsl/emailFile.xsl\"");

		doc.insertBefore(xslRef, emailFileElement);

		XmlUtil.setChildText(emailFileElement, "fileName", fileToSend, false);
        
        if (filePath != null) {
            XmlUtil.setChildText(emailFileElement, "filePath", filePath, false);
        }

		XmlUtil.setChildText(emailFileElement, "shortFileName", CommonUtils.shortName(fileToSend, 36), false);
		
		resp.setContentType("text/xml");
		
		BufferedWriter xmlOutFile = new BufferedWriter(output);
            
		XmlUtil.writeToStream(doc, xmlOutFile);

		output.flush();
	}
}