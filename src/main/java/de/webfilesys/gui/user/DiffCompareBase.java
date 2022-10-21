package de.webfilesys.gui.user;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;


import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiffCompareBase extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(DiffCompareBase.class);
    private static final long MAX_COMPARE_FILE_SIZE = 200 * 1024L;
    
    public DiffCompareBase(
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
        String file1Path = getParameter("file1Path");
        String file2Path = getParameter("file2Path");

        if ((file1Path == null) || (file2Path == null))
        {
            logger.error("missing file path for diff");
        }
        
        if ((!checkAccess(file1Path)) || (!checkAccess(file2Path)))
        {
            return;
        }
        
        output.println("<html>");
        output.println("<head>");
        output.print("<title>");
        output.print("WebFileSys: " + getResource("title.diff","Compare Files (diff)"));
        output.println("</title>");

		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/common.css\">");
        output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/skins/" + userMgr.getCSS(uid) + ".css\">");

		output.println("<script src=\"javascript/util.js\" type=\"text/javascript\"></script>");
		output.println("<script src=\"javascript/fileDiff.js\" type=\"text/javascript\"></script>");
        
        File file1 = new File(file1Path);
        File file2 = new File(file2Path);
        
        if ((file1.length() > MAX_COMPARE_FILE_SIZE) || (file2.length() > MAX_COMPARE_FILE_SIZE))
        {
            output.println("<script type=\"text/javascript\">");
            output.println("alert('" + getResource("compareFileSizeExceeded", "One or both of the files are too large for compare operation!") + "');");
            output.println("self.close();");
            output.println("</script>");
            output.println("</head>");
            output.println("</html>");
            output.flush();
            return;
        }
        
        String file1Content = readIntoBuffer(file1); 
        String file2Content = readIntoBuffer(file2); 
        
        diff_match_patch differ = new diff_match_patch();
        
        LinkedList differences = differ.diff_main(file1Content, file2Content);  

        Iterator iter = differences.iterator();

        int diffCount = 0;

        while (iter.hasNext())
        {
            Diff diffElem = (Diff) iter.next();
            
            if ((diffElem.operation == Operation.DELETE) || (diffElem.operation == Operation.INSERT)) 
            {
                diffCount++;
            }
        }
        
        output.println("</head>");
        output.println("<body class=\"diff\" onload=\"addScrollListener()\">");
        
        output.println("<table width=\"100%\" border=\"0\">");
        output.println("<tr>");
        output.println("<td class=\"diff\">");
        
        output.println("<span class=\"diff\" title=\"" + file1.getAbsolutePath() + "\">");
        output.println(CommonUtils.shortName(getHeadlinePath(file1.getAbsolutePath()), 36));
        output.println("</span>");
       
        output.println("<div id=\"file1Cont\" class=\"diff\">");
        
        output.println("<pre>");

        StringTokenizer lineParser = new StringTokenizer(file1Content, "\n", true);
        
        while (lineParser.hasMoreTokens())
        {
            String token = lineParser.nextToken();
            
            if (token.equals("\n"))
            {
                output.println();
            }
            else
            {
                output.print("<font class=\"diff\">");
                output.print(encodeSpecialChars(token));
                output.print("</font>");
            }
        }
        
        output.println("</pre>");
        output.println("</div>");

        output.println("</td>");
        output.println("<td class=\"diff\">");
        output.println("<span class=\"diff\">");
        output.print(getResource("compareResult", "compare result"));
        output.println(": " + diffCount + " " + getResource("differences", "differences"));
        output.println("</span>");
        
        if (diffCount > 0) {
            output.println("&nbsp;&nbsp;");
            output.println("<a href=\"javascript:gotoFirstDiff()\"><img src=\""+ req.getContextPath() +"/images/first.gif\"></a>");
            output.println("&nbsp;");
            output.println("<a href=\"javascript:gotoPrevDiff()\"><img src=\""+ req.getContextPath() +"/images/previous.gif\"></a>");
            output.println("&nbsp;");
            output.println("<a href=\"javascript:gotoNextDiff()\"><img src=\""+ req.getContextPath() +"/images/next.gif\"></a>");
            output.println("&nbsp;");
            output.println("<a href=\"javascript:gotoLastDiff()\"><img src=\""+ req.getContextPath() +"/images/last.gif\"></a>");
        }
        
        output.println("<div id=\"diffCont\" class=\"diff\">");
        
        output.println("<pre>");
        
        boolean prevElemEndsWithLinefeed = false;

        iter = differences.iterator();

        while (iter.hasNext())
        {
            Diff diffElem = (Diff) iter.next();
            
            String diffText = diffElem.text;
            
            boolean currentElemEndsWithLinefeed = (diffText.charAt(diffText.length() - 1) == '\n');
            
            if ((diffElem.operation == Operation.INSERT) || (diffElem.operation == Operation.DELETE))
            {
                diffText = highlightLonelyLinefeeds(diffText, prevElemEndsWithLinefeed);
            }
            else
            {
                diffText = encodeSpecialChars(diffText);
            }
            
            // diffText = newLineToHTML(diffText);
            
            if (diffElem.operation == Operation.EQUAL)
            {
                output.print("<font class=\"diffEqual\">");
                output.print(diffText);
                output.print("</font>");
            }
            else if (diffElem.operation == Operation.DELETE)
            {
                output.print("<i class=\"diffDel\">");
                output.print(diffText);
                output.print("</i>");
                diffCount++;
            }
            else if (diffElem.operation == Operation.INSERT)
            {
                output.print("<i class=\"diffIns\">");
                output.print(diffText);
                output.print("</i>");
                diffCount++;
            }
            
            prevElemEndsWithLinefeed = currentElemEndsWithLinefeed;
        }

        output.println("</pre>");

        output.println("</div>");

        output.println("</td>");
        output.println("<td class=\"diff\">");
        output.println("<span class=\"diff\" title=\"" + file2.getAbsolutePath() + "\">");
        output.println(CommonUtils.shortName(getHeadlinePath(file2.getAbsolutePath()), 36));
        output.println("</span>");
        
        output.println("<div id=\"file2Cont\" class=\"diff\">");

        output.println("<pre>");

        lineParser = new StringTokenizer(file2Content, "\n", true);
        
        while (lineParser.hasMoreTokens())
        {
            String token = lineParser.nextToken();
            
            if (token.equals("\n"))
            {
                output.println();
            }
            else
            {
                output.print("<font class=\"diff\">");
                output.print(encodeSpecialChars(token));
                output.print("</font>");
            }
        }
        
        output.println("</pre>");
        output.println("</div>");
        output.println("</td>");
        output.println("</tr>");
        output.println("</table>");
        
        output.println("</body>");
        
    	output.println("<script type=\"text/javascript\">");
    	output.println("resizeToFitScreen();");
    	if (diffCount == 0) {
            output.println("customAlert('" + getResource("noDifferences", "No differences found.") + "');");
        }
        output.println("</script>");

        output.println("</html>");
        output.flush();
    }
    
    private boolean checkForLonelyLinefeeds(String diffText)
    {
        boolean noneLinefeedChar = false;

        boolean lonelyLinefeed = false;
        
        for (int i = 0; (!lonelyLinefeed) && (i < diffText.length()); i++) 
        {
            char ch = diffText.charAt(i);
            
            if ((ch != '\n') && (ch != '\r'))
            {
                noneLinefeedChar = true;
            }
            else
            {
                if ((ch == '\n'))
                {
                    if (!noneLinefeedChar)
                    {
                        lonelyLinefeed = true;
                    }
                    else
                    {
                        noneLinefeedChar = false;
                    }
                }
            }
        }
        
        return lonelyLinefeed;
    }
    
    private String highlightLonelyLinefeeds(String diffText, boolean prevElemEndsWithLinefeed)
    {
        if (!checkForLonelyLinefeeds(diffText))
        {
            return encodeSpecialChars(diffText);
        }
        
        StringBuilder highlightBuff = new StringBuilder(diffText.length() + 1);
        
        boolean noneLinefeedChar = false;
        
        boolean crPending = false;
        
        for (int i = 0; i < diffText.length(); i++) 
        {
            char ch = diffText.charAt(i);
            
            if ((ch != '\n') && (ch != '\r'))
            {
                noneLinefeedChar = true;
                highlightBuff.append(ch);
            }
            else
            {
                if ((ch == '\n'))
                {
                    if (!noneLinefeedChar)
                    {
                        if (prevElemEndsWithLinefeed) 
                        {
                            highlightBuff.append(' ');
                        }
                    }

                    if (crPending)
                    {
                        highlightBuff.append('\r');
                        crPending = false;
                    }
                    
                    highlightBuff.append(ch);

                    if (!noneLinefeedChar)
                    {
                        if (!prevElemEndsWithLinefeed) 
                        {
                            highlightBuff.append(' ');
                        }
                    }
                    
                    noneLinefeedChar = false;
                }
                else
                {
                    crPending = true;
                }
            }
        }
        
        return encodeSpecialChars(highlightBuff.toString());
    }
    
    private String readIntoBuffer(File file) {
        
        String fileEncoding = guessFileEncoding(file.getAbsolutePath());
        
        if (fileEncoding != null) {
            logger.debug("reading diff file " + file.getAbsolutePath() + " with character encoding " + fileEncoding);
        }
        
        StringBuilder buff = new StringBuilder();     
        
        
        try (BufferedReader fileIn = fileEncoding == null ? new BufferedReader(new FileReader(file))
                : new BufferedReader(new InputStreamReader(new FileInputStream(file), 
                        "UTF-8-BOM".equals(fileEncoding) ? "UTF-8" : fileEncoding)))
        {
            
            String line = null;
            
            boolean firstLine = true;
            
            String UTF8_BOM = "\uFEFF";

            while ((line = fileIn.readLine()) != null)
            {
                if (firstLine) 
                {
                    if(line.startsWith(UTF8_BOM)){
                        line = line.replace(UTF8_BOM, "");
                    }
                    firstLine = false;
                }
                else
                {
                    buff.append("\n");
                }
                buff.append(line);
            }
        }
        catch (IOException ioex)
        {
            logger.error(ioex);
        }
        
        return buff.toString();
    }
    
    private String encodeSpecialChars(String line)
    {
        StringBuilder buff = new StringBuilder();

        for (int i = 0; i < line.length(); i++)
        {
            char ch = line.charAt(i);

            if (ch=='&')
            {
                buff.append("&amp;");
            }
            else if (ch == '<')
            {
                buff.append("&lt;");
            }
            else if (ch == '>')
            {
                buff.append("&gt;");
            }
            else if (ch == '"')
            {
                buff.append("&quot;");
            }
            else
            {
                if ((ch < 0x20) && (ch != 0x0a) && (ch != 0x0d))
                {
                    buff.append('.');
                }
                else
                {
                    buff.append(ch);
                }
            }
        }

        return(buff.toString());
    }
    
    private String newLineToHTML(String line)
    {
        StringBuilder buff = new StringBuilder();

        for (int i = 0; i < line.length(); i++)
        {
            char ch = line.charAt(i);

            if (ch == '\r')
            {
                buff.append("<br/>");
                if ((i + 1 < line.length()) && (line.charAt(i + 1) == '\n'))
                {
                    // ignore newline
                    i++;
                }
            }
            else if (ch == '\n')
            {
                buff.append("<br/>");
            }
            else 
            {
                buff.append(ch);
            }
        }

        return(buff.toString());
    }
    
}
