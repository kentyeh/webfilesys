package de.webfilesys.gui.ajax.calendar;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XmlMoveAppointmentHandler extends XmlRequestHandlerBase {
    private static final Logger logger = LogManager.getLogger(XmlMoveAppointmentHandler.class);
	
	public static final String SESSION_KEY_APPOINTMENT_TO_MOVE = "appointmentToMove";
	
	public XmlMoveAppointmentHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}
	
	protected void process()
	{
		if (!checkWriteAccess())
		{
			return;
		}

		String eventId = getParameter("eventId");
		if (CommonUtils.isEmpty(eventId)) {
			logger.warn("missing parameter eventId");
			return;
		}

		req.getSession(true).setAttribute(SESSION_KEY_APPOINTMENT_TO_MOVE, eventId);
		
		Element resultElement = doc.createElement("result");
		
		XmlUtil.setChildText(resultElement, "message", "appointment created");

		XmlUtil.setChildText(resultElement, "success", "true");
		
		doc.appendChild(resultElement);
		
		processResponse();
	}
}
