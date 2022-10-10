package de.webfilesys.gui.ajax.calendar;

import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.calendar.AlarmEntry;
import de.webfilesys.calendar.Appointment;
import de.webfilesys.calendar.AppointmentManager;
import de.webfilesys.gui.ajax.XmlRequestHandlerBase;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XmlDelayAppointmentHandler extends XmlRequestHandlerBase {
    private static final Logger logger = LogManager.getLogger(XmlDelayAppointmentHandler.class);
	public XmlDelayAppointmentHandler(
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

		String delayMinutesParm = getParameter("delayMinutes");
		if (CommonUtils.isEmpty(delayMinutesParm)) {
			logger.warn("missing parameter delayMinutes");
			return;
		}

		int delayMinutes = 0;
		try
		{
			delayMinutes = Integer.parseInt(delayMinutesParm);
		}
		catch (NumberFormatException numEx)
		{
			logger.warn("invalid parameter delayMinutes: " + delayMinutesParm);
			return;
		}
		
		if (delayMinutes > 0)
		{
			Appointment appointment = AppointmentManager.getInstance().getAppointment(uid, eventId);
			
			long newAlarmTime = System.currentTimeMillis() + (delayMinutes * 60000);
			
			appointment.setAlarmTime(new Date(newAlarmTime));
			appointment.setAlarmed(false);
			appointment.setMailAlarmed(true);
			
			AlarmEntry clone = AppointmentManager.getInstance().getAlarmIndex().addEvent(uid, appointment);
			
			clone.setCloned(true);

			if (logger.isDebugEnabled())
			{
				logger.debug("created AlarmEntry clone for delayed alarm: " + clone);
			}
		}
		
		Element resultElement = doc.createElement("result");
		
		XmlUtil.setChildText(resultElement, "message", "appointment delayed");

		XmlUtil.setChildText(resultElement, "success", "true");
		
		XmlUtil.setChildText(resultElement, "delayedId", eventId);

		doc.appendChild(resultElement);
		
		this.processResponse();
	}
}
