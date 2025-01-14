package de.webfilesys.gui.ajax;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.ThumbnailThread;
import de.webfilesys.graphics.VideoThumbnailCreator;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delete a picture file.
 */
public class DeleteFileHandler extends XmlRequestHandlerBase {
    private static final Logger logger = LogManager.getLogger(DeleteFileHandler.class);
    public DeleteFileHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
            PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    @Override
    protected void process() {
        if (!checkWriteAccess()) {
            return;
        }

        String filePath = getParameter("filePath");

        if (filePath == null) {
        	String fileName = getParameter("fileName");
        	if (CommonUtils.isEmpty(fileName)) {
        		logger.warn("missing parameter filePath or fileName");
        		return;
        	}
        	filePath = getCwd();
        	if (filePath.endsWith(File.separator)) {
        		filePath = filePath + fileName;
        	} else {
        		filePath = filePath + File.separator + fileName;
        	}
        } else {
            if (!accessAllowed(filePath)) {
                logger.warn("user " + uid + " tried to delete file outside of it's document root: " + filePath);
                return;
            }
            if (File.separatorChar == '\\') {
                filePath = filePath.replace('/', '\\');
            }
        }

        String deleteWriteProtected = getParameter("deleteWriteProtected");
        
        boolean success = false;

        File fileToDelete = new File(filePath);
        
        String deletedFile = null;

        if (fileToDelete.exists() && fileToDelete.isFile() && 
            (fileToDelete.canWrite() || (deleteWriteProtected != null))) {
            deletedFile = fileToDelete.getName();
            
            MetaInfManager metaInfMgr = MetaInfManager.getInstance();

            if (WebFileSys.getInstance().isReverseFileLinkingEnabled()) {
                metaInfMgr.updateLinksAfterMove(filePath, null, uid);
            }

            metaInfMgr.removeMetaInf(filePath);

            String thumbnailPath = ThumbnailThread.getThumbnailPath(filePath);

            File thumbnailFile = new File(thumbnailPath);

            if (thumbnailFile.exists()) {
                if (!thumbnailFile.delete()) {
                    logger.warn("failed to remove thumbnail file " + thumbnailPath);
                }
            }

            if (WebFileSys.getInstance().getFfmpegExePath() != null) {
                String videoThumbnailPath = VideoThumbnailCreator.getThumbnailPath(filePath);

                File videoThumbnailFile = new File(videoThumbnailPath);

                if (videoThumbnailFile.exists()) {
                    if (!videoThumbnailFile.delete()) {
                        logger.warn("failed to remove video thumbnail file " + videoThumbnailPath);
                    }
                }
            }
            
            if (fileToDelete.delete()) {
                success = true;
            } else {
                logger.warn("failed to delete file " + filePath);
            }
        }

        Element resultElement = doc.createElement("result");

        XmlUtil.setChildText(resultElement, "success", Boolean.toString(success));
        
        if (success) {
            XmlUtil.setChildText(resultElement, "deletedFile", deletedFile);
        }

        Integer viewMode = (Integer) session.getAttribute("viewMode");
        
        if (viewMode != null) {
            XmlUtil.setChildText(resultElement, "viewMode", Integer.toString(viewMode));
        }
        
        doc.appendChild(resultElement);

        processResponse();
    }

}
