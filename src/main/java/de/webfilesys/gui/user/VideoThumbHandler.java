package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.FileLink;
import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.VideoThumbnailCreator;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.MimeTypeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class VideoThumbHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(VideoThumbHandler.class);
	protected HttpServletResponse resp = null;
	
	public VideoThumbHandler(
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
		String videoFileName = getParameter("videoFile");

		String currentPath = getCwd();
		
		if (CommonUtils.isEmpty(currentPath)) {
			return;
		}

		String videoPath;

		boolean isLink = (getParameter("link") != null);
		if (isLink) {
			FileLink link = MetaInfManager.getInstance().getLink(currentPath, videoFileName);
			if (link != null) {
				videoPath = link.getDestPath();
				if (!accessAllowed(videoPath)) {
					logger.warn("unauthorized access to file " + videoPath);
					try {
						resp.sendError(HttpServletResponse.SC_FORBIDDEN);
					} catch (IOException ioex) {
					}
					return;
				}
			} else {
				logger.warn("invalid link: " + videoFileName);
				try {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (IOException ioex) {
				}
				return;
			}
		} else {
			if (currentPath.endsWith(File.separator)) {
				videoPath = currentPath + videoFileName;
			} else {
				videoPath = currentPath + File.separator + videoFileName;
			}
		}
		
		File videoFile = new File(videoPath);
		
		String thumbnailPath = VideoThumbnailCreator.getThumbnailPath(videoPath);

		File thumbnailFile = new File(thumbnailPath);
		if (thumbnailFile.exists() && thumbnailFile.isFile() && thumbnailFile.canRead()) {
		    if (thumbnailFile.lastModified() > videoFile.lastModified()) {
	            serveImageFromFile(thumbnailPath, true);
	            return;
		    }
		}
		
		VideoThumbnailCreator thumbCreator = new VideoThumbnailCreator(videoPath);
		
		thumbCreator.start();
		
		try {
			thumbCreator.join();

			if (thumbnailFile.exists() && thumbnailFile.isFile() && thumbnailFile.canRead()) {
				serveImageFromFile(thumbnailPath, true);
				return;
			}
			logger.error("new created video thumbnail file not found: " + thumbnailPath);
			throw new RuntimeException("failed to create video thumbnail");
		} catch (InterruptedException ex) {
			logger.error("error occured while waiting for video thumbnail creator thread for video file " + videoPath, ex);
		}
	}
	
	private void serveImageFromFile(String imgPath, boolean isThumbnail) {
		
        File fileToSend = new File(imgPath);
        
        if (fileToSend.exists() && fileToSend.isFile() && (fileToSend.canRead())) {
        	
    		resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
    		resp.setDateHeader("expires", 0l); 
    		
    		String mimeType = MimeTypeMap.getInstance().getMimeType(imgPath);
    		
    		resp.setContentType(mimeType);
        	
        	long fileSize = fileToSend.length();
        	
        	resp.setContentLength((int) fileSize);

        	byte buffer[] = null;
        	
            if (fileSize < 16192) {
                buffer = new byte[16192];
            } else {
                buffer = new byte[65536];
            }
        	
        	try (OutputStream byteOut = resp.getOutputStream();
                        FileInputStream fileInput = new FileInputStream(fileToSend)){
        		
        		int bytesRead = 0;
        		long bytesWritten = 0;
        		
                while ((bytesRead = fileInput.read(buffer)) >= 0) {
                    byteOut.write(buffer, 0, bytesRead);
                    bytesWritten += bytesRead;
                }

                if (bytesWritten != fileSize) {
                    logger.warn(
                        "only " + bytesWritten + " bytes of " + fileSize + " have been written to output");
                } 

                byteOut.flush();
                
                buffer = null;

                if (!isThumbnail) {
            		if (WebFileSys.getInstance().isDownloadStatistics()) {
            			MetaInfManager.getInstance().incrementDownloads(imgPath);
            		}
                }
        	} catch (IOException ioEx) {
            	logger.warn(ioEx);
            }
        } else {
        	logger.error(imgPath + " is not a readable file");
        }
	}
	
}
