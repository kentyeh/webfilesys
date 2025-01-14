package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.Category;
import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.gui.xsl.XslThumbnailHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Rename JPEG files according to the exposure date extracted from the Camera Exif
 * data.
 * @author Frank Hoehnel
 */
public class RenameToExifDateHandler extends MultiImageRequestHandler
{
    private static final Logger logger = LogManager.getLogger(RenameToExifDateHandler.class);
	private static final String DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";
	
	boolean clientIsLocal = false;
	
	public RenameToExifDateHandler(
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

		for (String selectedFile : selectedFiles)
		{
			String imgFileName = actPath + File.separator + selectedFile;

			String fileExt="";

			int extStart = imgFileName.lastIndexOf('.');
			if (extStart>0)
			{
				fileExt = imgFileName.substring(extStart).toUpperCase();
			}
            
            if (fileExt.equals(".JPG") || fileExt.equals(".JPEG"))
            {
				CameraExifData exifData=new CameraExifData(imgFileName);

				if (exifData.hasExifData())
				{
					Date exposureDate = exifData.getExposureDate();
				
					if (exposureDate != null)
					{
						SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

						File newImgFile = new File(getCwd(), dateFormat.format(exposureDate) + fileExt);
						
						File imgFile = new File(imgFileName);
						
						if (!imgFile.renameTo(newImgFile))
						{
							logger.error("could not rename image file " + imgFileName + " to exif exposure date: + " + newImgFile);
						}
						else
						{
							newImgFile.setLastModified(exposureDate.getTime());
							
							MetaInfManager metaInfMgr=MetaInfManager.getInstance();

							String description=metaInfMgr.getDescription(imgFileName);

							if ((description!=null) && (description.trim().length()>0))
							{
								metaInfMgr.setDescription(newImgFile.getAbsolutePath(),description);
							}

							ArrayList<Category> assignedCategories = metaInfMgr.getListOfCategories(imgFileName);
		
							if (assignedCategories != null)
							{
                                                            for (Category cat : assignedCategories) {
                                                                metaInfMgr.addCategory(newImgFile.getAbsolutePath(), cat);
                                                            }
							}

							metaInfMgr.removeMetaInf(imgFileName);
						}
					}
				}
            }
		}

	    (new XslThumbnailHandler(req, resp, session, output, uid, clientIsLocal)).handleRequest(); 

	}

}
