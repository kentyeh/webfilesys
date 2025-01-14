package de.webfilesys.gui.user;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


import com.ctc.wstx.exc.WstxParsingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delivers waypoint data extracted from GPX track as JSON.
 * 
 * @author Frank Hoehnel
 */
public class GPXWayPointHandler extends UserRequestHandler {
    private static final Logger logger = LogManager.getLogger(GPXWayPointHandler.class);

    public GPXWayPointHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}	
	
        @Override
	protected void process() {

	    String filePath = getParameter("filePath"); 

		try (BufferedReader gpxReader = new BufferedReader(new FileReader(filePath))){
			resp.setContentType("application/json");

			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(gpxReader);

			String currentElementName = null;

			String tagName = null;

			boolean documentEnd = false;

			boolean dataInvalid = false;
			
			boolean fatalError = false;

			ArrayList<WayPoint> wayPoints = new ArrayList<>();
			
			WayPoint currentWayPoint = null;
			
			while (!documentEnd && !fatalError) {
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
						currentElementName = tagName;

						if (tagName.equals("wpt")) {
							currentWayPoint = new WayPoint();
							String lat = parser.getAttributeValue(null, "lat");
							String lon = parser.getAttributeValue(null, "lon");
							try {
								double latitude = Double.parseDouble(lat);
								double longitude = Double.parseDouble(lon);
                                currentWayPoint.setLat(latitude);
                                currentWayPoint.setLon(longitude);
    							wayPoints.add(currentWayPoint);
							} catch (NumberFormatException numEx) {
								dataInvalid = true;
								logger.debug(numEx, numEx);
							}
						}
						
						break;

					case XMLStreamConstants.END_ELEMENT:

						tagName = parser.getLocalName();
						if (tagName.equals("wpt")) {
						    logger.debug("end waypoint tag with name: " + currentWayPoint.getName());
						    currentWayPoint = null;
						}							
						break;

					case XMLStreamConstants.CHARACTERS:

						if (currentWayPoint != null) {
							String elementText = parser.getText().trim();

							if (currentElementName.equals("ele")) {
								if (elementText.length() > 0) {
								    currentWayPoint.setEle(elementText);
								}
							} else if (currentElementName.equals("name")) {
								if (elementText.length() > 0) {
								    currentWayPoint.setName(elementText);
								}
							} 
						}
						
						break;
					default:
						// logger.debug("unhandled event: " + event);
					}
				} catch (WstxParsingException epex) {
					logger.warn("GPX parsing error", epex);
					fatalError = true;
				}
			}

			output.println("{\"waypoints\": [");
			Iterator<WayPoint> iter = wayPoints.iterator();
			while (iter.hasNext()) {
			    WayPoint wayPoint = iter.next();
				output.println("{");
				output.println("\"lat\": \"" + wayPoint.getLat() + "\",");
				output.println("\"lon\": \"" + wayPoint.getLon() + "\"");
				if (wayPoint.getEle() != null) {
					output.println(", \"ele\": \"" + wayPoint.getEle() + "\"");
				}
				if (wayPoint.getName() != null) {
					output.println(", \"name\": \"" + wayPoint.getName() + "\"");
				}
				output.println("}");
				if (iter.hasNext()) {
					output.println(",");
				}
			}
			output.println("]}");
			
			output.flush();
			
			if (dataInvalid) {
			    logger.warn("GPX file contains invalid data: " + filePath);
			}
		} catch (IOException ioex) {
			logger.error("failed to read GPX file", ioex);
		} catch (XMLStreamException xmlEx) {
			logger.error("error parsing XML stream", xmlEx);
		} catch (Exception e) {
			logger.error("failed to transform GPX file", e);
		}
	}
	
	public class WayPoint {
		private double lat = 0;
		private double lon = 0;
		private String ele = null;
		private String time = null;
		private String name = null;
		
		public void setLat(double newVal) {
			lat = newVal;
		}
		
		public double getLat() {
			return lat;
		}

		public void setLon(double newVal) {
			lon = newVal;
		}
		
		public double getLon() {
			return lon;
		}

		public void setEle(String newVal) {
			ele = newVal;
		}
		
		public String getEle() {
			return ele;
		}

		public void setTime(String newVal) {
			time = newVal;
		}
		
		public String getTime() {
			return time;
		}

		public void setName(String newVal) {
			name = newVal;
		}
		
		public String getName() {
			return name;
		}
	}

}
