/*  
 * WebFileSys
 * Copyright (C) 2011 Frank Hoehnel

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package de.webfilesys.decoration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.webfilesys.WebFileSys;
import de.webfilesys.util.XmlUtil;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manager for decoration of folders with individual icons and text colors.
 * @author Frank Hoehnel
 */
public class DecorationManager extends Thread {

    private static final Logger logger = LogManager.getLogger(DecorationManager.class);

    public static final String DECORATION_FILE_NAME = "decorations.xml";
	
    private static DecorationManager decoMgr = null;

    private boolean modified = false;

private final ReentrantLock lock= new ReentrantLock();    

    Document doc;

    DocumentBuilder builder;

    Element decorationRoot = null;
    
    String decorationFilePath = null;
    
    HashMap<String, Decoration> index = null;

    private DecorationManager()
    {
    	decorationFilePath = WebFileSys.getInstance().getConfigBaseDir() + "/" + DECORATION_FILE_NAME;
    	
    	index = new HashMap<>();
    	
        builder = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            
            decorationRoot = loadFromFile();

            if (decorationRoot == null)
            {
                doc = builder.newDocument();

                decorationRoot = doc.createElement("decorations");
            } else {
                createIndex(decorationRoot);
            }
        }
        catch (ParserConfigurationException pcex)
        {
        	logger.error(pcex.toString());
        }

        modified = false;

