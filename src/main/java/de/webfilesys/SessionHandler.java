package de.webfilesys;

import java.io.File;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import de.webfilesys.calendar.AppointmentManager;
import de.webfilesys.decoration.DecorationManager;
import de.webfilesys.graphics.AutoThumbnailCreator;
import de.webfilesys.user.UserManager;
import de.webfilesys.user.UserManagerBase;
import de.webfilesys.watch.FolderWatchManager;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionHandler
implements HttpSessionListener, ServletContextListener
{
    private static final Logger logger = LogManager.getLogger(SessionHandler.class);
	private static int activeSessions = 0;

	private static ConcurrentHashMap<String,HttpSession> sessionList = new ConcurrentHashMap<>();

	/**
	 * @see javax.servlet.http.HttpSessionListener#sessionCreated(HttpSessionEvent)
	 */
        @Override
	public void sessionCreated(HttpSessionEvent sessionEvent)
	{
		HttpSession session = sessionEvent.getSession();

		String sessionId = session.getId();

		sessionList.put(sessionId, session);

		activeSessions++;
		
        if (activeSessions >= 0)  // this value can be negative because of sessions that survived tomcat restart
        {
  		    // logger.debug("active sessions: " + activeSessions);
        }
	}

	/**
	 * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(HttpSessionEvent)
	 */
        @Override
	public void sessionDestroyed(HttpSessionEvent sessionEvent)
	{
		HttpSession session = sessionEvent.getSession();

		String sessionId = null;

		try
		{
			sessionId = session.getId();

			sessionList.remove(sessionId);

			String userid = (String) session.getAttribute("userid");

			if (userid == null)
			{
		        logger.info("session expired/destroyed with id " + sessionId);
			}
			else
			{
		        logger.info("session expired/destroyed for user: " + userid + " sessionId: " + sessionId);
			}
		}
		catch (IllegalStateException iex)
		{
			logger.info("session expired/destroyed with id " + sessionId);

			// In tomcat version 4 the session has already been invalidated when sessionDestroyed()
			// is called. So we get an IllegalStateException when we try to read the userid attribute.
			// In tomcat version 5 sessionDestroyed() is called before the session is being invalidated.
			
			logger.debug(iex);
		}

		activeSessions--;

		logger.debug("active sessions: " + activeSessions);
	}

	public static Enumeration<HttpSession> getSessions()
	{
		return (sessionList.elements());
	}
	
        @Override
    public void contextInitialized (ServletContextEvent servletContextEvent)
    {
    	// ServletContext servletContext = servletContextEvent.getServletContext ();
    }

        @Override
    public void contextDestroyed (ServletContextEvent servletContextEvent)
    {
        // ServletContext servletContext = servletContextEvent.getServletContext ();

    	logger.info("saving and cleaning up on context shutdown");
    	
    	UserManager userMgr = WebFileSys.getInstance().getUserMgr();    	
    	
    	((UserManagerBase) userMgr).interrupt();

        MetaInfManager.getInstance().interrupt();
        
        CategoryManager.getInstance().interrupt();

        DecorationManager.getInstance().interrupt();

        InvitationManager.getInstance().interrupt();

        FileSysBookmarkManager.getInstance().interrupt();
  
		if (SubdirExistTester.instanceCreated())
        {
            SubdirExistTester.getInstance().interrupt();
        }
        
        if (WebFileSys.getInstance().getDiskQuotaInspector() != null)
		{
			WebFileSys.getInstance().getDiskQuotaInspector().interrupt();
		}

		if (WebFileSys.getInstance().isEnableCalendar())
		{
			AppointmentManager.getInstance().interrupt();
		}

		if (AutoThumbnailCreator.instanceCreated())
        {
			AutoThumbnailCreator.getInstance().interrupt();
        }
        
        if (File.separatorChar == '\\')
        {
            WinDriveManager.getInstance().interrupt();
        }
        
        FastPathManager.getInstance().interrupt();
        
        if (WebFileSys.getInstance().isFolderWatch()) {
            FolderWatchManager.getInstance().interrupt();
        }

        do
		{
			try
			{
				Thread.currentThread().sleep(3000);
			}
			catch (InterruptedException iex)
			{
				logger.error(iex);
			}
		}
		while (!userMgr.isReadyForShutdown());

        logger.info("WebFileSys ready for shutdown");
    }
	
}
