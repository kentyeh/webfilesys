package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiImageDownloadHandler extends MultiImageRequestHandler
{
    private static final Logger logger = LogManager.getLogger(MultiImageDownloadHandler.class);
	protected HttpServletResponse resp = null;

	public MultiImageDownloadHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);

	    this.resp = resp;
	}

        @Override
	protected void process()
	{
		ArrayList<String> selectedFiles = (ArrayList<String>) session.getAttribute("selectedFiles");
		
		if ((selectedFiles == null) || (selectedFiles.isEmpty()))
		{
			logger.debug("MultiImageDownloadHandler: no files selected");
			
			return;
		}
		
		String actPath = getCwd();
		
		if (actPath == null)
		{
			logger.error("MultiImageDownloadHandler: actPath is null");
			
			return;
		}
		
		if (isMobile()) 
		{
		    actPath = this.getAbsolutePath(actPath);
		}
		
		if (!checkAccess(actPath)) 
		{
		    return;
		}
		
		
		try
		{
			File tempFile  = File.createTempFile("fmweb",null);
			
                        int count;
			byte[] buffer;
                    try (ZipOutputStream zip_out = new ZipOutputStream(new FileOutputStream(tempFile));
                            FileInputStream fin = new FileInputStream(tempFile);
                            OutputStream byteOut = resp.getOutputStream()) {
                        count = 0;
                        buffer = new byte[16192];
                        for (String selectedFile : selectedFiles)
                        {
                            try (FileInputStream inFile = new FileInputStream(new File(actPath, selectedFile)))
                            {
                                zip_out.putNextEntry(new ZipEntry(selectedFile));
                                
                                count=0;
                                
                                while (( count = inFile.read(buffer)) >= 0 )
                                {
                                    zip_out.write(buffer,0,count);
                                }
                            }
                            catch (Exception zioe)
                            {
                                logger.warn("failed to add file to temporary zip archive", zioe);
                                return;
                            }
			}
			
			resp.setContentType("application/zip");

			resp.setHeader("Content-Disposition", "attachment; filename=fmwebDownload.zip");
			
			resp.setContentLength((int) tempFile.length());


			while ((count = fin.read(buffer)) >= 0)
			{
				byteOut.write(buffer, 0, count);
			}


			byteOut.flush();

			buffer = null;
			
			tempFile.delete();
			
			if (WebFileSys.getInstance().isDownloadStatistics())
			{
				for (String selectedFile : selectedFiles) 
				{
                    String fullPath = null;
                     
                    if (actPath.endsWith(File.separator))
                    {
					    fullPath = actPath + selectedFile;
                    }
                    else
                    {
					    fullPath = actPath + File.separator + selectedFile;
                    }
				
				    MetaInfManager.getInstance().incrementDownloads(fullPath);
				}
			}
		}
                }
        catch (IOException ioex)
        {
        	logger.error(ioex);
        	return;
        }
        
        session.removeAttribute("selectedFiles");
	}
}
