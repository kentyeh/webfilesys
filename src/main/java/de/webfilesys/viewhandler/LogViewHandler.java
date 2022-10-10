package de.webfilesys.viewhandler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import de.webfilesys.ViewHandlerConfig;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Formats the log files of application servers as colored HTML.
 * @author Frank Hoehnel
 */
public class LogViewHandler implements ViewHandler
{
	private static final Logger logger = LogManager.getLogger(LogViewHandler.class);
	private ConcurrentHashMap<String, String> colorMap;
	
	private ConcurrentHashMap<String, Boolean> ignoreMap;
	
	private ArrayList<String> keywordList;
	
	public LogViewHandler() 
	{
		colorMap = new ConcurrentHashMap<>(5);
		
		ignoreMap = new ConcurrentHashMap<>(5);
		
		keywordList = new ArrayList<>();
	}
	
    @Override
    public void process(String filePath, ViewHandlerConfig viewHandlerConfig, HttpServletRequest req, HttpServletResponse resp)
    {
    	String colorConfig = viewHandlerConfig.getParameter("colorConfig");
    	
    	if (colorConfig != null)
    	{
    		StringTokenizer colorParser = new StringTokenizer(colorConfig, "=,");
    		
    		while (colorParser.hasMoreTokens())
    		{
    			String logEntrySeverity = colorParser.nextToken();
    			
    			if (logEntrySeverity.charAt(0) == '\'')
    			{
    				logEntrySeverity = logEntrySeverity.substring(1, logEntrySeverity.length() - 1);
    			}

    			if (colorParser.hasMoreTokens())
    			{
    				String colorCode = colorParser.nextToken();
    				
    				colorMap.put(logEntrySeverity, colorCode);
    				
    				keywordList.add(logEntrySeverity);
    			}
    		}
    	}
    	
        String ignoreConfig = viewHandlerConfig.getParameter("ignoreConfig");
        
        if (ignoreConfig != null)
        {
            StringTokenizer ignoreParser = new StringTokenizer(ignoreConfig, ",");
            
            while (ignoreParser.hasMoreTokens())
            {
                String ignoreParm = ignoreParser.nextToken();
                
                String ignoreText = ignoreParm.substring(1, ignoreParm.length() - 1);
                
                ignoreMap.put(ignoreText, true);

                keywordList.add(ignoreText);
            }
        }
    	
        String charEncoding = viewHandlerConfig.getParameter("charEncoding");
        
    	try
    	{
        	PrintWriter output = resp.getWriter();
        	
        	output.println("<html>");
        	output.println("<head>");
        	output.print("<title>WebFileSys: ");
        	output.print(CommonUtils.extractFileName(filePath));
        	output.println("</title>");
        	output.println("</head>");
        	output.println("<body>");
        	output.println("<pre>");
        	
        	String currentColor = "black";
        	
            FileInputStream fis = new FileInputStream(filePath);
            
            if ((charEncoding != null) && charEncoding.equals("UTF-8-BOM")) {
                // skip over BOM
                fis.read();
                fis.read();
                fis.read();
                charEncoding = "UTF-8";
            }
            
            
            try (InputStreamReader isr = charEncoding==null?new InputStreamReader(fis):new InputStreamReader(fis, charEncoding);
                    BufferedReader fileIn = new BufferedReader(isr)) {
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Reading log file with char encoding " + isr.getEncoding());
                }
                    String line = null;
                    
                    boolean eof = false;
                    
                    int excCounter = 0;
                    
                    while ((!eof) && (excCounter < 5))
                    {
                        try
                        {
                            line = fileIn.readLine();
                            
                            if (line == null)
                            {
                                eof = true;
                            }
                            else
                            {
                                excCounter = 0;
                                
                                boolean ignore = false;
                                
                                for (String keyword : keywordList) {
                                    if (line.contains(keyword))
                                    {
                                        String colorCode = (String) colorMap.get(keyword);
                                        
                                        if (colorCode != null)
                                        {
                                            currentColor = colorCode;
                                        }
                                        else
                                        {
                                            if (ignoreMap.get(keyword) != null) {
                                                ignore = true;
                                            }
                                        }
                                    }
                                }
                                
                                if (!ignore) {
                                    output.print("<font color=\"" + currentColor + "\">");
                                    output.print(htmlEncode(line));
                                    output.println("</font>");
                                }
                            }
                        }
                        catch (Exception ex) {
                            logger.warn("error during reading log file", ex);
                            excCounter++;
                        }
                    }
                    
                    output.println("</pre>");
                    output.println("</body>");
                    output.println("</html>");
                    
                    output.flush();
                }
    	}
    	catch (IOException ioex)
    	{
    		logger.error(ioex);
    	}
    }
    
    private String htmlEncode(String original)
    {
    	String encoded = replaceAll(original, "<", "&lt;");
    	encoded = replaceAll(encoded, ">", "&gt;");
    	return encoded;
    }

    private String replaceAll(String source, String toReplace, String replacement) 
    {
		int idx = source.lastIndexOf(toReplace);
		
		if ( idx < 0)
		{
			return (source);
		}

		StringBuilder ret = new StringBuilder(source);
		
		ret.replace(idx, idx + toReplace.length(), replacement );
		
		while((idx = source.lastIndexOf(toReplace, idx - 1)) != -1 ) 
		{
			ret.replace(idx, idx + toReplace.length(), replacement);
		}

		return(ret.toString());
    }

    
    /**
     * Create the HTML response for viewing the given file contained in a ZIP archive..
     * 
     * @param zipFilePath path of the ZIP entry
     * @param zipIn the InputStream for the file extracted from a ZIP archive
     * @param req the servlet request
     * @param resp the servlet response
     */
    @Override
    public void processZipContent(String zipFilePath, InputStream zipIn, ViewHandlerConfig viewHandlerConfig, HttpServletRequest req, HttpServletResponse resp)
    {
        // not yet supported
        logger.warn("reading from ZIP archive not supported by ViewHaandler " + this.getClass().getName());
    }
    
    /**
     * Does this ViewHandler support reading the file from an input stream of a ZIP archive?
     * @return true if reading from ZIP archive is supported, otherwise false
     */
    @Override
    public boolean supportsZipContent()
    {
        return false;
    }

}
