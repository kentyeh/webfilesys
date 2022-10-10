package de.webfilesys;

import java.util.ArrayList;
import java.util.Date;
import de.webfilesys.mail.MailTemplate;
import de.webfilesys.mail.SmtpEmail;
import de.webfilesys.user.UserManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiskQuotaInspector extends Thread
{
    private static final Logger logger = LogManager.getLogger(DiskQuotaInspector.class);

    boolean shutdownFlag=false;

    public DiskQuotaInspector()
    {
    }

    @Override
    public synchronized void run()
    {
        setPriority(1);

        while (!shutdownFlag)
        {
            int hour = LocalDateTime.now(ZoneId.systemDefault()).getHour();

            if (hour == WebFileSys.getInstance().getDiskQuotaCheckHour())
            {
                inspectDiskQuotas();
            }
            
            try
            {
                 this.sleep(60 * 60 * 1000);
            }
            catch (InterruptedException e)
            {
                shutdownFlag=true;
            }
        }
    }

    protected void inspectDiskQuotas()
    {
        long startTime=System.currentTimeMillis();

        logger.info("disk quota inspection for webspace users started");

        UserManager userMgr = WebFileSys.getInstance().getUserMgr();

        StringBuffer adminMailBuffer=new StringBuffer();

        ArrayList<String> allUsers = userMgr.getListOfUsers();

        for (int i=0;i<allUsers.size();i++)
        {
            String userid = allUsers.get(i);

            String role=userMgr.getRole(userid);

            if (role.equals("webspace"))
            {
                String homeDir=userMgr.getDocumentRoot(userid);

                if ((homeDir!=null) && (!homeDir.startsWith("*")))
                {
                    long diskQuota=userMgr.getDiskQuota(userid);

                    if (diskQuota > 0)
                    {
                        FileSysStat fileSysStat = new FileSysStat(homeDir);

                        fileSysStat.getStatistics();

                        if (fileSysStat.getTotalSizeSum() > diskQuota)
                        {
                            logger.warn("disk quota exceeded for user " + userid + " (" + (diskQuota / 1024l) + " / " + (fileSysStat.getTotalSizeSum() / 1024l) + ")");

                            if (WebFileSys.getInstance().getMailHost() !=null)
                            {
                                StringBuilder mailContent=new StringBuilder("Disk quota exceeded for user ");
                                mailContent.append(userid);
                                mailContent.append("\r\n\r\n");
                                mailContent.append("disk quota   : ");
                                mailContent.append(diskQuota / 1024l);
                                mailContent.append(" KByte\r\n");
                                mailContent.append("current usage: ");
                                mailContent.append(fileSysStat.getTotalSizeSum() / 1024l);
                                mailContent.append(" KByte\r\n");

                                if (WebFileSys.getInstance().isMailNotifyQuotaUser())
                                {
                                    String email=userMgr.getEmail(userid);

                                    if ((email!=null) && (email.trim().length()>0))
                                    {
                                        String userLanguage=userMgr.getLanguage(userid);

                                        String mailText=mailContent.toString();

                                        try
                                        {
                                        	String templateFilePath = WebFileSys.getInstance().getConfigBaseDir() + "/languages/diskquota_" + userLanguage + ".template";
                                        	
                                            MailTemplate diskQuotaTemplate=new MailTemplate(templateFilePath);

                                            diskQuotaTemplate.setVarValue("LOGIN",userid);
                                            diskQuotaTemplate.setVarValue("QUOTA","" + (diskQuota / 1024l));
                                            diskQuotaTemplate.setVarValue("USAGE","" + (fileSysStat.getTotalSizeSum() / 1024l));

                                            mailText=diskQuotaTemplate.getText();
                                        }
                                        catch (IllegalArgumentException iaex)
                                        {
                                            logger.error(iaex);
                                        }

                                        String subject = LanguageManager.getInstance().getResource(userLanguage,"subject.diskquota","Disk quota exceeded");

                                        (new SmtpEmail(email,subject,mailText)).send();
                                    }
                                }

                                adminMailBuffer.append(mailContent.toString());
                                adminMailBuffer.append("\r\n\r\n");
                            }
                        }
                    }
                }
            }
        }
        
        long endTime=System.currentTimeMillis();
        
        if ((WebFileSys.getInstance().getMailHost() != null) && WebFileSys.getInstance().isMailNotifyQuotaAdmin())
        {
            if (adminMailBuffer.length()==0)
            {
                adminMailBuffer.append("no disk quota exceeded");
            }

            ArrayList<String> adminUserEmailList = userMgr.getAdminUserEmails();
            
            (new SmtpEmail(adminUserEmailList, "Disk quota report " + WebFileSys.getInstance().getLogDateFormat().format(new Date(endTime)),
                       adminMailBuffer.toString())).send();
        }

        logger.info("disk quota inspection ended (" + ((endTime - startTime) / 1000) + " sec)");
    }

}
