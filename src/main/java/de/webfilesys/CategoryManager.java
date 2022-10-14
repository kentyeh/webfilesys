package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
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

public class CategoryManager extends Thread {

    private static final Logger logger = LogManager.getLogger(CategoryManager.class);

    public static final String CATEGORIES_DIR = "categories";

    ConcurrentHashMap<String, Element> categoryTable = null;

    ConcurrentHashMap<String, ConcurrentHashMap<String, Element>> indexTable = null;

    ConcurrentHashMap<String, Boolean> cacheDirty = null;

    DocumentBuilder builder = null;

    String categoryFileName = null;

    boolean shutdownFlag = false;

    private static CategoryManager categoryManager = null;

    private String categoryPath = null;
    
    private  final ReentrantLock lockSelf = new ReentrantLock();
    private  final ReentrantLock lockElementList = new ReentrantLock();

    private CategoryManager() {
        categoryPath = WebFileSys.getInstance().getConfigBaseDir() + "/" + CATEGORIES_DIR;

        categoryTable = new ConcurrentHashMap<>();

        indexTable = new ConcurrentHashMap<>();

        cacheDirty = new ConcurrentHashMap<>();

        shutdownFlag = false;

        builder = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pcex) {
        }

        this.start();
    }

    public static CategoryManager getInstance() {
        if (categoryManager == null) {
            categoryManager = new CategoryManager();
        }

        return (categoryManager);
    }

    public Element getCategoryList(String userid) {
        Element categoryList = categoryTable.get(userid);

        if (categoryList != null) {
            return (categoryList);
        }

        categoryFileName = categoryPath + File.separator + userid + ".xml";

        File categoryFile = new File(categoryFileName);

        if (categoryFile.exists() && categoryFile.isFile()) {
            if (!categoryFile.canRead()) {
                logger.error("cannot read categories file for user {}" ,userid);
                return (null);
            }

            categoryList = readCategoryList(categoryFile.getAbsolutePath());

            if (categoryList != null) {
                categoryTable.put(userid, categoryList);
                createIndex(categoryList, userid);

                return (categoryList);
            }
        }

        return (null);
    }

    Element readCategoryList(String categoryFilePath) {
        File categoryFile = new File(categoryFilePath);

        if ((!categoryFile.exists()) || (!categoryFile.canRead())) {
            return (null);
        }

        Document doc = null;

        try (FileInputStream fis = new FileInputStream(categoryFile)){

            InputSource inputSource = new InputSource(fis);

            inputSource.setEncoding("UTF-8");

            logger.debug("reading categories from {}" , categoryFilePath);

            doc = builder.parse(inputSource);
        } catch (SAXException | IOException saxex) {
            logger.error("failed to load category file : " + categoryFilePath, saxex);
        }

        return (doc.getDocumentElement());
    }

    protected void createIndex(Element categoryList, String userid) {
        NodeList categories = categoryList.getElementsByTagName("category");

        if (categories == null) {
            indexTable.remove(userid);
            return;
        }

        int listLength = categories.getLength();

        ConcurrentHashMap<String, Element> userIndex = new ConcurrentHashMap<>();

        for (int i = 0; i < listLength; i++) {
            Element category = (Element) categories.item(i);

            String categoryId = category.getAttribute("id");

            if (categoryId != null) {
                userIndex.put(categoryId, category);
            }
        }

        indexTable.put(userid, userIndex);
    }

    public void disposeCategoryList(String userid) {
        Boolean dirtyFlag = cacheDirty.get(userid);

        if ((dirtyFlag != null) && dirtyFlag) {
            saveToFile(userid);
        }

        if (categoryTable.get(userid) != null) {
            logger.debug("disposing category list of user {}" , userid);
        }

        categoryTable.remove(userid);
        indexTable.remove(userid);
    }

    public void disposeAllCategories() {
        saveChangedUsers();

        categoryTable = new ConcurrentHashMap<>();
        indexTable = new ConcurrentHashMap<>();
    }

    public ArrayList<String> getCategoryIds(String userid) {
        Element categoryList = getCategoryList(userid);

        ArrayList<String> categoryIds = null;

        if (categoryList == null) {
            return (null);
        }

        NodeList categories = categoryList.getElementsByTagName("category");

        if (categories != null) {
            int listLength = categories.getLength();

            for (int i = 0; i < listLength; i++) {
                Element category = (Element) categories.item(i);

                if (categoryIds == null) {
                    categoryIds = new ArrayList<>();
                }

                categoryIds.add(category.getAttribute("id"));
            }
        } else {
            logger.debug("no categories found for userid {}" , userid);
        }

        return categoryIds;
    }

    public ArrayList<Category> getListOfCategories(String userid) {
        Element categoryList = getCategoryList(userid);

        ArrayList<Category> listOfCategories = new ArrayList<>();

        if (categoryList == null) {
            logger.debug("category list for user {} does not exist!",userid);
            return listOfCategories;
        }

        NodeList categories = categoryList.getElementsByTagName("category");

        if (categories != null) {
            int listLength = categories.getLength();

            for (int i = 0; i < listLength; i++) {
                Element category = (Element) categories.item(i);

                Category newCategory = new Category(category.getAttribute("id"));

                newCategory.setName(XmlUtil.getChildText(category, "name"));

                long creationTime = 0L;

                String timeString = XmlUtil.getChildText(category, "creationTime");

                try {
                    creationTime = Long.parseLong(timeString);
                } catch (NumberFormatException nfe) {
                    logger.warn(nfe);
                    creationTime = (new Date()).getTime();
                }

                newCategory.setCreationTime(new Date(creationTime));

                long updateTime = 0L;
                timeString = XmlUtil.getChildText(category, "updateTime");
                try {
                    updateTime = Long.parseLong(timeString);
                } catch (NumberFormatException nfe) {
                    logger.warn(nfe);
                    updateTime = (new Date()).getTime();
                }

                newCategory.setUpdateTime(new Date(updateTime));

                listOfCategories.add(newCategory);
            }
        }

        Collections.sort(listOfCategories, new CategoryComparator());

        return listOfCategories;
    }

    public Category getCategory(String userid, String searchedId) {
        Element category = getCategoryElement(userid, searchedId);

        if (category == null) {
            logger.warn("category for user " + userid + "id " + searchedId + " does not exist!");
            return (null);
        }

        Category foundCategory = new Category(category.getAttribute("id"));

        foundCategory.setName(XmlUtil.getChildText(category, "name"));

        long creationTime = 0L;
        String timeString = XmlUtil.getChildText(category, "creationTime");
        try {
            creationTime = Long.parseLong(timeString);
        } catch (NumberFormatException nfe) {
            logger.error(nfe);
            creationTime = (new Date()).getTime();
        }

        foundCategory.setCreationTime(new Date(creationTime));

        long updateTime = 0L;
        timeString = XmlUtil.getChildText(category, "updateTime");
        try {
            updateTime = Long.parseLong(timeString);
        } catch (NumberFormatException nfe) {
            logger.error(nfe);
            updateTime = (new Date()).getTime();
        }

        foundCategory.setUpdateTime(new Date(updateTime));

        return (foundCategory);
    }

    protected Element getCategoryElement(String userid, String searchedId) {
        Element categoryList = getCategoryList(userid);

        if (categoryList == null) {
            return (null);
        }

        Element category = null;

        ConcurrentHashMap<String, Element> userIndex = indexTable.get(userid);

        if (userIndex != null) {
            category = (Element) userIndex.get(searchedId);

            if (category != null) {
                return (category);
            }
        }

        logger.warn("category with id " + searchedId + " not found in index");

        NodeList categories = categoryList.getElementsByTagName("category");

        if (categories == null) {
            return (null);
        }

        int listLength = categories.getLength();

        for (int i = 0; i < listLength; i++) {
            category = (Element) categories.item(i);

            if (category.getAttribute("id").equals(searchedId)) {
                return (category);
            }
        }

        return (null);
    }

    protected Element createCategoryList(String userid) {
        logger.debug("creating new category list for user : " + userid);

        Document doc = builder.newDocument();

        Element categoryListElement = doc.createElement("categoryList");

        Element lastIdElement = doc.createElement("lastId");
        XmlUtil.setElementText(lastIdElement, "0");

        categoryListElement.appendChild(lastIdElement);

        doc.appendChild(categoryListElement);

        categoryTable.put(userid, categoryListElement);

        indexTable.put(userid, new ConcurrentHashMap<>());

        return (categoryListElement);
    }

    public Element createCategory(String userid, Category newCategory) {
        Element categoryList = getCategoryList(userid);

        if (categoryList == null) {
            categoryList = createCategoryList(userid);
        }

        Element newElement = null;

        try {
            this.lockSelf.lock();
            Document doc = categoryList.getOwnerDocument();

            newElement = doc.createElement("category");

            newElement.appendChild(doc.createElement("name"));
            newElement.appendChild(doc.createElement("creationTime"));
            newElement.appendChild(doc.createElement("updateTime"));

            categoryList.appendChild(newElement);

            int lastId = getLastId(userid);

            lastId++;

            setLastId(userid, lastId);

            String newIdString = "" + lastId;

            newCategory.setId(newIdString);
            newElement.setAttribute("id", newIdString);

            ConcurrentHashMap<String, Element> userIndex = indexTable.get(userid);
            userIndex.put(newIdString, newElement);
        } finally {
            this.lockSelf.unlock();
        }

        updateCategory(userid, newCategory);

        return (newElement);
    }

    protected int getLastId(String userid) {
        Element categoryList = getCategoryList(userid);

        if (categoryList == null) {
            return (-1);
        }

        String lastIdString = XmlUtil.getChildText(categoryList, "lastId").trim();

        int lastId = 0;
        try {
            lastId = Integer.parseInt(lastIdString);
        } catch (NumberFormatException nfe) {
            logger.warn(nfe);
        }

        return (lastId);
    }

    protected void setLastId(String userid, int lastId) {
        Element categoryList = getCategoryList(userid);

        if (categoryList == null) {
            return;
        }

        XmlUtil.setChildText(categoryList, "lastId", Integer.toString(lastId));
    }

    public Element updateCategory(String userid, Category changedCategory) {

        try {
            this.lockSelf.lock();
            Element categoryElement = getCategoryElement(userid, changedCategory.getId());

            if (categoryElement == null) {
                logger.warn("updateCategory: category for user " + userid + " with id " + changedCategory.getId() + " not found");
                return (null);
            }

            XmlUtil.setChildText(categoryElement, "name", changedCategory.getName(), true);
            XmlUtil.setChildText(categoryElement, "creationTime", "" + changedCategory.getCreationTime().getTime());
            XmlUtil.setChildText(categoryElement, "updateTime", "" + changedCategory.getUpdateTime().getTime());

            cacheDirty.put(userid, true);

            return (categoryElement);
        } finally {
            this.lockSelf.unlock();
        }
    }

    public Element getCategoryElementByName(String uid, String searchedName) {
        Element categoryListElement = getCategoryList(uid);

        if (categoryListElement == null) {
            return (null);
        }

        try {
            this.lockElementList.lock();
            NodeList categories = categoryListElement.getElementsByTagName("category");

            if (categories == null) {
                return (null);
            }

            int listLength = categories.getLength();

            for (int i = 0; i < listLength; i++) {
                Element categoryElement = (Element) categories.item(i);

                String catName = XmlUtil.getChildText(categoryElement, "name");

                if ((catName != null) && catName.equals(searchedName)) {
                    return (categoryElement);
                }
            }
        } finally {
            this.lockElementList.unlock();
        }

        return (null);
    }

    public Category getCategoryByName(String uid, String searchedName) {
        Element categoryElement = getCategoryElementByName(uid, searchedName);

        if (categoryElement == null) {
            return (null);
        }

        Category newCategory = new Category(categoryElement.getAttribute("id"));

        newCategory.setName(XmlUtil.getChildText(categoryElement, "name"));

        long creationTime = 0L;

        String timeString = XmlUtil.getChildText(categoryElement, "creationTime");

        try {
            creationTime = Long.parseLong(timeString);
        } catch (NumberFormatException nfe) {
            logger.warn(nfe);
            creationTime = (new Date()).getTime();
        }

        newCategory.setCreationTime(new Date(creationTime));

        long updateTime = 0L;
        timeString = XmlUtil.getChildText(categoryElement, "updateTime");
        try {
            updateTime = Long.parseLong(timeString);
        } catch (NumberFormatException nfe) {
            logger.warn(nfe);
            updateTime = (new Date()).getTime();
        }

        newCategory.setUpdateTime(new Date(updateTime));

        return (newCategory);
    }

    public void removeCategory(String userid, String searchedId) {
        Element categoryListElement = getCategoryList(userid);

        try {
            this.lockElementList.lock();
            Element categoryElement = getCategoryElement(userid, searchedId);

            if (categoryElement == null) {
                logger.warn("category for user " + userid + " id " + searchedId + " not found");
                return;
            }

            Node categoryList = categoryElement.getParentNode();

            if (categoryList != null) {
                ConcurrentHashMap<String, Element> userIndex = indexTable.get(userid);
                userIndex.remove(categoryElement.getAttribute("id"));

                categoryList.removeChild(categoryElement);

                cacheDirty.put(userid, true);
            }
        } finally {
            this.lockElementList.unlock();
        }

    }

    protected void saveToFile(String userid) {
        Element categoryListElement = getCategoryList(userid);

        if (categoryListElement == null) {
            logger.warn("category list for user " + userid + " does not exist");
            return;
        }

        logger.debug("saving categories for user " + userid);

        try {
            this.lockElementList.lock();
            String xmlFileName = categoryPath + File.separator + userid + ".xml";

            try (FileOutputStream fos = new FileOutputStream(xmlFileName);
                    OutputStreamWriter xmlOutFile = new OutputStreamWriter(fos, "UTF-8")){

                XmlUtil.writeToStream(categoryListElement, xmlOutFile);

                xmlOutFile.flush();
            } catch (IOException io1) {
                logger.error("error saving category file " + xmlFileName, io1);
            }
        } finally {
            this.lockElementList.unlock();
        }
    }

    public void saveChangedUsers() {
        try{
            this.lockSelf.lock();
        Enumeration cacheUserList = cacheDirty.keys();

        while (cacheUserList.hasMoreElements()) {
            String userid = (String) cacheUserList.nextElement();

            boolean dirtyFlag = cacheDirty.get(userid);

            if (dirtyFlag) {
                saveToFile(userid);
                cacheDirty.put(userid, false);
            }
        }
        }finally{
            this.lockSelf.unlock();
        }
    }

    @Override
    public void run() {
        try{
            this.lockSelf.lock();
        boolean stop = false;

        while (!stop) {
            try {
                this.wait(120000);

                saveChangedUsers();
            } catch (InterruptedException e) {
                saveChangedUsers();

                stop = true;
            }
        }
        } finally {
            this.lockSelf.unlock();
        }
    }

}
