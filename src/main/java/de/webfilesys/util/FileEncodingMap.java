package de.webfilesys.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;


import de.webfilesys.WebFileSys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileEncodingMap 
{
    private static final Logger logger = LogManager.getLogger(FileEncodingMap.class);
    private static final String MAPPING_FILE = "fileEncoding.conf";

    private Properties encodingMap = null;

    private static FileEncodingMap instance = null;

    private FileEncodingMap()
    {
        encodingMap = new Properties();
        String propFilePath = WebFileSys.getInstance().getConfigBaseDir() + "/" + MAPPING_FILE;
        try(FileInputStream fin = new FileInputStream(propFilePath))
        {
        	
        	
            logger.debug("reading file encoding map from " + propFilePath);
            
        	encodingMap.load(fin);
        }
        catch (IOException ioex)
        {
            logger.error("Failed to read file encoding configuration", ioex);
        }
    }

    public synchronized static FileEncodingMap getInstance()
    {
        if (instance == null)
        {
            instance = new FileEncodingMap();
        }

        return(instance);
    }

    public String getFileEncoding(String fileName)
    {
        Enumeration keys = encodingMap.keys();
        
        while (keys.hasMoreElements()) 
        {
            String key = (String) keys.nextElement();
            
            if (PatternComparator.patternMatch(fileName, key)) 
            {
                return (String) encodingMap.get(key); 
            }
        }
        
        return null;
    }
}


