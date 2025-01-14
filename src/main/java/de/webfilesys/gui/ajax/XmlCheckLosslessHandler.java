package de.webfilesys.gui.ajax;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.graphics.ScaledImage;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class XmlCheckLosslessHandler extends XmlRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XmlCheckLosslessHandler.class);
	public XmlCheckLosslessHandler(
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
		String imgPath = getParameter("imgPath");

		ScaledImage sourceImage = null;
		
        try
        {
            sourceImage = new ScaledImage(imgPath, 1000, 1000);
        }
        catch (IOException ioex)
        {
            logger.error(ioex);
            return;
        }

        boolean lossless = true;
        
        if ((sourceImage.getImageType() == ScaledImage.IMG_TYPE_BMP) ||
            (sourceImage.getImageType() == ScaledImage.IMG_TYPE_PNG) ||
            (sourceImage.getImageType() == ScaledImage.IMG_TYPE_GIF))
        {
            lossless = false;
        }
        else if ((sourceImage.getRealWidth() % 8 != 0) || (sourceImage.getRealHeight() % 8 != 0))
        {
        	lossless = false;
        }

		Element resultElement = doc.createElement("result");

		doc.appendChild(resultElement);
		
		XmlUtil.setChildText(resultElement, "lossless", "" + lossless);
		
		this.processResponse();
	}
}
