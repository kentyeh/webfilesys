package de.webfilesys.gui.ajax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.gui.user.RemoteEditorRequestHandler;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class XmlSaveRemoteEditorHandler extends XmlRequestHandlerBase {
    private static final Logger logger = LogManager.getLogger(XmlSaveRemoteEditorHandler.class);
	public XmlSaveRemoteEditorHandler(
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

		String fileName = getParameter("filename");

		if (!checkAccess(fileName)) {
			return;
		}

		boolean writeError = false;

        File destFile = new File(fileName);
        
        if (!destFile.canWrite()) {
            logger.warn("failed to save editor content - file is not writable: " + fileName);
        	writeError = true;
        } else {
			String text = getParameter("text");

			String tmpFileName = fileName + "_tmp$edit";

			String fileEncoding = (String) req.getSession(true).getAttribute(RemoteEditorRequestHandler.SESSION_KEY_FILE_ENCODING);

			req.getSession(true).removeAttribute(RemoteEditorRequestHandler.SESSION_KEY_FILE_ENCODING);
			
			try (FileOutputStream fos = new FileOutputStream(tmpFileName);
                                PrintWriter fout = fileEncoding == null?new PrintWriter(fos):
                                        new PrintWriter(new OutputStreamWriter(fos, 
                                                "UTF-8-BOM".equals(fileEncoding) ? "UTF-8" : fileEncoding))) {

                if (fileEncoding == null) {
			        // use OS default encoding
			    } else {
	                logger.debug("saving editor file " + fileName + " with character encoding " + fileEncoding);
			        
			        if (fileEncoding.equals("UTF-8-BOM")) {
			            // write UTF-8 BOM
                        fout.write("\ufeff");
                        fileEncoding = "UTF-8";
			        }
			    }

                if (File.separatorChar == '/') {
                    boolean endsWithLineFeed = text.charAt(text.length() - 1) == '\n';
                    
                    BufferedReader textReader = new BufferedReader(new StringReader(text));
                    
                    String line = null;
                    boolean firstLine = true;
                    
                    while ((line = textReader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                        } else {
                            fout.print('\n');
                        }
                        
                        fout.print(line);    
                    }
                    
                    if (endsWithLineFeed) {
                        fout.print('\n');
                    }
                } else {
                    fout.print(text);
                }

				fout.flush();

				if (!copyFile(tmpFileName, fileName)) {
					String logMsg = "cannot copy temporary file to edited file " + fileName;
					logger.error(logMsg);
					writeError = true;
				} else {
					File tmpFile = new File(tmpFileName);
				
					if (!tmpFile.delete()) {
						logger.warn("cannot delete temporary file " + tmpFile);
					}
				}
			} catch (Exception ex) {
				String logMsg="cannot save changed content of edited file " + fileName + ": " + ex;
				logger.error(logMsg);
				writeError = true;
			}
        }
		
		String resultMsg;

		Element resultElement = doc.createElement("result");

		if (writeError) {
			resultMsg = "failed to save editor content";
			XmlUtil.setChildText(resultElement, "message", resultMsg);
			XmlUtil.setChildText(resultElement, "success", "false");
		} else {
			resultMsg = "editor content saved successfully";
			XmlUtil.setChildText(resultElement, "message", resultMsg);
			XmlUtil.setChildText(resultElement, "success", "true");
			
    		XmlUtil.setChildText(resultElement, "mobile", Boolean.toString(session.getAttribute("mobile") != null));
		}
			
		doc.appendChild(resultElement);
		
		processResponse();
	}
}
