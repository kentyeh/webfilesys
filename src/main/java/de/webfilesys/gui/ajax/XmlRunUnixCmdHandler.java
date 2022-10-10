package de.webfilesys.gui.ajax;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.WebFileSys;
import de.webfilesys.util.XmlUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class XmlRunUnixCmdHandler extends XmlRequestHandlerBase
{
    private static final Logger logger = LogManager.getLogger(XmlRunUnixCmdHandler.class);
	public XmlRunUnixCmdHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}
	
        @Override
	protected void process()
	{
        if ((!isAdminUser(false)) || (!userMgr.getDocumentRoot(uid).equals("/")))
        {
            logger.warn("UNIX command line is only available for admin users with root access");
            return;
        }
		
        String cmdOutput = "";
        
        String unixCmd = req.getParameter("unixCmd");
        
        if (unixCmd != null)
        {
            unixCmd = unixCmd.trim();
            
            if (unixCmd.length() > 0)
            {
                cmdOutput = runUnixCmd(unixCmd);
            }
        }
        
		Element resultElement = doc.createElement("result");
		
		XmlUtil.setChildText(resultElement, "cmdOutput", cmdOutput);

		XmlUtil.setChildText(resultElement, "success", "true");
			
		doc.appendChild(resultElement);
		
		this.processResponse();
	}
	
	private String runUnixCmd(String unixCmd)
	{
	    StringBuilder buff = new StringBuilder();
	    
	    String cmdToExecute = unixCmd;
	    
        if (WebFileSys.getInstance().getOpSysType() == WebFileSys.OS_AIX)
        {
            cmdToExecute = unixCmd + " 2>&1";
        }
 
        logger.debug("executing command : " + cmdToExecute);

        String cmdWithParms[];
        
        if (WebFileSys.getInstance().getOpSysType() == WebFileSys.OS_AIX)
        {
            cmdWithParms = new String[2];
            cmdWithParms[0] = "/bin/sh";
            cmdWithParms[1] = cmdToExecute;
        }
        else
        {
            cmdWithParms = new String[3];
            cmdWithParms[0] = "/bin/sh";
            cmdWithParms[1] = "-c";
            cmdWithParms[2] = cmdToExecute + " 2>&1";
        }

        Runtime rt = Runtime.getRuntime();

        Process cmdProcess = null;

        try
        {
            cmdProcess = rt.exec(cmdWithParms);
            String stdoutLine = null;

            boolean done = false;

            try (BufferedReader cmdOut = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream()))){
                while (!done) {
                    stdoutLine = cmdOut.readLine();

                    if (stdoutLine == null) {
                        done = true;
                    } else {
                        buff.append(stdoutLine);
                        buff.append('\n');
                    }
                }
            } catch (IOException ioe) {
                logger.error("failed to read OS command output", ioe);
            }
        }
        catch (Exception e)
        {
            logger.error("failed to run OS command", e);
        }

        return buff.toString();
	}
}
