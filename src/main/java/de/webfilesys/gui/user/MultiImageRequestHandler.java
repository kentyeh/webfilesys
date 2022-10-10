package de.webfilesys.gui.user;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class MultiImageRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(MultiImageRequestHandler.class);
	private static final int prefixLength = Constants.CHECKBOX_LIST_PREFIX.length();
	
	protected String actPath = null;
	
	protected ArrayList<String> selectedFiles = null;

    protected String cmd = null;
	
	public MultiImageRequestHandler(
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
			String parmKey=(String) allKeys.nextElement();

            if (parmKey.startsWith(Constants.CHECKBOX_LIST_PREFIX))
            {
				String fileName = parmKey.substring(prefixLength);
				
				selectedFiles.add(fileName); 
            }
			else if (parmKey.equals("actpath"))
			{
				actPath = req.getParameter(parmKey);
			}
            else if (parmKey.equals("cmd"))
            {
                cmd = req.getParameter(parmKey);
            }
		}
		
		session.setAttribute("selectedFiles", selectedFiles);
	}

        @Override
	public void handleRequest()
	{
		if (actPath == null)
		{
			actPath = getCwd();
			
			if (actPath == null)
			{
				logger.warn("current path cannot be determined");
				return;
			}
		}
		
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
