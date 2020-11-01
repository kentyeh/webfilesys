package de.webfilesys.gui.xsl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.graphics.ScaledImage;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class XslResizeParmsHandler extends XslRequestHandlerBase {
    private static final Logger LOG = Logger.getLogger(XslResizeParmsHandler.class);
	
	public XslResizeParmsHandler(
			HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}
	  
	protected void process() {
		if (!checkWriteAccess()) {
			return;
		}

		String currentPath = getCwd();

		String imgFilePath = getParameter("imgFile");

		boolean anyJpegFileSelected = false;
		
		if (imgFilePath == null) {
			// save selected files in session
			
	        ArrayList<String> selectedFiles = new ArrayList<String>();

	        Enumeration allKeys = req.getParameterNames();

	        while (allKeys.hasMoreElements())
	        {
	            String parmKey = (String) allKeys.nextElement();

	            if (parmKey.startsWith("list-"))
	            {
	                selectedFiles.add(parmKey.substring(5));
	            }
	        }

	        session.setAttribute("selectedFiles", selectedFiles);

	        for (String imgFile : selectedFiles) {
	        	if (imgFile.toLowerCase().endsWith("jpg") || imgFile.toLowerCase().endsWith("jpeg")) {
	        		anyJpegFileSelected = true;
	        	}
	        }
		}
		
		Element resizeParamsElement = doc.createElement("resizeParams");
			
		doc.appendChild(resizeParamsElement);

		ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/webfilesys/xsl/resizeParams.xsl\"");

		doc.insertBefore(xslRef, resizeParamsElement);

		String popup = getParameter("popup");
		
		int fileNameDislayLength = (popup == null ? 80 : 50);
		
		String shortPath = CommonUtils.shortName(getHeadlinePath(currentPath), fileNameDislayLength);
		
		XmlUtil.setChildText(resizeParamsElement, "shortPath", shortPath, false);

		if (imgFilePath != null) {
			String shortImgPath = CommonUtils.shortName(getHeadlinePath(imgFilePath), fileNameDislayLength);
			XmlUtil.setChildText(resizeParamsElement, "shortImgPath", shortImgPath, false);
		}
		
		XmlUtil.setChildText(resizeParamsElement, "imageFolderPath", currentPath, false);
		
		if (imgFilePath != null) {
			XmlUtil.setChildText(resizeParamsElement, "imageFilePath", imgFilePath, false);

			XmlUtil.setChildText(resizeParamsElement, "singleImage", "true", false);
		} else {
			XmlUtil.setChildText(resizeParamsElement, "multiImage", "true", false);
		}
		
		if (popup != null) {
			// resize in popup window (called from showImage)
			XmlUtil.setChildText(resizeParamsElement, "popup", "true", false);
		} else {
			if (imgFilePath != null) {
				XmlUtil.setChildText(resizeParamsElement, "cropEnabled", "true", false);
			}
		}
		
		if (imgFilePath != null) {
	        String imgSrc = "/webfilesys/servlet?command=getFile&filePath=" + UTF8URLEncoder.encode(imgFilePath);
			XmlUtil.setChildText(resizeParamsElement, "imgSrc", imgSrc, false);

			int thumbnailWidth = 400;
			int thumbnailHeight = 400;
			
	        try {
	            ScaledImage scaledImage = new ScaledImage(imgFilePath, 400, 400);
	            
				CameraExifData exifData = new CameraExifData(imgFilePath);
				if ((exifData.getThumbnailLength() <= 0) && 
	                ((exifData.getOrientation() == 6) || (exifData.getOrientation() == 8))) {
		            thumbnailWidth = scaledImage.getScaledHeight();
		    		thumbnailHeight = scaledImage.getScaledWidth();
	            } else {
		            thumbnailWidth = scaledImage.getScaledWidth();
		    		thumbnailHeight = scaledImage.getScaledHeight();
	            }

	    		XmlUtil.setChildText(resizeParamsElement, "imageType", Integer.toString(scaledImage.getImageType()), false);
	        } catch (IOException ioEx) {
	            LOG.error("failed to get image dimensions", ioEx);
	        }
	        
			XmlUtil.setChildText(resizeParamsElement, "thumbnailWidth", Integer.toString(thumbnailWidth), false);
			XmlUtil.setChildText(resizeParamsElement, "thumbnailHeight", Integer.toString(thumbnailHeight), false);
		} else {
			if (anyJpegFileSelected) {
	    		XmlUtil.setChildText(resizeParamsElement, "imageType", Integer.toString(ScaledImage.IMG_TYPE_JPEG), false);
			}
		}
		
		processResponse("resizeParams.xsl");
    }
}