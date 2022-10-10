package de.webfilesys.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.webfilesys.WebFileSys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CSSManager
{
    private static final Logger logger = LogManager.getLogger(CSSManager.class);

    public static final String DEFAULT_LAYOUT = "fmweb";

    private static final String CSS_DIR    = "styles/skins";
    
    private final ArrayList<String> availableCss;
    
    private static CSSManager layoutMgr=null;
    
    private String cssPath = null;

    private CSSManager()
    {
    	cssPath = WebFileSys.getInstance().getWebAppRootDir() + "/" + CSS_DIR;
    	
        availableCss = new ArrayList<>();

        readAvailableCss();      
    }

    public static CSSManager getInstance()
    {
        if (layoutMgr==null)
        {
            layoutMgr=new CSSManager();
        }

        return(layoutMgr);
    }

    protected void readAvailableCss()
    {
        File cssDir = new File(cssPath);

        if ((!cssDir.exists()) || (!cssDir.isDirectory()) || (!cssDir.canRead()))
        {
            logger.error("CSS directory not found or not readable: " + cssPath);
             
            return;
        } 

        String cssList[] = cssDir.list();

        for (String cssFileName : cssList) {
            if (cssFileName.endsWith(".css"))
            {
                File cssFile = new File(cssPath + "/" + cssFileName);
                
                if (cssFile.isFile() && cssFile.canRead() && (cssFile.length() > 0L))
                {
                    String cssName = cssFileName.substring(0, cssFileName.lastIndexOf('.'));
                    
                    availableCss.add(cssName);
                }
            }
        }

        if (availableCss.size() > 1)
        {
            Collections.sort(availableCss);
        }
    }

    public ArrayList<String> getAvailableCss()
    {
        return(availableCss);
    }
}

