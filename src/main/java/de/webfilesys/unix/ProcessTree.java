package de.webfilesys.unix;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessTree
{
    private static final Logger logger = LogManager.getLogger(ProcessTree.class);
    private HashMap<String, UnixProcess> processList = null;

    private UnixProcess rootProcess=null;

    String forUserid=null;

    private StringBuffer outBuffer;
 
    public ProcessTree(String userid)
    {
        forUserid=userid;
        processList = new HashMap<>();
        readProcessList();
    }

    // must be overwritten by derived classes
    protected void readProcessList()
    {
    }

    protected void addProcessToTree(UnixProcess newProcess)
    {
        String pid=newProcess.getPid();

        UnixProcess existingProcess = processList.get(pid);

        if (existingProcess!=null)
        {
            newProcess.childList=existingProcess.childList;
            processList.put(pid, newProcess);

            if ((rootProcess==null) || (existingProcess.getPid().equals(rootProcess.getPid())))
            {
                rootProcess=newProcess;
            }
        }

        String ppid=newProcess.getPPid();

        UnixProcess parent = processList.get(ppid);

        if (parent==null)
        {
            // System.out.println("adding placeholder parent " + ppid);
            parent=new UnixProcess();
            parent.setPid(ppid);
            processList.put(ppid, parent);
        }

        parent.addChild(newProcess);
        processList.put(pid, newProcess);
        if (rootProcess==null)
        {
            rootProcess=newProcess;
        }
        else
        {
            if (rootProcess.getPid().equals(newProcess.getPid()))
            {
                rootProcess=parent;
            }
        }

    }

    @Override
    public String toString()
    {
        if (rootProcess==null)
        {
            // System.out.println("root process is null");
        }

        outBuffer=new StringBuffer(); 

        treeToString(rootProcess,0);

        return(outBuffer.toString());
    }
 
    private void treeToString(UnixProcess actProcess,int level) 
    {
        for (int i=0;i<level;i++)
        {
             outBuffer.append("  ");
        }
        outBuffer.append(actProcess.getPid());
        outBuffer.append("  ");
        outBuffer.append(actProcess.getCmd());
        outBuffer.append("\n");
        ArrayList<UnixProcess> childList = actProcess.getChildren();

        for (UnixProcess childProcess : childList)
        {
            treeToString(childProcess, level + 1);
        }
    }

    public String toHTML(boolean allowKill)
    {
        if (rootProcess==null)
        {
            logger.error("ProcessTree.toHTML(): rootProcess is null");
            return "";
        }
        StringBuilder outString=new StringBuilder(); 

        outString.append("<table class=\"processList\">\n");
        outString.append("<tr>\n");
        
        if (allowKill)
        {
            outString.append("<th class=\"processList\">op</th>");
        }
        outString.append("<th class=\"processList\">PID</th>\n");
        outString.append("<th class=\"processList\">UID</th>\n");
        outString.append("<th class=\"processList\">CMD</th>\n");
        outString.append("<th class=\"processList\">Start Time</th>\n");
        outString.append("<th class=\"processList\">CPU Time</th>\n");
        outString.append("<th class=\"processList\">TTY</th>\n");
        outString.append("</tr>\n");

        treeToHTML(rootProcess,0,allowKill);

        outString.append("</table>\n");

        return(outString.toString());
    }
 
    private void treeToHTML(UnixProcess actProcess, int level, boolean allowKill) 
    {
        String rowClass = null;
        if (actProcess.getUID().equals("root") || actProcess.getUID().equals("0"))
        {
            rowClass = "processRowRoot";
        }
        else
        {
            rowClass = "processRowOtherUser";
        }

        outBuffer.append("<tr class=\"").append(rowClass).append("\">\n");

        if (allowKill)
        {
            outBuffer.append("<td class=\"processKill\"> <a href=\"javascript:confirmKill('").append(actProcess.getPid()).append("')\"><img align=\"center\" border=\"0\" src=\"images/redx2.gif\" alt=\"kill process\"></a></td>\n");
        }
   
        outBuffer.append("<td class=\"processPID\">").append(actProcess.getPid()).append("</td>");
        outBuffer.append("<td class=\"processUID\">").append(actProcess.getUID()).append("</td>");

        outBuffer.append("<td class=\"processCMD\">");

        for (int i=0;i<level;i++)
        {
             outBuffer.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        outBuffer.append(actProcess.getCmd());
        outBuffer.append("</td>\n");

        String startTime=actProcess.getStartTime();
        if (startTime.length()==0)
        {
            outBuffer.append("<td class=\"processStartTime\">&nbsp;</td>");
        }
        else
        {
            outBuffer.append("<td class=\"processStartTime\">").append(startTime).append("</td>");
        }
        outBuffer.append("<td class=\"processCPUTime\">").append(actProcess.getCPUTime()).append("</td>");
        outBuffer.append("<td class=\"processTTY\">").append(actProcess.getTTY()).append("</td>");

        outBuffer.append("</tr>\n");
        
        ArrayList<UnixProcess> childList = actProcess.getChildren();

        for (UnixProcess childProcess : childList)
        {
            treeToHTML(childProcess,level+1,allowKill);
        }
    }

}