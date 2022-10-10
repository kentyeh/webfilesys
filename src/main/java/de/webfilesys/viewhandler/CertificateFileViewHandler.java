package de.webfilesys.viewhandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import de.webfilesys.ViewHandlerConfig;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Show the content of certificate files.
 * 
 * @author Frank Hoehnel
 */
public class CertificateFileViewHandler implements ViewHandler {
	
	private static final Logger logger = LogManager.getLogger(CertificateFileViewHandler.class);
	
        @Override
	public void process(String filePath, ViewHandlerConfig viewHandlerConfig, HttpServletRequest req,
			HttpServletResponse resp) {

		try {
			PrintWriter output = resp.getWriter();
			
			output.println("<html>");
			output.println("<head>");
			output.println("<title>WebFileSys certificate file viewer</title>");
			output.println("<style>td {padding:2px 6px}</style>");
			output.println("</head>");
			output.println("<body>");

			output.println("<div style=\"font-family:Arial,Helvetica;font-size:16px;color:navy;margin-bottom:16px\">Contents of certificate file " + CommonUtils.extractFileName(filePath) + "</div>");
			
			try (FileInputStream fis = new FileInputStream(filePath)){
				CertificateFactory fact = CertificateFactory.getInstance("X.509");
			    X509Certificate cert = (X509Certificate) fact.generateCertificate(fis);
			    
	        	output.println("<table style=\"border:1px solid #a0a0a0;font-family:Arial,Helvetica;font-size:16px;border-collapse:collapse\">");
			    
	        	output.println("<tr style=\"background-color:ivory\"><td style=\"white-space:nowrap\">certificate type:</td><td>" + cert.getType() + "</td></tr>");
			    
        		boolean valid = true;
	        	
	        	try {
	        	    cert.checkValidity();
	        	} catch (CertificateExpiredException | CertificateNotYetValidException expEx) {
	        		valid = false;
	        	}

	        	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        	
                String validFrom = dateFormat.format(cert.getNotBefore());
                String validUntil = dateFormat.format(cert.getNotAfter());
	        	
	        	output.println("<tr style=\"background-color:ivory\"><td>valid:</td><td>" + Boolean.toString(valid) + " (from " + validFrom + " until " + validUntil + ")</td></tr>");
        		
	        	X500Principal issuer = cert.getIssuerX500Principal();
	        	
	        	if (issuer != null) {
		        	output.println("<tr style=\"background-color:ivory\"><td style=\"vertical-align:top\">issuer:</td><td>" + issuer.getName() + "</td></tr>");
	        	}
	        	
	        	X500Principal subject = cert.getSubjectX500Principal();

	        	if (subject != null) {
		        	output.println("<tr style=\"background-color:ivory\"><td style=\"vertical-align:top\">subject:</td><td>" + subject.getName() + "</td></tr>");
	        	}

	        	output.println("</table>");
	        	
		    } catch (CertificateException certEx) {
		    	logger.warn("failed to load certificate " + filePath, certEx);
		    	output.println("failed to load certificate: " + certEx);
		    } catch (IOException ioEx) {
		    	logger.warn("failed to load certificate " + filePath, ioEx);
		    	output.println("failed to load certificate: " + ioEx);
			}

		    output.println("</body>");
			output.println("</html>");
			output.flush();
		} catch (IOException ex) {
	    	logger.warn("failed to get certificate content of file " + filePath, ex);
		}
	}

	/**
	 * Does this ViewHandler support reading the file from an input stream of a
	 * ZIP archive?
	 * 
	 * @return true if reading from ZIP archive is supported, otherwise false
	 */
        @Override
	public boolean supportsZipContent() {
		return false;
	}

        @Override
	public void processZipContent(String fileName, InputStream zipIn, ViewHandlerConfig viewHandlerConfig,
			HttpServletRequest req, HttpServletResponse resp) {
		// ZIP not supported
	}

}
