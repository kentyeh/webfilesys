package de.webfilesys.viewhandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.webfilesys.ViewHandlerConfig;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Show the content of Java keystore files.
 * 
 * @author Frank Hoehnel
 */
public class KeyStoreViewHandler implements ViewHandler {
	
	private static final Logger logger = LogManager.getLogger(KeyStoreViewHandler.class);
	
        @Override
	public void process(String filePath, ViewHandlerConfig viewHandlerConfig, HttpServletRequest req,
			HttpServletResponse resp) {

		String keyStorePassword = req.getParameter("passwd");
		
		if (CommonUtils.isEmpty(keyStorePassword)) {
			sendPasswordPrompt(req, resp, filePath);
			return;
		}
		
		try {
			PrintWriter output = resp.getWriter();
			
			output.println("<html>");
			output.println("<head>");
			output.println("<title>WebFileSys KeyStore viewer</title>");
			output.println("<style>td {padding:2px 6px}</style>");
			output.println("</head>");
			output.println("<body>");

			output.println("<div style=\"font-family:Arial,Helvetica;font-size:16px;color:navy;margin-bottom:16px\">Contents of keystore " + CommonUtils.extractFileName(filePath) + "</div>");
			
		    try (FileInputStream fis = new java.io.FileInputStream(filePath)){
			    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

		        keyStore.load(fis, keyStorePassword.toCharArray());
		        
		        Enumeration<String> aliases = keyStore.aliases();
		        
		        boolean empty = true;
		        
		        if (aliases.hasMoreElements()) {
		        	output.println("<table style=\"border:1px solid #a0a0a0;font-family:Arial,Helvetica;font-size:16px;border-collapse:collapse\">");
		        	empty = false;
		        }

                ArrayList<String> sortList = new ArrayList<>();
                
		        while (aliases.hasMoreElements()) {
		            sortList.add(aliases.nextElement());
		        }
		        
		        if (sortList.size() > 1) {
		        	Collections.sort(sortList);
		        }
		        
		        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		        
		        for (String alias : sortList) {
		        	output.println("<tr style=\"background-color:lavender\"><td>alias:</td><td>" + alias + "</td></tr>");
		        	
		        	KeyStore.Entry entry = null;
		        	
                    try {
    		        	entry = keyStore.getEntry(alias, null);
                    } catch (UnrecoverableKeyException ukEx) {
    		        	try {
    		        		entry = keyStore.getEntry(alias,  new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
    		        	} catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
    		        		logger.warn("failed to determine type of keystore entry", ex);
    		        	}
                    }
		        	
		        	String entryType = "unknown";
		        	if (entry != null) {
			        	if (entry instanceof KeyStore.PrivateKeyEntry) {
			        		entryType = "private key";
			        	} else if (entry instanceof KeyStore.SecretKeyEntry) {
			        		entryType = "secret key";
			        	} else if (entry instanceof KeyStore.TrustedCertificateEntry) {
			        		entryType = "trusted certificate";
			        	}
		        	}
		        	output.println("<tr style=\"background-color:ivory\"><td style=\"white-space:nowrap\">entry type:</td><td>" + entryType + "</td></tr>");
		        	
		        	Certificate cert = keyStore.getCertificate(alias);
		        	
		        	output.println("<tr style=\"background-color:ivory\"><td style=\"white-space:nowrap\">certificate type:</td><td>" + cert.getType() + "</td></tr>");

		        	if (cert instanceof X509Certificate) {
		        		X509Certificate x509Cert = (X509Certificate) cert;

		        		boolean valid = true;
			        	
			        	try {
			        	    x509Cert.checkValidity();
			        	} catch (CertificateExpiredException | CertificateNotYetValidException expEx) {
			        		valid = false;
			        	}
			        	
                        String validFrom = dateFormat.format(x509Cert.getNotBefore());
                        String validUntil = dateFormat.format(x509Cert.getNotAfter());
			        	
			        	output.println("<tr style=\"background-color:ivory\"><td>valid:</td><td>" + Boolean.toString(valid) + " (from " + validFrom + " until " + validUntil + ")</td></tr>");
		        		
			        	X500Principal issuer = x509Cert.getIssuerX500Principal();
			        	
			        	if (issuer != null) {
				        	output.println("<tr style=\"background-color:ivory\"><td style=\"vertical-align:top\">issuer:</td><td>" + issuer.getName() + "</td></tr>");
			        	}
			        	
			        	X500Principal subject = x509Cert.getSubjectX500Principal();

			        	if (subject != null) {
				        	output.println("<tr style=\"background-color:ivory\"><td style=\"vertical-align:top\">subject:</td><td>" + subject.getName() + "</td></tr>");
			        	}
		        	}
		        }
		        if (!empty) {
		        	output.println("</table>");
		        }
		    } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableEntryException keyEx) {
		    	logger.warn("failed to load keystore " + filePath, keyEx);
		    	output.println("failed to load keystore: " + keyEx);
		    }

		    output.println("</body>");
			output.println("</html>");
			output.flush();
		} catch (IOException ex) {
	    	logger.warn("failed to list keystore content" + filePath, ex);
		}
	}

	private void sendPasswordPrompt(HttpServletRequest req, HttpServletResponse resp, String keyStoreFilePath) {
		
		try {
			PrintWriter output = resp.getWriter();

			output.println("<html>");
			output.println("<head>");
			output.println("<title>WebFileSys KeyStore viewer</title>");
			output.println("</head>");
			output.println("<body style=\"font-family:Arial,Helvetica;font-size:16px\">");
			output.println("<form method=\"get\" action=\"" + req.getRequestURI() + "\">");
			output.println("<label>password for keystore " + CommonUtils.extractFileName(keyStoreFilePath) + ":</label>");
			output.println("<input type=\"password\" name=\"passwd\" style=\"width:120px;\">");
			output.println("<input type=\"submit\" style=\"width:60px;\" value=\"OK\">");
			output.println("</form>");
			output.println("</body>");
			output.println("</html>");
			output.flush();
		} catch (IOException e) {
			logger.error("failed to send keystore password prompt",e);
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
