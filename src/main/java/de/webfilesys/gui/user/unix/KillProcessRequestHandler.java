package de.webfilesys.gui.user.unix;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.WebFileSys;
import de.webfilesys.gui.user.UserRequestHandler;
import de.webfilesys.user.UserManager;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class KillProcessRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(KillProcessRequestHandler.class);
	public KillProcessRequestHandler(
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

		boolean allowProcessKill = WebFileSys.getInstance().isAllowProcessKill();
		
		UserManager userMgr = WebFileSys.getInstance().getUserMgr();

		if ((!allowProcessKill) ||
			(!userMgr.getDocumentRoot(uid).equals("/")))
		{
			return;
		}

		String pid=getParameter("pid");

		String prog_name_parms[];
		
		if (WebFileSys.getInstance().getOpSysType() == WebFileSys.OS_AIX)
		{
			prog_name_parms=new String[2];
			prog_name_parms[0]="/bin/sh";
			prog_name_parms[1]="kill -9 " + pid;

		}
		else
		{
			prog_name_parms=new String[3];
			prog_name_parms[0]="/bin/sh";
			prog_name_parms[1]="-c";
			prog_name_parms[2]="kill -9 " + pid;
		}

		Runtime rt=Runtime.getRuntime();

		try
		{
			rt.exec(prog_name_parms);
		}
		catch (IOException e)
		{
			logger.warn(e);
			return;
		}

		logger.debug("killing process " + pid);

		output.print("<HTML>");
		output.print("<HEAD>");
		output.print("<META HTTP-EQUIV=\"REFRESH\" CONTENT=\"3; URL="+ req.getContextPath() + "/servlet?command=processList\">");
		output.print("</HEAD>"); 
		output.print("<body> trying to kill Process " + pid + " ... </body>"); 
		output.print("</HTML>");

		output.flush(); 
	}
}
