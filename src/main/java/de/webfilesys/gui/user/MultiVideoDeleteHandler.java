package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.MetaInfManager;
import de.webfilesys.graphics.VideoThumbnailCreator;
import de.webfilesys.gui.xsl.XslVideoListHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiVideoDeleteHandler extends MultiImageRequestHandler {
    private static final Logger logger = LogManager.getLogger(MultiVideoDeleteHandler.class);
	boolean clientIsLocal = false;

	public MultiVideoDeleteHandler(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			PrintWriter output, String uid, boolean clientIsLocal) {
		super(req, resp, session, output, uid);

		this.clientIsLocal = clientIsLocal;
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

				String thumbnailPath = VideoThumbnailCreator.getThumbnailPath(filePath);

				File thumbnailFile = new File(thumbnailPath);

				if (thumbnailFile.exists()) {
					if (!thumbnailFile.delete()) {
						logger.debug("failed to remove video thumbnail file " + thumbnailPath);
					}
				}
			}
		}

		if (errorMsg.length() > 0) {
			setParameter("errorMsg", errorMsg.toString());
		}

		(new XslVideoListHandler(req, resp, session, output, uid, clientIsLocal)).handleRequest();
	}

}
