package de.webfilesys.gui.xsl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


import com.ctc.wstx.exc.WstxParsingException;

import de.webfilesys.WebFileSys;
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GPS track file viewer.
 * 
 * @author Frank Hoehnel
 */
public class GPXViewHandler extends UserRequestHandler {
    private static final Logger logger = LogManager.getLogger(GPXViewHandler.class);
	private static String STYLESHEET_REF;

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>";

	public GPXViewHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
            STYLESHEET_REF = "<?xml-stylesheet type=\"text/xsl\" href=\""+ req.getContextPath() +"/xsl/gpxViewer.xsl\"?>";
	}	
	
	protected void process() {
	
	    String filePath = getParameter("filePath"); 
		
		String googleMapsAPIKey = null;
		if (req.getScheme().equalsIgnoreCase("https")) {
			googleMapsAPIKey = WebFileSys.getInstance().getGoogleMapsAPIKeyHTTPS();
		} else {
			googleMapsAPIKey = WebFileSys.getInstance().getGoogleMapsAPIKeyHTTP();
		}

		try (BufferedReader gpxReader = new BufferedReader(new FileReader(filePath))){
			resp.setContentType("text/xml");

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(gpxReader);

			String tagName = null;

			boolean documentEnd = false;

			int trackCounter = 0;

			while (!documentEnd) {
				try {
					int event = parser.next();

					switch (event) {
					case XMLStreamConstants.END_DOCUMENT:
						parser.close();
						documentEnd = true;
						break;

					case XMLStreamConstants.START_DOCUMENT:
						break;

					case XMLStreamConstants.START_ELEMENT:
						tagName = parser.getLocalName();

						if (tagName.equals("gpx")) {
							output.println(XML_HEADER);
							output.println(STYLESHEET_REF);

							output.println("<gpx>");

							if (!CommonUtils.isEmpty(googleMapsAPIKey)) {
								output.println("  <googleMapsAPIKey>" + googleMapsAPIKey + "</googleMapsAPIKey>");
							}
							output.println("  <filePath>" + CommonUtils.escapeForJavascript(filePath) + "</filePath>");
							
							output.println("  <language>" + language + "</language>");
						}

						if (tagName.equals("trk")) {
							output.println("<track>" + trackCounter + "</track>");
							trackCounter++;
						}

						break;

					case XMLStreamConstants.END_ELEMENT:

						tagName = parser.getLocalName();
						if (tagName.equals("gpx")) {
							output.println("</gpx>");
						}
						break;

					default:
						// System.out.println("unhandled event: " + event);
					}
				} catch (WstxParsingException epex) {
					logger.warn("GPX parsing error", epex);
				}
			}

			output.flush();
		} catch (IOException e) {
			logger.error("failed to read GPX file", e);
		} catch (XMLStreamException e) {
			logger.error("error parsing XML stream", e);
		} catch (Exception e) {
			logger.error("failed to transform GPX file", e);
		}
	}
}
