package de.webfilesys.gui.google;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.w3c.dom.Element;

/**
 * @author Frank Hoehnel
 */
public class GoogleEarthSinglePlacemarkHandler extends GoogleEarthHandlerBase
{
	public GoogleEarthSinglePlacemarkHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
 	}

        @Override
	protected ArrayList<Element> createPlacemarkXml() 
	{
        String imgPath = req.getParameter("path");
        
        ArrayList<Element> placemarkElementList = new ArrayList();
        
        placemarkElementList.add(createPlacemark(imgPath));
        
        return placemarkElementList;
	}
}
