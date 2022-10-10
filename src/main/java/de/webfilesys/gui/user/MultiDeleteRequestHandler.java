package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.ThumbnailThread;
import de.webfilesys.gui.xsl.XslFileListHandler;
import de.webfilesys.gui.xsl.mobile.MobileFolderFileListHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiDeleteRequestHandler extends MultiFileRequestHandler {
    private static final Logger logger = LogManager.getLogger(MultiDeleteRequestHandler.class);
	public MultiDeleteRequestHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			PrintWriter output, String uid) {
		super(req, resp, session, output, uid);
	}

        @Override
	protected void process() {
		if (!checkWriteAccess()) {
			return;
		}

		StringBuilder errorMsg = new StringBuilder();

		MetaInfManager metaInfMgr = MetaInfManager.getInstance();

		for (String selectedFile : selectedFiles) {
			String filePath = null;

			if (actPath.endsWith(File.separator)) {
				filePath = actPath + selectedFile;
			} else {
				filePath = actPath + File.separator + selectedFile;
			}

			File delFile = new File(filePath);

			if ((!delFile.canWrite()) || (!delFile.delete())) {
				if (errorMsg.length() > 0) {
					errorMsg.append("<br/>");
				}
				errorMsg.append(getResource("alert.delete.failed", "cannot delete file ")).append("<br/>").append(selectedFile);
			} else {
				metaInfMgr.removeMetaInf(actPath, selectedFile);

				String thumbnailPath = ThumbnailThread.getThumbnailPath(filePath);

				File thumbnailFile = new File(thumbnailPath);

				if (thumbnailFile.exists()) {
					if (!thumbnailFile.delete()) {
						logger.debug("cannot remove thumbnail file " + thumbnailPath);
					}
				}
			}
		}

		if (errorMsg.length() > 0) {
			setParameter("errorMsg", errorMsg.toString());
		}
		
		String mobile = (String) session.getAttribute("mobile");

		if (mobile != null) {
			(new MobileFolderFileListHandler(req, resp, session, output, uid)).handleRequest();
		} else {
		    (new XslFileListHandler(req, resp, session, output, uid)).handleRequest();
		}
	}

}
