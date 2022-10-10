package de.webfilesys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastPathManager extends Thread
{
    private static final Logger logger = LogManager.getLogger(FastPathManager.class);

    private static FastPathManager fastPathMgr=null;

    HashMap<String, FastPathQueue> queueTable = null;

    HashMap<String, Boolean> cacheModified = null;

    private FastPathManager()
    {
        queueTable = new HashMap<>(5);

        cacheModified = new HashMap<>(5);

        this.start();
    }

    public static synchronized FastPathManager getInstance()
    {
        if (fastPathMgr == null)
        {
            fastPathMgr = new FastPathManager();
        }

        return(fastPathMgr);
    }

    public synchronized void queuePath(String userid, String pathName)
    {
        FastPathQueue userQueue = queueTable.get(userid);

        if (userQueue==null)
        {
            userQueue=new FastPathQueue(userid);

            queueTable.put(userid, userQueue);
        }

        userQueue.queuePath(pathName);

        cacheModified.put(userid, true);
    }

    /**
     * Remove a (renamed) folder tree and all it's subfolders from the FastPath queue.
     * @param userid the userir
     * @param path path to remove
     */
    public void removeTree(String userid, String path) {
        FastPathQueue userQueue = (FastPathQueue) queueTable.get(userid);

        if (userQueue == null)
        {
            return;
        }
        
        ArrayList<String> fastPathList = userQueue.getPathList();        

        for (int i = fastPathList.size() - 1; i >= 0; i--) 
        {
            String fastPath = (String) fastPathList.get(i);
            
            if (fastPath.startsWith(path))
            {
                if (fastPath.equals(path) || 
                    (fastPath.charAt(path.length()) == '/') ||
                    (fastPath.charAt(path.length()) == File.separatorChar))
                {
                    fastPathList.remove(i);                     
                }
            }
        }
        
        cacheModified.put(userid, true);
    }
    
    public ArrayList<String> getPathList(String userid)
    {
        FastPathQueue userQueue = queueTable.get(userid);

        if (userQueue == null)
        {
            return new ArrayList<>();
        }

        return userQueue.getPathList();
    }
    
    public String returnToPreviousDir(String userid)
    {
        FastPathQueue userQueue = queueTable.get(userid);

        if (userQueue == null)
        {
            return null;
        }

        ArrayList<String> fastPathList = userQueue.getPathList();
        
        if ((fastPathList == null) || (fastPathList.isEmpty()))
        {
            return null;
        }
        
        if (fastPathList.size() == 1)
        {
            return (String) fastPathList.get(0);
        }
        
        String lastVisitedDir = (String) fastPathList.get(1);
        
        fastPathList.remove(0);

        cacheModified.put(userid, true);
        
        return lastVisitedDir;
    }

    protected void saveChangedUsers()
    {
        for (String userid : cacheModified.keySet()) {

            Boolean modified = cacheModified.get(userid);

            if (modified)
            {
                FastPathQueue userQueue = queueTable.get(userid);

                userQueue.saveToFile();
                
                cacheModified.put(userid, false);
            }
        }
    }

    public void deleteUser(String userid)
    {
        queueTable.remove(userid);
        cacheModified.remove(userid);
        
        String fastPathFileName = WebFileSys.getInstance().getConfigBaseDir() + "/" + FastPathQueue.FAST_PATH_DIR + "/" + userid + ".dat";
        
        File fastPathFile = new File(fastPathFileName);
        
        if (!fastPathFile.exists() || !fastPathFile.isFile())
        {
            return;
        }
        
        if (!fastPathFile.delete()) {
            logger.warn("failed to delete fastpath file for user " + userid);
        }
    }
    
    @Override
    public synchronized void run()
    {
       	logger.debug("FastPathManager started");
    	
    	boolean stop = false;
    	
        while (!stop)
        {
            try
            {
                this.wait(60000);
            }
            catch (InterruptedException e)
            {
                stop = true;
            }

            saveChangedUsers();
            
            if (stop)
            {
                logger.debug("FastPathManager ready for shutdown");
            }
        }
    }

}
