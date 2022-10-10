package de.webfilesys.unix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SolarisProcessTree extends ProcessTree
{
    private static final Logger logger = LogManager.getLogger(SolarisProcessTree.class);
    public SolarisProcessTree(String userid)
    {
        super(userid);
    }

    @Override
    protected void readProcessList()
    {
        String prog_name_parms[];
     
        prog_name_parms=new String[3];

        prog_name_parms[0]="/bin/sh";
        prog_name_parms[1]="-c";

        // prog_name_parms[2]=new String("ps auxlww --sort=uid,pid" + " 2>&1");
        prog_name_parms[2]="ps -eo user,pid,ppid,vsz,tty,time,stime,args" + " 2>&1";

        Runtime rt=Runtime.getRuntime();

        Process dsmc_process=null;

        try
        {
            dsmc_process=rt.exec(prog_name_parms);
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }

        try(BufferedReader  dsmc_out=new BufferedReader(new InputStreamReader(dsmc_process.getInputStream()))){
        String stdout_line=null;

        StringTokenizer proc_parser=null;

        String pid_string=null;
        String size_string;
        boolean done=false;
        boolean first_line=true;
        String userid=null;
        String cmd_str;
        String cpu_time;
        String tty;
        String start_time;
        String ppid;
        int cmdStartIndex=0;
        int ttyStartIndex=0;
        int timeStartIndex=0;

        while (!done)
        {
            stdout_line = dsmc_out.readLine();
              
            if (stdout_line==null)
            {
                done=true;
            }
            else
            {
                // System.out.println(stdout_line);
                             
                proc_parser=new StringTokenizer(stdout_line);
             
                if (first_line)
                {
                    first_line=false;
                }
                else
                {   proc_parser=new StringTokenizer(stdout_line);
                 
                    userid=proc_parser.nextToken();
                    pid_string=proc_parser.nextToken();
                    ppid=proc_parser.nextToken(); 
                    size_string=proc_parser.nextToken();
                    tty=proc_parser.nextToken();
                    cpu_time=proc_parser.nextToken();
                    start_time=proc_parser.nextToken();
                    cmd_str=proc_parser.nextToken();
                    while (proc_parser.hasMoreTokens())
                    {
                        cmd_str=cmd_str + " " + proc_parser.nextToken(); 
                    }
                     
                    if (forUserid.equals("all") || forUserid.equals(userid))
                    {
                        UnixProcess newProcess=new UnixProcess();
                        newProcess.setUID(userid);
                        newProcess.setPid(pid_string);
                        newProcess.setPPid(ppid);
                        newProcess.setSize(size_string);
                        newProcess.setTTY(tty);
                        newProcess.setCPUTime(cpu_time);
                        newProcess.setStartTime(start_time); 
                        newProcess.setCmd(cmd_str); 
                     
                        addProcessToTree(newProcess);
                    }
                }
            }
        }}
        catch (IOException ioe)
        {
                logger.error(ioe.getMessage(),ioe);
        }
    }

}