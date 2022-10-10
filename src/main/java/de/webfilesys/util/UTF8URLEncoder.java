package de.webfilesys.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * URL encoding with charset UTF-8.
 */
public class UTF8URLEncoder
{
    private static final Logger logger = LogManager.getLogger(UTF8URLEncoder.class);
    public static final String encode(String val) 
    {
        try
        {
            return URLEncoder.encode(val, "UTF-8").replaceAll("\\+","%20");
        }
        catch (UnsupportedEncodingException uex)
        {
            logger.error(uex);
        }
        
        return null;
    }
}
