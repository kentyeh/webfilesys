package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;

import de.webfilesys.SubdirExistTester;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.AutoThumbnailCreator;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.UTF8URLEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Extract files from a TAR archive.
 * @author Frank Hoehnel
 */
public class UntarRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(UntarRequestHandler.class);
	public UntarRequestHandler(
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
		if (!checkWriteAccess())
		{
			return;
		}

		String filePath = getParameter("filePath");

		if (!checkAccess(filePath))
		{
			return;
		}

		output.println("<html>");
		output.println("<head>");

		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/common.css\">");
		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/skins/" + userMgr.getCSS(uid) + ".css\">");

		output.println("</head>");
		output.println("<body>");

        headLine(getResource("label.untarhead", "Extract from TAR archive"));

        output.println("<br/>");

        output.println("<form accept-charset=\"utf-8\" name=\"form1\">");

        output.println("<table class=\"dataForm\" width=\"100%\">");
        
        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm1\">");
        output.println(getResource("label.extractfrom","extracting from") + ":");
        output.println("</td>");
        output.println("</tr>");
        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm2\">");
        output.println(this.getHeadlinePath(filePath));
        output.println("</td>");
        output.println("</tr>");
        
        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm1\">");
        output.println(getResource("label.currentzip","current file") + ":");
        output.println("</td>");
        output.println("</tr>");
        
        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm2\">");
        output.println("<div id=\"currentFile\" />");
        output.println("</td>");
        output.println("</tr>");

        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm1\">");
        output.println(getResource("label.untarResult","files extracted") + ":");
        output.println("</td>");
        output.println("</tr>");
        
        output.println("<tr>");
        output.println("<td colspan=\"2\" class=\"formParm2\">");
        output.println("<div id=\"extractCount\" />");
        output.println("</td>");
        output.println("</tr>");
        
        boolean untarOkay = true;
        
        int untarNum = 0;

        boolean dirCreated = false;
        
        try (TarInputStream tarFile = new TarInputStream(new FileInputStream(filePath))){
            TarEntry tarEntry = null;

            byte buff[] = new byte[4096];

            while ((tarEntry = tarFile.getNextEntry()) != null) {

                output.println("<script language=\"javascript\">");
                output.println("document.getElementById('currentFile').innerHTML='" + CommonUtils.shortName(tarEntry.getName(),45) + " (" + tarEntry.getSize() + " bytes)';");
                output.println("document.getElementById('extractCount').innerHTML='" + untarNum +  "';");
                output.println("</script>");
                output.flush();

                File untarOutFile = createUntarFile(tarEntry.getName());

                if (!(untarOutFile.isDirectory())) {
                    try (FileOutputStream destination = new FileOutputStream(untarOutFile)){
                        boolean done = false;
                        while (!done) {
                            int bytesRead = tarFile.read(buff);

                            if (bytesRead == -1) {
                                done = true;
                            } else {
                                destination.write(buff, 0, bytesRead);
                            }
                        }

                    } catch (IOException ioex) {
                        logger.error("untar error in file " + untarOutFile, ioex);

                        output.println("<font class=\"error\">");
                        output.println(getResource("label.untarError", "Failed to extract from TAR archive") + ": " + untarOutFile);
                        output.println("</font><br><br>");
                        untarOkay = false; 
                        
                        try {
                            untarOutFile.delete(); 
                        } catch (Exception ex) {
                        }
                    }
                    
                    untarNum++;
                } else {
                    dirCreated = true;
                }
            }
            
        } catch (IOException ioex) {
            logger.error("failed to extract from tar archive", ioex);

            output.println("<font class=\"error\">");
            output.println(getResource("label.untarError", "Failed to extract from TAR archive"));
            output.println("</font><br><br>");
            untarOkay = false; 
        }

        if (dirCreated)
        {
	        SubdirExistTester.getInstance().queuePath(getCwd(), 1, true);	        
        }

        String filenameWithoutPath = filePath.substring(filePath.lastIndexOf(File.separator)+1);

        output.println("<script language=\"javascript\">");
        output.println("document.getElementById('extractCount').innerHTML='" + untarNum +  "';");
        output.println("</script>");
        output.flush();
        
        String returnUrl = null;
        
        if (!untarOkay)
        {
            output.println("<tr>");
            output.println("<td colspan=\"2\" class=\"formButton\">");
            returnUrl= req.getContextPath() + "/servlet?command=listFiles";
            output.println("<input type=\"button\" value=\"" + getResource("button.return","Return") + "\" onclick=\"window.location.href='" + returnUrl + "'\">");
            output.println("</td>");
            output.println("</tr>");
        }
        else
        {
            output.println("<tr>");
            output.println("<td class=\"formButton\">");
            
            String mobile = (String) session.getAttribute("mobile");
            
            if (mobile != null)
            {
                returnUrl = ""+ req.getContextPath() + "/servlet?command=mobile&cmd=folderFileList&keepListStatus=true";
            }
            else
            {
                returnUrl = ""+ req.getContextPath() + "/servlet?command=listFiles&keepListStatus=true";
            }
            output.print("<input type=\"button\" value=\"" + getResource("button.keepTarArchive","keep TAR archive") + "\" onclick=\"");
            
            if ((mobile == null) && (dirCreated))
            {
                output.print("window.parent.frames[1].location.href='"+ req.getContextPath() + "/servlet?command=refresh&path=" + UTF8URLEncoder.encode(getCwd()) + "';");
            }
            output.println("window.location.href='" + returnUrl + "'\">");

            output.println("</td>");
            
            output.println("<td class=\"formButton\" style=\"text-align:right\">");

            returnUrl = ""+ req.getContextPath() + "/servlet?command=fmdelete&fileName=" + UTF8URLEncoder.encode(filenameWithoutPath) + "&deleteRO=no";
            output.print("<input type=\"button\" value=\"" + getResource("button.delTarArchive","delete TAR archive") + "\" onclick=\"");
            
            if ((mobile == null) && (dirCreated))
            {
                output.print("window.parent.frames[1].location.href='"+ req.getContextPath() + "/servlet?command=refresh&path=" + UTF8URLEncoder.encode(getCwd()) + "';");
            }
            
            output.println("window.location.href='" + returnUrl + "'\">");
            
            output.println("</td>");
            output.println("</tr>");

            if (WebFileSys.getInstance().isAutoCreateThumbs())
            {
                if (dirCreated)
                {
                    AutoThumbnailCreator.getInstance().queuePath(getCwd(), AutoThumbnailCreator.SCOPE_TREE);
                }
                else
                {
                    AutoThumbnailCreator.getInstance().queuePath(getCwd(), AutoThumbnailCreator.SCOPE_DIR);
                }
            }
        }

        output.println("</table>");

        output.println("</form>");

		output.print("</body>");
		output.println("</html>");
		output.flush();
	}
	
    protected File createUntarFile(String tarEntryName) {
        File tarOutFile = new File(getCwd(), tarEntryName);

        if (tarEntryName.indexOf('/') >= 0) {
            String dir_name = tarEntryName.substring(0, tarEntryName.lastIndexOf('/'));

            File dir = new File(getCwd(), dir_name);

            if (dir.exists()) {
                return (tarOutFile);
            }

            if (!dir.mkdirs()) {
                logger.error(
                        "Cannot create output directory " + dir);
            }
            return (tarOutFile);
        }

        return (tarOutFile);
    }
}
