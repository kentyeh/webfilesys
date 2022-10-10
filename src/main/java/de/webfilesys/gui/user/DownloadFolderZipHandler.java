package de.webfilesys.gui.user;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author Frank Hoehnel
 */
public class DownloadFolderZipHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(DownloadFolderZipHandler.class);
	public DownloadFolderZipHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}

        @Override
	protected void process()
	{
		String path = getParameter("path");

		if (!checkAccess(path))
		{
		    return;	
		}

		String errorMsg = null;
		
        File folderFile = new File(path);
        
        if ((!folderFile.exists()) || (!folderFile.isDirectory()) || (!folderFile.canRead()))
        {
            errorMsg = "folder is not a readable directory: " + path;
        }

        String dirName = null;
        
        int lastSepIdx = path.lastIndexOf(File.separatorChar);
        
        if (lastSepIdx < 0) 
        {
            lastSepIdx = path.lastIndexOf('/');
        }
        
        if ((lastSepIdx < 0) || (lastSepIdx == path.length() - 1))
        {
            errorMsg = "invalid path for folder download: " + path;
        }
        else
        {
            dirName = path.substring(lastSepIdx + 1);
        }
        
        if (errorMsg != null)
        {
        	logger.warn(errorMsg);
        	
            resp.setStatus(404);

            try(PrintWriter output = new PrintWriter(resp.getWriter()))
    		{
    			
    			output.println(errorMsg);
    			
    			output.flush();
    			
    			return;
    		}
            catch (IOException ioEx)
            {
            	logger.warn(ioEx);
            }
        }

        resp.setContentType("application/zip");

        resp.setHeader("Content-Disposition", "attachment; filename=" + dirName + ".zip");

		try(BufferedOutputStream buffOut = new BufferedOutputStream(resp.getOutputStream());
                        ZipOutputStream zipOut = new ZipOutputStream(buffOut))
		{
			zipFolderTree(path, "", zipOut);
			
			buffOut.flush();
		}
        catch (IOException ioEx)
        {
        	logger.warn(ioEx);
        }
	}
	
    private void zipFolderTree(String actPath, String relativePath, ZipOutputStream zipOut)
    {
        File actDir = new File(actPath);

        String fileList[]=actDir.list();

        if ((fileList == null) || (fileList.length == 0))
        {
            return;
        }

        byte buff[] = new byte[4096];

            for (String fileList1 : fileList) {
                File tempFile = new File(actPath + File.separator + fileList1);
                if (tempFile.isDirectory()) {
                    zipFolderTree(actPath + File.separator + fileList1, relativePath + fileList1 + "/", zipOut);
                } else {
                    String fullFileName = actPath + File.separator + fileList1;
                    String relativeFileName = relativePath + fileList1;
                    try
                    {
                        ZipEntry newZipEntry = new ZipEntry(relativeFileName);
                        
                        zipOut.putNextEntry(newZipEntry);
                        
                        try(BufferedInputStream inStream = new BufferedInputStream(
                                new FileInputStream(new File(fullFileName))))
                        {
                            
                            int count;
                            
                            while ((count = inStream.read(buff)) >= 0)
                            {
                                zipOut.write(buff,0,count);
                            }
                        }
                        catch (Exception zioe)
                        {
                            logger.warn("failed to zip file " + fullFileName, zioe);
                        }
                    }
                    catch (IOException ioex)
                    {
                        logger.error("failed to zip file " + fullFileName, ioex);
                    }
                }
            }
    }
}
