package de.webfilesys.gui.xsl;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.webfilesys.Constants;
import de.webfilesys.FileComparator;
import de.webfilesys.FileContainer;
import de.webfilesys.FileLinkSelector;
import de.webfilesys.FileSelectionStatus;
import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.ImageDimensions;
import de.webfilesys.graphics.ImageUtils;
import de.webfilesys.graphics.ThumbnailThread;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.SessionKey;
import de.webfilesys.util.UTF8URLEncoder;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class XslSlideShowInFrameHandler extends XslRequestHandlerBase
{
	public XslSlideShowInFrameHandler(
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
		String actPath = getParameter("actpath");
		
		if (actPath == null)
		{
			actPath = getCwd();
		}
		
		if (!checkAccess(actPath))
		{
			return;
		}
		
        Element slideShowElement = doc.createElement("slideShow");
        
        doc.appendChild(slideShowElement);
            
        ProcessingInstruction xslRef = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/webfilesys/xsl/slideShowInFrame.xsl\"");

        doc.insertBefore(xslRef, slideShowElement);

        if (readonly)
        {
            XmlUtil.setChildText(slideShowElement, "readonly", "true", false);
        }
        else
        {
            XmlUtil.setChildText(slideShowElement, "readonly", "false", false);
        }
        
        addMsgResource("label.slideshow", getResource("label.slideshow", "Picture Slideshow"));
        addMsgResource("alt.exitslideshow", getResource("alt.exitslideshow","exit slideshow"));
        addMsgResource("alt.pause", getResource("alt.pause","pause slideshow"));
        addMsgResource("alt.continue", getResource("alt.continue","continue slideshow"));
        addMsgResource("alt.back", getResource("alt.back","previous picture"));
        addMsgResource("alt.next", getResource("alt.next","next picture"));
		
        // String encodedPath = UTF8URLEncoder.encode(actPath);
        
        String windowWidth = getParameter("windowWidth");
        String windowHeight = getParameter("windowHeight");

        int windowXSize = 300;
        int windowYSize = 300;
        
        try
        {
            windowXSize = Integer.parseInt(windowWidth);
            windowYSize = Integer.parseInt(windowHeight);   
        }
        catch (NumberFormatException nfex)
        {
        }
        
        String screenWidthParm = getParameter("screenWidth");
        String screenHeightParm = getParameter("screenHeight");

        if (screenWidthParm!=null)
        {
            try
            {
                int newScreenWidth = Integer.parseInt(screenWidthParm);

                session.setAttribute("screenWidth", newScreenWidth);
            }
            catch (NumberFormatException nfex)
            {
            }
        }

        if (screenHeightParm!=null)
        {
            try
            {
                int newScreenHeight = Integer.parseInt(screenHeightParm);

                session.setAttribute("screenHeight", newScreenHeight);
            }
            catch (NumberFormatException nfex)
            {
            }
        }

        int screenWidth = Constants.DEFAULT_SCREEN_WIDTH;
        int screenHeight = Constants.DEFAULT_SCREEN_HEIGHT;

        Integer widthScreen = (Integer) session.getAttribute("screenWidth");
        
        if (widthScreen != null)
        {
            screenWidth = widthScreen;
        }

        Integer heightScreen = (Integer) session.getAttribute("screenHeight");
        
        if (heightScreen != null)
        {
            screenHeight = heightScreen;
        }

        int delay = WebFileSys.getInstance().getSlideShowDelay();
        
        int imageIdx = (-1);
        try
        {
            String delayParam = getParameter("delay");
            if (delayParam != null)
            {
                delay = Integer.parseInt(delayParam);
            }
            
            String indexParam = getParameter("imageIdx");
            if (!CommonUtils.isEmpty(indexParam))
            {
                imageIdx = Integer.parseInt(indexParam);
            }
        }
        catch (NumberFormatException nfe)
        {
        }

        boolean autoForward = (!CommonUtils.isEmpty(getParameter("autoForward")));

        boolean recurse = (!CommonUtils.isEmpty(getParameter("recurse")));

        boolean crossfade = (!CommonUtils.isEmpty(req.getParameter("crossfade")));

        boolean randomize = (!CommonUtils.isEmpty(getParameter("randomize")));
        
        String role = userMgr.getRole(uid);
        
        if ((role != null) && role.equals("album"))
        {
            XmlUtil.setChildText(slideShowElement, "album", "true", false);
        }        
        
        ArrayList<String> imageFiles = null;
        
        if (imageIdx < 0)
        {
            session.removeAttribute(SessionKey.SLIDESHOW_BUFFER);
            getImageTree(actPath, recurse, randomize);

            String startFilePath = getParameter("startFilePath");
			if (startFilePath == null) {
				imageIdx = 0;
			} 
			else 
			{
				imageIdx = getStartFileIndex(startFilePath);
			}
        }
        else
        {
            imageFiles = (ArrayList<String>) session.getAttribute(SessionKey.SLIDESHOW_BUFFER); 
            if ((imageFiles == null) || (imageIdx>=imageFiles.size()))
            {
                session.removeAttribute(SessionKey.SLIDESHOW_BUFFER);
                getImageTree(actPath, recurse, randomize);
                imageIdx=0;
            }
        }

        imageFiles = (ArrayList<String>) session.getAttribute(SessionKey.SLIDESHOW_BUFFER); 
        
        XmlUtil.setChildText(slideShowElement, "imageCount", Integer.toString(imageFiles.size()), false);
        
        if (imageFiles.isEmpty()) {
            addMsgResource("alert.nopictures", getResource("alert.nopictures","No picture files (JPG,GIF,PNG) exist in this directory"));
            XmlUtil.setChildText(slideShowElement, "shortPath", getHeadlinePath(CommonUtils.shortName(actPath, 50)), false);

            processResponse("slideShowInFrame.xsl");
            return;
        }
        
        XmlUtil.setChildText(slideShowElement, "delay", Integer.toString(delay), false);

        if (autoForward)
        {
            XmlUtil.setChildText(slideShowElement, "autoForward", "true", false);
        }

        if (crossfade)
        {
            XmlUtil.setChildText(slideShowElement, "crossfade", "true", false);
        }
        
        XmlUtil.setChildText(slideShowElement, "imgIdx", Integer.toString(imageIdx), false);

        String imgPath = imageFiles.get(imageIdx);
        
        XmlUtil.setChildText(slideShowElement, "imgPath", imgPath, false);
        
        XmlUtil.setChildText(slideShowElement, "shortImgName", getHeadlinePath(CommonUtils.shortName(imgPath, 50)), false);
        
        XmlUtil.setChildText(slideShowElement, "encodedPath", UTF8URLEncoder.encode(imgPath), false);

        if (imageIdx == 0) 
        {
            XmlUtil.setChildText(slideShowElement, "firstImg", "true", false);
        }
        
        int nextImageIdx = imageIdx + 1;
        
        if (nextImageIdx >= imageFiles.size())
        {
            nextImageIdx = 0;
        }
        
        XmlUtil.setChildText(slideShowElement, "nextImgIdx", Integer.toString(nextImageIdx), false);

        String nextFileName = imageFiles.get(nextImageIdx);

        nextFileName = UTF8URLEncoder.encode(nextFileName);
		
        XmlUtil.setChildText(slideShowElement, "nextImgPath", nextFileName, false);
		
        if (imageIdx > 0) 
        {
            XmlUtil.setChildText(slideShowElement, "prevImgIdx", Integer.toString(imageIdx - 1), false);
        }
            
  		ImageDimensions scaledDim = ImageUtils.getScaledImageDimensions(imgPath, windowXSize - 20, windowYSize - 90);
        	
        XmlUtil.setChildText(slideShowElement, "displayWidth", Integer.toString(scaledDim.getWidth()), false);
        XmlUtil.setChildText(slideShowElement, "displayHeight", Integer.toString(scaledDim.getHeight()), false);
		
        if (autoForward)
        {
            String pause = getParameter("pause");

            if ((pause != null) && pause.equalsIgnoreCase("true"))
            {
                XmlUtil.setChildText(slideShowElement, "paused", "true", false);
            }
        }
        
        MetaInfManager metaInfMgr = MetaInfManager.getInstance();

        String description = metaInfMgr.getDescription(imgPath);

        if ((description != null) && (description.trim().length() > 0))
        {
            XmlUtil.setChildText(slideShowElement, "description", description, true);
        }
        
		processResponse("slideShowInFrame.xsl");
	}
	
    public void getImageTree(String actPath, boolean recurse, boolean randomize)
    {
        int i;

        String pathWithSlash=null;
        if (actPath.endsWith(File.separator))
        {
            pathWithSlash=actPath;
        }
        else
        {
            pathWithSlash=actPath + File.separator;
        }

        ArrayList<String> imageTree = (ArrayList<String>) session.getAttribute(SessionKey.SLIDESHOW_BUFFER);
        if (imageTree == null)
        {
            imageTree = new ArrayList<>();
            session.setAttribute(SessionKey.SLIDESHOW_BUFFER,imageTree);
        }

        FileLinkSelector fileSelector=new FileLinkSelector(actPath,FileComparator.SORT_BY_FILENAME);

        FileSelectionStatus selectionStatus=fileSelector.selectFiles(Constants.imgFileMasks,4096,null,null);

        ArrayList<FileContainer> imageFiles = null;
        if (randomize) {
            imageFiles = selectionStatus.getRandomizedFiles();
        } else {
            imageFiles = selectionStatus.getSelectedFiles();
        }

        if (imageFiles!=null)
        {
            for (FileContainer fileCont: imageFiles){
                imageTree.add(fileCont.getRealFile().getAbsolutePath());
            }
        }

        // and now recurse into subdirectories

        if (!recurse)
        {
            return;
        }

        File dirFile;
        File tempFile;
        String subDir;
        String fileList[]=null;

        dirFile = new File(actPath);
        fileList = dirFile.list();

        if (fileList == null)
        {
            return;
        }

        for (i = 0; i < fileList.length; i++)
        {
            if (!fileList[i].equals(ThumbnailThread.THUMBNAIL_SUBDIR))
            {
                tempFile = new File(pathWithSlash + fileList[i]);

                if (tempFile.isDirectory())
                {
                    subDir = pathWithSlash + fileList[i];

                    getImageTree(subDir, recurse, randomize);
                }
            }
        }
    }
    
    private int getStartFileIndex(String startFilePath) 
    {
		ArrayList<String> imageTree = (ArrayList<String>) session.getAttribute(SessionKey.SLIDESHOW_BUFFER);
		if (imageTree == null)
		{
			return 0;
		}

		for (int i = 0; i < imageTree.size(); i++) 
		{
			String fileName = imageTree.get(i);
			if (fileName.equals(startFilePath))
			{
				return i;
			}
		}
		return 0;
    }
	
}
