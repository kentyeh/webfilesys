package de.webfilesys.gui.google;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.w3c.dom.Element;

/**
 * @author Frank Hoehnel
 */
public class GoogleEarthFolderPlacemarkHandler extends GoogleEarthHandlerBase
{
	public GoogleEarthFolderPlacemarkHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
 	}

	protected ArrayList<Element> createPlacemarkXml() 
	{
        String folderPath = getCwd();
        
        if (folderPath.endsWith(File.separator)) 
        {
            folderPath = folderPath + ".";
        }
        else
        {
            folderPath = folderPath + File.separator + ".";
        }
        
        ArrayList<Element> placemarkElementList = new ArrayList();
        
        placemarkElementList.add(createPlacemark(folderPath));
        
        return placemarkElementList;
	}
}
