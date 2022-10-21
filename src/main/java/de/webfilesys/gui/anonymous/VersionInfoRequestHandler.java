package de.webfilesys.gui.anonymous;

import java.io.PrintWriter;

import de.webfilesys.WebFileSys;

/**
 * @author Frank Hoehnel
 */
public class VersionInfoRequestHandler
{
	private PrintWriter output = null;
        private String contextPath = "/webfilesys";
	
	public VersionInfoRequestHandler(PrintWriter output,String contextPath)
	{
		this.output = output;
                this.contextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
	}

    public void handleRequest()
    {
		output.println("<html>");
		output.println("<head>");
		output.println("<title> WebFileSys Version Info </title>");

		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + this.contextPath + "/styles/common.css\">");
		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + this.contextPath + "/styles/skins/fmweb.css\">");

		output.println("</head>");
		output.println("<body style=\"background-color:#e0e0e0\">");
		output.println("<table border=\"0\" width=\"100%\" cellpadding=\"14\">");
		output.println("<tr><td class=\"story\" style=\"text-align:center\"> WebFileSys </td></tr>");
		output.println("<tr><td class=\"value\" style=\"text-align:center\">" + WebFileSys.VERSION + "</td></tr>");
		output.println("<tr><td style=\"text-align:center\"><a class=\"fn\" href=\"http://www.webfilesys.de\" target=\"_blank\">www.webfilesys.de</a></td></tr>");
		output.println("<tr><td style=\"text-align:center\"><form><input type=\"button\" value=\"關閉\" onClick=\"self.close()\" /></form></td></tr>");
		output.println("</table>");
		output.println("</body></html>");
		output.flush();
    }
}
