package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.webfilesys.util.XmlUtil;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSysBookmarkManager extends Thread
{
    private static final Logger logger = LogManager.getLogger(FileSysBookmarkManager.class);
    public static final String BOOKMARK_DIR    = "bookmarks";
	
    HashMap<String, Element> bookmarkTable = null;

    HashMap<String, HashMap<String, Element>> indexTable = null;

    HashMap<String, Boolean> cacheDirty = null;
    
    DocumentBuilder builder = null;
    
    String bookmarkFileName = null;
    
    boolean shutdownFlag = false;

    private static FileSysBookmarkManager bookmarkManager = null;
    
    private String bookmarkPath = null;

    private final ReentrantLock lock = new ReentrantLock();    
    private FileSysBookmarkManager()
    {
    	bookmarkPath = WebFileSys.getInstance().getConfigBaseDir() + "/" + BOOKMARK_DIR;
    	
        bookmarkTable = new HashMap<>();
        
        indexTable = new HashMap<>();
        
        cacheDirty = new HashMap<>();

        shutdownFlag = false;
        
        builder = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException pcex)
        {
            logger.error(pcex);
        }

        this.start();
    }

    public static FileSysBookmarkManager getInstance()
    {
        if (bookmarkManager == null)
        {
            bookmarkManager = new FileSysBookmarkManager();
        }

        return(bookmarkManager);
    }

    public Element getBookmarkList(String userid)
    {
        Element bookmarkList = bookmarkTable.get(userid);

        if (bookmarkList!=null)
        {
            return(bookmarkList);
        }
    
        bookmarkFileName = bookmarkPath + File.separator + userid + ".xml";

        File bookmarkFile = new File(bookmarkFileName);

        if (bookmarkFile.exists() && bookmarkFile.isFile())
        {
            if (!bookmarkFile.canRead())
            {
                logger.error("cannot read bookmark file for user " + userid);
                return(null);
            }

            bookmarkList = readBookmarkList(bookmarkFile.getAbsolutePath());

            if (bookmarkList != null)
            {
                bookmarkTable.put(userid, bookmarkList);
                createIndex(bookmarkList, userid);

                return(bookmarkList);
            }
        }
        
        return(null);
    }

    Element readBookmarkList(String bookmarkFilePath)
    {
        File categoryFile = new File(bookmarkFilePath);

        if ((!categoryFile.exists()) || (!categoryFile.canRead()))
        {
            return(null);
        }
        
        Document doc = null;

        try (FileInputStream fis = new FileInputStream(categoryFile))
        {
            InputSource inputSource = new InputSource(fis);
            
            inputSource.setEncoding("UTF-8");

            logger.debug("reading bookmarks from " + bookmarkFilePath);

            doc = builder.parse(inputSource);
        }
        catch (SAXException | IOException saxex)
        {
            logger.error("failed to load category file : " + bookmarkFilePath, saxex);
        }
        
        return(doc.getDocumentElement());
    }

    protected void createIndex(Element bookmarkList, String userid)
    {
        NodeList bookmarks = bookmarkList.getElementsByTagName("bookmark");

        if (bookmarks == null)
        {
            indexTable.remove(userid);
            return;
        }

        int listLength = bookmarks.getLength();

        HashMap<String, Element> userIndex = new HashMap<>();

        for (int i = 0; i < listLength; i++)
        {
             Element bookmark =(Element) bookmarks.item(i);

             String bookmarkId = bookmark.getAttribute("id");

             if (bookmarkId!=null)
             {
                 userIndex.put(bookmarkId, bookmark);
             }
        }

        indexTable.put(userid, userIndex);
    }

    public void disposeBookmarkList(String userid)
    {
        Boolean dirtyFlag = cacheDirty.get(userid);

        if ((dirtyFlag!=null) && dirtyFlag)
        {
            saveToFile(userid);
        }

        if (bookmarkTable.get(userid) != null)
        {
			logger.debug("disposing bookmark list of user " + userid);
        }

        bookmarkTable.remove(userid);
        indexTable.remove(userid);
    }

    public void disposeAllBookmarks()
    {
        saveChangedUsers();

        bookmarkTable = new HashMap<>();
        indexTable = new HashMap<>();
    }

    public ArrayList<String> getBookmarkIds(String userid)
    {
        Element bookmarkList = getBookmarkList(userid);

        ArrayList<String> bookmarkIds = null;

        if (bookmarkList == null)
        {
            // System.out.println("bookmark list for user " + userid + " does not exist!");
            return(null);
        }

        NodeList bookmarks = bookmarkList.getElementsByTagName("bookmark");

        if (bookmarks != null)
        {
            int listLength = bookmarks.getLength();

            for (int i=0; i < listLength;i++)
            {
                Element bookmark = (Element) bookmarks.item(i);

                if (bookmarkIds == null)
                {
                    bookmarkIds = new ArrayList<String>();
                }

                bookmarkIds.add(bookmark.getAttribute("id"));
            }
        }
        else
        {
            logger.debug("no bookmarks found for userid " + userid);
        }
    
        return(bookmarkIds);
    }

    public ArrayList<FileSysBookmark> getListOfBookmarks(String userid)
    {
        Element bookmarkList = getBookmarkList(userid);

        ArrayList<FileSysBookmark> listOfBookmarks = new ArrayList<>();

        if (bookmarkList==null)
        {
            logger.debug("bookmark list for user " + userid + " does not exist!");

            return(listOfBookmarks);
        }

        NodeList bookmarks = bookmarkList.getElementsByTagName("bookmark");

        if (bookmarks != null)
        {
            int listLength = bookmarks.getLength();
            
            for (int i=0; i < listLength; i++)
            {
                Element bookmark = (Element) bookmarks.item(i);

                FileSysBookmark newBookmark = new FileSysBookmark(bookmark.getAttribute("id"));

                newBookmark.setName(XmlUtil.getChildText(bookmark, "name"));

                newBookmark.setPath(XmlUtil.getChildText(bookmark, "path"));

                long creationTime = 0L;
                
                String timeString = XmlUtil.getChildText(bookmark, "creationTime");
                
                try
                {
                    creationTime=Long.parseLong(timeString);
                }
                catch (NumberFormatException nfe)
                {
                    logger.warn(nfe);
                    creationTime=(new Date()).getTime();
                }

                newBookmark.setCreationTime(new Date(creationTime));

                long updateTime=0L;
                timeString = XmlUtil.getChildText(bookmark, "updateTime");
                try
                {
                    updateTime=Long.parseLong(timeString);
                }
                catch (NumberFormatException nfe)
                {
                    logger.warn(nfe);
                    updateTime=(new Date()).getTime();
                }

                newBookmark.setUpdateTime(new Date(updateTime));

                listOfBookmarks.add(newBookmark);
            }
        }
    
        if (listOfBookmarks != null)
        {
            Collections.sort(listOfBookmarks, new FileSysBookmarkComparator());
        }

        return(listOfBookmarks);
    }

    public FileSysBookmark getBookmark(String userid, String searchedId)
    {
        Element bookmark = getBookmarkElement(userid, searchedId);

        if (bookmark == null)
        {
            logger.warn("bookmark for user " + userid + "id " + searchedId + " does not exist!");
            return(null);
        }

        FileSysBookmark foundBookmark = new FileSysBookmark(bookmark.getAttribute("id"));

        foundBookmark.setName(XmlUtil.getChildText(bookmark, "name"));

        foundBookmark.setPath(XmlUtil.getChildText(bookmark, "path"));

        long creationTime=0L;
        String timeString = XmlUtil.getChildText(bookmark, "creationTime");
        try
        {
            creationTime=Long.parseLong(timeString);
        }
        catch (NumberFormatException nfe)
        {
            logger.error(nfe);
            creationTime=(new Date()).getTime();
        }

        foundBookmark.setCreationTime(new Date(creationTime));

        long updateTime = 0L;
        timeString = XmlUtil.getChildText(bookmark, "updateTime");
        try
        {
             updateTime=Long.parseLong(timeString);
        }
        catch (NumberFormatException nfe)
        {
			logger.error(nfe);
            updateTime=(new Date()).getTime();
        }

        foundBookmark.setUpdateTime(new Date(updateTime));

        return(foundBookmark);
    }

    protected Element getBookmarkElement(String userid, String searchedId)
    {
        Element bookmarkList = getBookmarkList(userid);

        if (bookmarkList == null)
        {
            return(null);
        }

        Element bookmark = null;

        HashMap<String, Element> userIndex = indexTable.get(userid);

        if (userIndex!=null)
        {
            bookmark = (Element) userIndex.get(searchedId);

            if (bookmark != null)
            {
                return(bookmark);
            }
        }

        logger.warn("bookmark with id " + searchedId + " not found in index");

        NodeList bookmarks = bookmarkList.getElementsByTagName("bookmark");

        if (bookmarks == null)
        {
            return(null);
        }

        int listLength = bookmarks.getLength();

        for (int i = 0; i < listLength; i++)
        {
            bookmark = (Element) bookmarks.item(i);

            if (bookmark.getAttribute("id").equals(searchedId))
            {
                return(bookmark);
            }
        }
    
        return(null);
    }

    protected Element createBookmarkList(String userid)
    {
        logger.debug("creating new bookmark list for user : " + userid);
        
        Document doc = builder.newDocument();

        Element bookmarkListElement = doc.createElement("bookmarkList");

        Element lastIdElement = doc.createElement("lastId");
        XmlUtil.setElementText(lastIdElement,"0");

        bookmarkListElement.appendChild(lastIdElement);
        
        doc.appendChild(bookmarkListElement);

        bookmarkTable.put(userid, bookmarkListElement);

        indexTable.put(userid, new HashMap<>());
        
        return(bookmarkListElement);
    }

    public Element createBookmark(String userid, FileSysBookmark newBookmark)
    {
        Element bookmarkList = getBookmarkList(userid);

        if (bookmarkList == null)
        {
            bookmarkList = createBookmarkList(userid);
        }

        Element newElement = null;
        try{
            this.lock.lock();
            Document doc = bookmarkList.getOwnerDocument();

            newElement = doc.createElement("bookmark");

            newElement.appendChild(doc.createElement("name"));
            newElement.appendChild(doc.createElement("path"));
            newElement.appendChild(doc.createElement("creationTime"));
            newElement.appendChild(doc.createElement("updateTime"));

            bookmarkList.appendChild(newElement);

            int lastId = getLastId(userid);

            lastId++;

            setLastId(userid, lastId);

            String newIdString = Integer.toString(lastId);

            newBookmark.setId(newIdString);
            newElement.setAttribute("id", newIdString);
            
            HashMap<String, Element> userIndex = indexTable.get(userid);
            userIndex.put(newIdString, newElement);
        } finally {
            this.lock.unlock();
        }

        updateBookmark(userid, newBookmark);

        return(newElement);
    }

    protected int getLastId(String userid)
    {
        Element bookmarkList = getBookmarkList(userid);

        if (bookmarkList == null)
        {
            return(-1);
        }

        String lastIdString = XmlUtil.getChildText(bookmarkList, "lastId").trim();

        int lastId=0;
        try
        {
            lastId=Integer.parseInt(lastIdString);
        }
        catch (NumberFormatException nfe)
        {
            logger.warn(nfe);
        }

        return(lastId);
    }

    protected void setLastId(String userid, int lastId)
    {
        Element bookmarkList = getBookmarkList(userid);

        if (bookmarkList == null)
        {
            return;
        }

        XmlUtil.setChildText(bookmarkList, "lastId", Integer.toString(lastId));
    }

    public Element updateBookmark(String userid, FileSysBookmark changedBookmark)
    {
        try{
            this.lock.lock();
            Element bookmarkElement = getBookmarkElement(userid, changedBookmark.getId());

            if (bookmarkElement == null)
            {
                logger.warn("updateBookmark: bookmark for user " + userid + " with id " + changedBookmark.getId() +  " not found");
                return(null);
            }

            XmlUtil.setChildText(bookmarkElement, "name", changedBookmark.getName(), true);
            XmlUtil.setChildText(bookmarkElement, "path", changedBookmark.getPath(), true);
			XmlUtil.setChildText(bookmarkElement, "creationTime", "" + changedBookmark.getCreationTime().getTime());
			XmlUtil.setChildText(bookmarkElement, "updateTime", "" + changedBookmark.getUpdateTime().getTime());

            cacheDirty.put(userid, true);
            
            return(bookmarkElement);
        } finally {
            this.lock.unlock();
        }
    }

    public Element getBookmarkElementByName(String uid, String searchedName)
    {
		Element bookmarkListElement = getBookmarkList(uid);
		
		if (bookmarkListElement == null)
		{
			return(null);
		}

		try
		{
                    this.lock.lock();
			NodeList bookmarks = bookmarkListElement.getElementsByTagName("bookmark");

			if (bookmarks == null)
			{
				return(null);
			}

			int listLength = bookmarks.getLength();

			for (int i = 0; i < listLength; i++)
			{
				Element bookmarkElement = (Element) bookmarks.item(i);
				
				String bookmarkName = XmlUtil.getChildText(bookmarkElement, "name");
				
				if ((bookmarkName != null) && bookmarkName.equals(searchedName))
				{
					return(bookmarkElement);
				}
			}
		} finally {
                    this.lock.unlock();
                }
		
		return(null);
    }
    
    public FileSysBookmark getBookmarkByName(String uid, String searchedName)
    {
    	Element bookmarkElement = getBookmarkElementByName(uid, searchedName);
    	
    	if (bookmarkElement == null)
    	{
    		return(null);
    	}

    	FileSysBookmark newBookmark = new FileSysBookmark(bookmarkElement.getAttribute("id"));

		newBookmark.setName(XmlUtil.getChildText(bookmarkElement, "name"));

		newBookmark.setPath(XmlUtil.getChildText(bookmarkElement, "path"));

		long creationTime = 0L;
                
		String timeString = XmlUtil.getChildText(bookmarkElement, "creationTime");
                
		try
		{
			creationTime=Long.parseLong(timeString);
		}
		catch (NumberFormatException nfe)
		{
			logger.warn(nfe);
			creationTime=(new Date()).getTime();
		}

		newBookmark.setCreationTime(new Date(creationTime));

		long updateTime=0L;
		timeString = XmlUtil.getChildText(bookmarkElement, "updateTime");
		try
		{
			updateTime=Long.parseLong(timeString);
		}
		catch (NumberFormatException nfe)
		{
			logger.warn(nfe);
			updateTime=(new Date()).getTime();
		}

		newBookmark.setUpdateTime(new Date(updateTime));
		
		return(newBookmark);
    }

    public void removeBookmark(String userid, String searchedId)
    {
        Element bookmarkListElement = getBookmarkList(userid);

        try
        {
            this.lock.lock();

            Element bookmarkElement = getBookmarkElement(userid, searchedId);

            if (bookmarkElement == null)
            {
                logger.warn("bookmark for user " + userid + " id " + searchedId + " not found");
                return;
            }

            Node bookmarkList = bookmarkElement.getParentNode();

            if (bookmarkList!=null)
            {
                HashMap<String, Element> userIndex = indexTable.get(userid);
                userIndex.remove(bookmarkElement.getAttribute("id"));
                
                bookmarkList.removeChild(bookmarkElement);

                cacheDirty.put(userid, true);
            }
        } finally {
            this.lock.unlock();
        }

    }

    protected  void saveToFile(String userid)
    {
        try{
            this.lock.lock();
        Element bookmarkListElement = getBookmarkList(userid);

        if (bookmarkListElement == null)
        {
            logger.warn("bookmark list for user " + userid + " does not exist");
            return;
        }

        logger.debug("saving bookmarks for user " + userid);
        
            String xmlFileName = bookmarkPath + File.separator + userid + ".xml";

            try (FileOutputStream fos = new FileOutputStream(xmlFileName);
                    OutputStreamWriter xmlOutFile = new OutputStreamWriter(fos, "UTF-8"))
            {
                
                XmlUtil.writeToStream(bookmarkListElement, xmlOutFile);
                
                xmlOutFile.flush();
            }
            catch (IOException io1)
            {
                logger.error("error saving bookmark file " + xmlFileName, io1);
            }
        } finally {
                this.lock.unlock();
            }
    }

    public void saveChangedUsers()
    {
        try{
            this.lock.lock();
        Set<String> cacheUserList = cacheDirty.keySet();

        for (String userid : cacheUserList) {

            boolean dirtyFlag = cacheDirty.get(userid);

            if (dirtyFlag)
            {
                saveToFile(userid);
                cacheDirty.put(userid, false);
            }
        }
        } finally {
                this.lock.unlock();
            }
    }

    public void deleteUser(String userid)
    {
        bookmarkTable.remove(userid);
        indexTable.remove(userid);
        
        String bookmarkFileName = bookmarkPath + File.separator + userid + ".xml";
        
        File bookmarkFile = new File(bookmarkFileName);
        
        if (!bookmarkFile.exists() || !bookmarkFile.isFile())
        {
            return;
        }
        
        if (bookmarkFile.delete())
        {
                logger.debug("bookmark file deleted for user " + userid);
        }
        else
        {
            logger.warn("failed to delete bookmark file for user " + userid);
        }
    }
    
	public synchronized void run()
	{
		boolean stop = false;

		while (!stop)
		{
			try
			{
				this.wait(120000);

				saveChangedUsers();
			}
			catch (InterruptedException e)
			{
				saveChangedUsers();
				
				stop = true;
			}
		}
	}
	
}