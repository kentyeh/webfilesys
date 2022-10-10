package de.webfilesys.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * URL decoding with charset UTF-8.
 */
public class UTF8URLDecoder
{
    private static final Logger logger = LogManager.getLogger(UTF8URLDecoder.class);
    public static final String decode(String val) 
    {
        try
        {
            return URLDecoder.decode(val, "UTF-8");
        }
        catch (UnsupportedEncodingException uex)
        {
            logger.error(uex);
        }
        
        return null;
    }
}
