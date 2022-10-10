package de.webfilesys.gui.user;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.ViewHandlerConfig;
import de.webfilesys.ViewHandlerManager;
import de.webfilesys.util.FileEncodingMap;
import de.webfilesys.util.MimeTypeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * View a file contained in a ZIP archive.
 * @author Frank Hoehnel
 */
public class ZipContentFileRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(ZipContentFileRequestHandler.class);
	public ZipContentFileRequestHandler(
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
        String zipFilePath = getParameter("zipFilePath");
        String zipContentPath = getParameter("zipContentPath");

		if ((zipFilePath == null) || (zipContentPath == null))
		{
            return;
		}
		
		if (!checkAccess(zipFilePath))
		{
			return;
		}

		zipContentPath = zipContentPath.replace("\\", "/");
        
        try (ZipFile zipFile = new ZipFile(zipFilePath))
        {
                
            ZipEntry zipEntry = zipFile.getEntry(zipContentPath);
                
            if (zipEntry != null) 
            {
                long fileSize;
                byte[] buffer;
                long bytesWritten;
                try (InputStream zipInFile = zipFile.getInputStream(zipEntry);
                        OutputStream respOut = resp.getOutputStream()) {
                    ViewHandlerConfig viewHandlerConfig = ViewHandlerManager.getInstance().getViewHandlerConfig(zipContentPath);
                    if (viewHandlerConfig != null)
                    {
                        String viewHandlerClassName = viewHandlerConfig.getHandlerClass();
                        
                        if (viewHandlerClassName != null)
                        {
                            if (delegateToViewHandler(viewHandlerConfig, zipEntry.getName(), zipInFile))
                            {
                                return;
                            }
                        }
                    }   String mimeType = MimeTypeMap.getInstance().getMimeType(zipContentPath);
                    resp.setContentType(mimeType);
                    String encoding = FileEncodingMap.getInstance().getFileEncoding(zipContentPath);
                    if (encoding != null) {
                        resp.setCharacterEncoding(encoding);
                    }   fileSize = zipEntry.getSize();
                    resp.setContentLength((int) fileSize);
                    buffer = null;
                    if (fileSize < 16192)
                    {
                        buffer = new byte[(int) fileSize];
                    }
                    else
                    {
                        buffer = new byte[65536];
                    }
                    int count = 0;
                    bytesWritten = 0;
                    while ((count = zipInFile.read(buffer)) >= 0)
                    {
                        respOut.write(buffer, 0, count);
                        
                    bytesWritten += count;
                }
                    
                if (bytesWritten != fileSize)
                {
                    logger.warn(
                        "only " + bytesWritten + " bytes of " + fileSize + " have been written to output");
                }

                respOut.flush();
                
                buffer = null;
                }
            }
        }
        catch (IOException ioex)
        {
            logger.error("cannot read ZIP file content " + zipContentPath + " from " + zipFilePath, ioex);
        }
    }
}