        start();
    }

    public static DecorationManager getInstance()
    {
        if (decoMgr == null)
        {
            decoMgr = new DecorationManager();
        }

        return(decoMgr);
    }

    public void saveToFile()
    {
        if (decorationRoot == null)
        {
            return;
        }
            
        File decoFile = new File(decorationFilePath);
        
        if (decoFile.exists() && (!decoFile.canWrite()))
        {
        	logger.error("cannot write to decoration file " + decoFile.getAbsolutePath());
            return;
        }

        try {
            this.lock.lock();

            try (FileOutputStream fos = new FileOutputStream(decoFile);
                    OutputStreamWriter xmlOutFile = new OutputStreamWriter(fos, "UTF-8"))
            {
                logger.debug("Saving decorations to file " + decoFile.getAbsolutePath());
                
                XmlUtil.writeToStream(decorationRoot, xmlOutFile);
                
                xmlOutFile.flush();

                modified = false;
            }
            catch (IOException io1)
            {
                logger.error("error saving decoration to file " + decoFile.getAbsolutePath(), io1);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public Element loadFromFile()
    {
       File decorationFile = new File(decorationFilePath);

       if ((!decorationFile.exists()) || (!decorationFile.canRead()))
       {
           return(null);
       }

       logger.info("reading decorations from " + decorationFile.getAbsolutePath());

       doc = null;
       
       try (FileInputStream fis = new FileInputStream(decorationFile))
       {
           InputSource inputSource = new InputSource(fis);
           
           inputSource.setEncoding("UTF-8");

           doc = builder.parse(inputSource);
       }
       catch (SAXException | IOException saxex)
       {
           logger.error("failed to load decoration from file : " + decorationFile.getAbsolutePath(), saxex);
       }
       
       if (doc == null)
       {
           return(null);
       }

       return(doc.getDocumentElement());
    }
    
    private void createIndex(Element decorationRoot) {
        NodeList decorationList = decorationRoot.getElementsByTagName("decoration");

        if (decorationList == null)
        {
            return;
        }

        int listLength = decorationList.getLength();

        for (int i = 0; i < listLength; i++)
        {
        	Element decorationElement = (Element) decorationList.item(i);
            
            String path = XmlUtil.getChildText(decorationElement, "path");
            
            Decoration deco = new Decoration();
            String icon = XmlUtil.getChildText(decorationElement, "icon");
            if ((icon != null) && (icon.length() > 0))
            {
                deco.setIcon(icon);
            }
            String textColor = XmlUtil.getChildText(decorationElement, "textColor");
            if ((textColor != null) && (textColor.length() > 0))
            {
                deco.setTextColor(textColor);
            }
            
            index.put(path, deco);
        }
    }
    
    public Decoration getDecoration(String path)
    {
    	return (Decoration) index.get(path.replace('\\', '/'));
    }
    
    public void setDecoration(String path, Decoration newDeco) 
    {
        try {
            this.lock.lock();
            String normalizedPath = path.replace('\\', '/');    	
        	
            boolean existingFound = false;
            
        	if (index.get(normalizedPath) != null) 
        	{
        		// decoration for this path exists
                NodeList decorationList = decorationRoot.getElementsByTagName("decoration");

                if (decorationList != null)
                {
                    int listLength = decorationList.getLength();

                    for (int i = 0; (!existingFound) && (i < listLength); i++)
                    {
                        Element decorationElement = (Element) decorationList.item(i);
                        String existingPath = XmlUtil.getChildText(decorationElement, "path");
                        
                        if (existingPath.equals(normalizedPath)) 
                        {
                        	if (newDeco.getIcon() != null) 
                        	{
                            	XmlUtil.setChildText(decorationElement, "icon", newDeco.getIcon());
                        	}
                        	else
                        	{
                        		Element oldIcon = XmlUtil.getChildByTagName(decorationElement, "icon");
                        		if (oldIcon != null) {
                        			decorationElement.removeChild(oldIcon);
                        		}
                        	}
                        	if (newDeco.getTextColor() != null) 
                        	{
                            	XmlUtil.setChildText(decorationElement, "textColor", newDeco.getTextColor());
                        	}
                        	else
                        	{
                        		Element oldTextColor = XmlUtil.getChildByTagName(decorationElement, "textColor");
                        		if (oldTextColor != null) {
                        			decorationElement.removeChild(oldTextColor);
                        		}
                        	}
                        	
                        	existingFound = true;
                        }
                    }
                }
        	}
        	
        	if (!existingFound) {
            	Element newDecoElem = decorationRoot.getOwnerDocument().createElement("decoration");
                
            	XmlUtil.setChildText(newDecoElem, "path", normalizedPath);
            	if (newDeco.getIcon() != null) 
            	{
                	XmlUtil.setChildText(newDecoElem, "icon", newDeco.getIcon());
            	}
            	if (newDeco.getTextColor() != null) 
            	{
                 	XmlUtil.setChildText(newDecoElem, "textColor", newDeco.getTextColor());
            	}
             	
            	decorationRoot.appendChild(newDecoElem);
        	}
        	
        	index.put(normalizedPath, newDeco);
        	
        	modified = true;
        } finally {
            this.lock.unlock();
        }
    }
    
    /**
     * Icons available for folder decoration.
     * @return List of filenames of files in the icons directory.
     */
    public ArrayList<String> getAvailableIcons() 
    {
    	ArrayList<String> availableIcons = new ArrayList<>();
    	
    	String iconDirPath = WebFileSys.getInstance().getWebAppRootDir() + "icons";
    	
    	File iconDir = new File(iconDirPath);
    	
    	if (iconDir.exists() && iconDir.isDirectory() && iconDir.canRead())
    	{
    		String[] iconFiles = iconDir.list();
    		
                availableIcons.addAll(Arrays.asList(iconFiles));
    	}
    	
    	if (availableIcons.size() > 1) {
    		Collections.sort(availableIcons);
    	}
    	
    	return availableIcons;
    }
    
    public void collectGarbage()
    {
        try {
            this.lock.lock();
            NodeList decorationList = decorationRoot.getElementsByTagName("decoration");

            if (decorationList == null)
            {
                return;
            }

            ArrayList<String> availableIcons = getAvailableIcons();
            
            int decoGarbageCounter = 0;
            
            int listLength = decorationList.getLength();

            for (int i = listLength - 1; i >= 0; i--)
            {
                Element decorationElement = (Element) decorationList.item(i);
                
                String path = XmlUtil.getChildText(decorationElement, "path");

            	File checkExistFile = new File(path);
            	
            	if (!checkExistFile.exists()) {
                    decorationRoot.removeChild(decorationElement);
                    index.remove(path);
                    modified = true;
                    decoGarbageCounter++;
            	} else {
            		String icon = XmlUtil.getChildText(decorationElement, "icon");
            		if ((icon != null) && (icon.length() > 0))
            		{
                		if (!availableIcons.contains(icon))
                		{
                			decorationElement.removeChild(XmlUtil.getChildByTagName(decorationElement, "icon"));
                            
                			Decoration deco = index.get(path);
                			if (deco != null) {
                				deco.setIcon(null);
                			}
                			
                			modified = true;
                			
                	        logger.info("removing folder decoration for non-existing icon " + icon);
                		}
            		}
            	}
            }            
        	
            logger.info(decoGarbageCounter + " decorations for removed folders deleted");
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public synchronized void run()
    {
        int counter = 1;

        int sleepHours = 1;

        boolean stop = false;
        
        while (!stop)
        {
            try
            {
                this.wait(60000);

                if (modified)
                {
                    saveToFile();

                    modified = false;
                }

                if (++counter == (sleepHours * 60))
                {
                	collectGarbage();

                    counter = 0;

                    sleepHours = 24;
                }
            }
            catch (InterruptedException e)
            {
                if (modified)
                {
                	saveToFile();
                }
				
                logger.debug("DecorationManager ready for shutdown");
                
				stop = true;
            }
        }
    }
}
