package de.webfilesys;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import de.webfilesys.graphics.ThumbnailThread;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SubdirExistCache {
	
    private ConcurrentHashMap<String, Integer> subdirExists = null;
    private final ReentrantLock lock = new ReentrantLock();

    private static SubdirExistCache instance = null;
    
    private SubdirExistCache() 
    {
    	subdirExists = new ConcurrentHashMap<>(100);
    }
    
    public static SubdirExistCache getInstance() 
    {
    	if (instance == null)
    	{
    		instance = new SubdirExistCache();
    	}
    	return instance;
    }
    
    /**
     * Does a (sub)folder with the given path exist?
     * @param path filesystem path of the folder
     * @return null if not known, Integer(1) if folder exists, Integer(0) if folder does NOT exist
     */
    public Integer existsSubdir(String path)
    {
        return(subdirExists.get(path));	
    }
    
    /**
     * Are there any subfolders in the folder with the given path?
     * @param path filesystem path of the folder
     * @param newVal 1 if subfolders exist, 0 if NO subfolders exist
     */
    public void setExistsSubdir(String path, Integer newVal)
    {
    	    subdirExists.put(path, newVal);
    }
    
    /**
     * Remove a folder and all subfolders from the subdir exist cache.
     * @param path the root of the folder tree
     */
    public void cleanupExistSubdir(String path)
    {
    	try {
            this.lock.lock();
    		Iterator<String> keyIter = subdirExists.keySet().iterator();
    		
    		ArrayList<String> keysToRemove = new ArrayList<>();
    		
    		while (keyIter.hasNext())
    		{
    			String key = keyIter.next();
    			
                if (key.startsWith(path))
                {
                	if (key.equals(path) || 
                		(key.charAt(path.length()) == '/') ||
                		(key.charAt(path.length()) == File.separatorChar))
            		{
                         keysToRemove.add(key);                		
            		}
                }
    		}
    		
    		for (int i = keysToRemove.size() - 1; i >= 0; i--)
    		{
    			subdirExists.remove(keysToRemove.get(i));
    		}
    	} finally {
            this.lock.unlock();
        }
    }
    
    public void initialReadSubdirs(int operatingSystemType)
    {
        String rootDirPath;
        if ((operatingSystemType == WebFileSys.OS_OS2) || (operatingSystemType == WebFileSys.OS_WIN))
        {
            rootDirPath = "C:\\";
        }
        else
        {
            rootDirPath = "/";
        }

        File rootDir = new File(rootDirPath);

        File[] rootFileList = rootDir.listFiles();
        if (rootFileList != null)
        {
        	try {
                    this.lock.lock();
                    for (File tempFile : rootFileList) {
                        if (tempFile.isDirectory()) 
                        {
                            File subDir = tempFile; 
                            File[] subFileList = subDir.listFiles();
                            
                            boolean hasSubdirs = false;
                            if (subFileList != null)
                            {
                                for (int k = 0; (!hasSubdirs) && (k < subFileList.length); k++)
                                {
                                    if (subFileList[k].isDirectory())
                                    {
                                        if (!subFileList[k].getName().equals(ThumbnailThread.THUMBNAIL_SUBDIR))
                                        {
                                            hasSubdirs = true;
                                        }
                                    }
                                }
                            }
                            if (hasSubdirs)
                            {
                                setExistsSubdir(subDir.getAbsolutePath(), 1);
                            }
                            else
                            {
                                setExistsSubdir(subDir.getAbsolutePath(), 0);
                            }
                        }
                    }
        	} finally {
            this.lock.unlock();
        }
        }
    }
}
