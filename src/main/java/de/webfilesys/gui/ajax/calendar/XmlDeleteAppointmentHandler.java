package de.webfilesys.gui.ajax.calendar;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.calendar.Appointment;
import de.webfilesys.calendar.AppointmentManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XmlDeleteAppointmentHandler extends XmlRequestHandlerBase {
    private static final Logger logger = LogManager.getLogger(XmlDeleteAppointmentHandler.class);
	public XmlDeleteAppointmentHandler(
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

		AppointmentManager appMgr = AppointmentManager.getInstance();
		
		Appointment appointment = appMgr.getAppointment(uid, eventId);
		
		if (appointment == null) 
		{
			logger.warn("appointment to be deleted not found with id :" + eventId);
			return;
		}
		
		appMgr.removeAppointment(uid, eventId);
		
		// remove the clone created by delaying
		appMgr.getAlarmIndex().delEvent(uid, appointment);
		
		Element resultElement = doc.createElement("result");
		
		XmlUtil.setChildText(resultElement, "message", "appointment deleted");

		XmlUtil.setChildText(resultElement, "success", "true");
		
		XmlUtil.setChildText(resultElement, "deletedId", eventId);

		doc.appendChild(resultElement);
		
		this.processResponse();
	}
}
