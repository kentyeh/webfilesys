package de.webfilesys.gui.user;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.util.UTF8URLDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiFileRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(MultiFileRequestHandler.class);
	protected String actPath = null;
	
	protected ArrayList<String> selectedFiles = null;

	protected String cmd = null;
	
	public MultiFileRequestHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
        
		selectedFiles = new ArrayList<>();

		Enumeration allKeys = req.getParameterNames();
		
		while (allKeys.hasMoreElements())
		{
			String parm_key=(String) allKeys.nextElement();

			String parm_value = req.getParameter(parm_key);
			
			if (parm_key.equals("cmd"))
			{
				cmd=parm_value;
			}
			else if (parm_key.equals("actpath"))
			{
				actPath = parm_value;
			}
			else if ((!parm_key.equals("cb-setAll")) && (!parm_key.equals("command")))
			{
				try
				{
					String fileName = UTF8URLDecoder.decode(parm_key);
					selectedFiles.add(fileName); 
				}
				catch (Exception ue1)
				{
					logger.error(ue1);
				}
			}
		}

		session.setAttribute("selectedFiles", selectedFiles);
		
		if (actPath == null) 
		{
		    actPath = getCwd();
		}
		else
		{
	        if (isMobile()) {
	            actPath = getAbsolutePath(actPath);
	        }
		}
	}

        @Override
	public void handleRequest()
	{
		if (!accessAllowed(actPath))
		{
			logger.warn("user " + uid + " tried to access folder outside of it's document root: " + actPath);
			return;
		}
		
		if (selectedFiles.isEmpty())
		{
			output.print("<HTML>");
			output.print("<HEAD>");

			javascriptAlert(getResource("alert.noFilesSelected","No files have been selected"));

			output.println("<script language=\"javascript\">");
			output.println("window.location.href='/webfilesys/servlet?command=listFiles';");
			output.println("</script>");
			output.println("</HEAD></HTML>");
			output.flush();
			return;
		}

		process();
	}
}
