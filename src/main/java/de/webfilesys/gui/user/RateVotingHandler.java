package de.webfilesys.gui.user;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.xsl.XslShowImageHandler;
import de.webfilesys.gui.xsl.album.XslAlbumPictureHandler;
import de.webfilesys.servlet.VisitorServlet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class RateVotingHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(RateVotingHandler.class);
	public RateVotingHandler(
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
		String imagePath = getParameter("imagePath");
		
		if ((imagePath == null) || (imagePath.trim().length() == 0))
		{
			logger.error("RateVotingHandler: imagePath missing");
			return;
		}
		
		if (!this.checkAccess(imagePath))
		{
			return;
		}

		String imagePathOS = imagePath.replace('/', File.separatorChar);
		
        String temp = getParameter("rating");
        
        if (temp == null)
        {
        	logger.error("rating is null");
        	return;
        }
        
        int rating = (-1);
        
        try
        {
        	rating = Integer.parseInt(temp);
        }
        catch (NumberFormatException nfe)
        {
			logger.error("invalid rating: " + temp);
			return;
        }

		MetaInfManager metaInfMgr = MetaInfManager.getInstance();

		if (readonly)
		{
            ConcurrentHashMap<String,Boolean> ratedPictures = (ConcurrentHashMap<String,Boolean>) session.getAttribute("ratedPictures");
			
			if (ratedPictures == null)
	    	{
	    		ratedPictures = new ConcurrentHashMap<>(5);
	    		
	    		session.setAttribute("ratedPictures", ratedPictures);
	    	}

			if (ratedPictures.get(imagePathOS) == null)
            {
				String visitorId = (String) req.getSession().getAttribute(VisitorServlet.SESSION_ATTRIB_VISITOR_ID);
				
				if (visitorId != null) {
					metaInfMgr.addIdentifiedVisitorRating(visitorId, imagePathOS, rating);
				} else {
					metaInfMgr.addVisitorRating(imagePathOS, rating);
				}

				ratedPictures.put(imagePathOS, true);
            }
		}
        else
        {
			metaInfMgr.setOwnerRating(imagePathOS, rating);
        }

        String role = userMgr.getRole(uid);
        
        if ((role != null) && role.equals("album"))
        {
			(new XslAlbumPictureHandler(req, resp, session, output, uid)).handleRequest();
        }
        else
        {
			this.setParameter("imgname", imagePath);

		    (new XslShowImageHandler(req, resp, session, output, uid)).handleRequest(); 
        }
    }

}
