package de.webfilesys;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.webfilesys.util.PatternComparator;
import de.webfilesys.util.XmlUtil;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewHandlerManager 
{
    private static final Logger logger = LogManager.getLogger(ViewHandlerManager.class);
	private static final String CONFIG_FILE_NAME = "viewHandler.xml";
	
	private ConcurrentHashMap<String,ViewHandlerConfig> viewHandlerMap = null;
	
	private static ViewHandlerManager manager = null;
	
    private Document doc;

    private DocumentBuilder builder;
	
	public static ViewHandlerManager getInstance()
	{
		if (manager == null)
		{
			manager = new ViewHandlerManager();
		}
		
		return(manager);
	}
	
	private ViewHandlerManager()
	{
		viewHandlerMap = new ConcurrentHashMap<>();
		
		registerViewHandlers();
	}
	
	private void registerViewHandlers()
	{
    	String configPath = WebFileSys.getInstance().getConfigBaseDir() + "/" + CONFIG_FILE_NAME;
    	
        builder = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            
            Element registryElement = loadFromFile(configPath);

            if (registryElement != null)
            {
                NodeList handlerList = registryElement.getElementsByTagName("viewHandler");

                if (handlerList == null)
                {
                	logger.debug("no view handler defined in registry");
                    return;
                }

                int listLength = handlerList.getLength();

                for (int i = 0; i < listLength; i++)
                {
                    Element handlerElement = (Element) handlerList.item(i);
                    
                    String handlerClass = XmlUtil.getChildText(handlerElement, "class");
                    
                    try
                    {
                    	Class.forName(handlerClass);
                    	
                        ViewHandlerConfig handlerConfig = new ViewHandlerConfig();

                        handlerConfig.setHandlerClass(handlerClass);
                        
                        Element parameterListElement = XmlUtil.getChildByTagName(handlerElement, "parameterList");
                        
                        if (parameterListElement != null)
                        {
                        	NodeList parameterList = parameterListElement.getElementsByTagName("parameter");
                        	
                        	int parameterListLength = parameterList.getLength();
                        	
                        	for (int k = 0; k < parameterListLength; k++)
                        	{
                        		Element parameterElement = (Element) parameterList.item(k);
                        		
                        		String paramName = XmlUtil.getChildText(parameterElement, "paramName");
                        		String paramValue = XmlUtil.getChildText(parameterElement, "paramValue");
                        		
                        		if ((paramName != null) && (paramValue != null))
                        		{
                        			handlerConfig.addParameter(paramName, paramValue);
                        		}
                        	}
                        }
                        
                        Element patternListElement = XmlUtil.getChildByTagName(handlerElement, "filePatternList");
                        
                        if (patternListElement != null)
                        {
                        	NodeList patternList = patternListElement.getElementsByTagName("filePattern");
                        	
                        	int patternListLength = patternList.getLength();
                        	
                        	for (int k = 0; k < patternListLength; k++)
                        	{
                            	Element patternElement = (Element) patternList.item(k);
                        		
                        		String pattern = XmlUtil.getElementText(patternElement);
                        		
                                viewHandlerMap.put(pattern, handlerConfig);        

                                logger.info("registering view handler for file pattern " + pattern + ": " + handlerConfig.getHandlerClass());
                        	}
                        }
                    }
                    catch (ClassNotFoundException cnfex)
                    {
                    	logger.error("view handler class not found: " + handlerClass);
                    }
                }
            }
        }
        catch (ParserConfigurationException pcex)
        {
        	logger.error(pcex.toString());
        }
	}
	
    public Element loadFromFile(String configPath)
    {
       File configFile = new File(configPath);

       if ((!configFile.exists()) || (!configFile.canRead()))
       {
    	   logger.debug("view handler config file does not exist");
    	   
           return(null);
       }

       String absoluteFileName = configFile.getAbsolutePath();

       String configFileUrl = null;

       if (absoluteFileName.charAt(0)=='/')
       {
           configFileUrl = "file://" + absoluteFileName;
       }
       else
       {
           configFileUrl = "file:///" + absoluteFileName;
       }

       logger.info("reading view handler config from URL " + configFileUrl);

       doc = null;

       try
       {
           doc = builder.parse(configFileUrl);
       }
       catch (SAXException | IOException saxex)
       {
    	   logger.error("cannot load view handler config: " + saxex);
       }

       if (doc == null)
       {
           return(null);
       }

       return(doc.getDocumentElement());
    }
    
    public ViewHandlerConfig getViewHandlerConfig(String fileName)
    {
    	Enumeration keys = viewHandlerMap.keys();
    	
    	while (keys.hasMoreElements())
    	{
    		String filePattern = (String) keys.nextElement();
    		
    		if (PatternComparator.patternMatch(fileName, filePattern))
    		{
    	    	return((ViewHandlerConfig) viewHandlerMap.get(filePattern));
    		}
    	}

    	return(null);
    }

}
