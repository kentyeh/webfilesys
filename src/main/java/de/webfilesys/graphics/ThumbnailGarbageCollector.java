package de.webfilesys.graphics;
import java.io.File;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThumbnailGarbageCollector extends Thread
{
    private static final Logger logger = LogManager.getLogger(ThumbnailGarbageCollector.class);
    public final static String imgFileMasks[]={"*"};

    String path;

    int removedCount=0;

    public ThumbnailGarbageCollector(String path)
    {
        this.path=path;
    }
 
    @Override
    public synchronized void run()
    {
        setPriority(1);
        
        removeThumbnailGarbage();
    }

    protected void removeThumbnailGarbage()
    {
        logger.debug("thumbnail garbage collector started for dir " + path);
        long startTime=(new Date()).getTime();

        removedCount=0;

        exploreTree(path);

        long endTime=(new Date()).getTime();

		logger.debug("thumbnail garbage collection ended for dir " + path + " (" + (endTime-startTime) + " ms) - " + removedCount + " thumbnails removed.");
    }

    protected void exploreTree(String path)
    {
        String pathWithSlash=path;

        if (!path.endsWith(File.separator))
        {
            pathWithSlash=path + File.separator;
        }

        File dirFile=new File(path);
        
        String fileList[]=dirFile.list();

        if (fileList==null)
        {
            return;
        }

        for (String subdirName : fileList) {
            String subdirPath=pathWithSlash + subdirName;

            File tempFile=new File(subdirPath);

            if (tempFile.isDirectory() && tempFile.canRead())
            {
                if (subdirName.equals(ThumbnailThread.THUMBNAIL_SUBDIR))
                {
                    removeExpiredThumbnails(subdirPath);
                }                
                else
                {
                    exploreTree(subdirPath);
                }
            }
        }
    }

    protected void removeExpiredThumbnails(String thumbnailPath)
    {
        File dirFile=new File(thumbnailPath);
        
        String fileList[]=dirFile.list();

        if (fileList==null)
        {
            return;
        }

        for (String fileName : fileList) {
            File tempFile=new File(thumbnailPath,fileName);
            
            if (tempFile.isFile() && tempFile.canWrite())
            {
                String imgPath=thumbnailPath.substring(0,thumbnailPath.lastIndexOf(File.separatorChar)+1);
                
                String imgFileName=imgPath + fileName;
                
                File imgFile=new File(imgFileName);

                if ((!imgFile.exists()) || (!imgFile.isFile()))
                {
                    if (!tempFile.delete())
                    {
                        logger.warn("cannot remove thumbnail garbage file " + tempFile);
                    }
                    else
                    {
                        removedCount++;

                        logger.debug("removed thumbnail garbage file: " + tempFile);
                    }
                }
            }
        }

        fileList=dirFile.list();

        if ((fileList!=null) && (fileList.length==0))
        {
            if (dirFile.delete())
            {
    			logger.debug("removed empty thumbnail dir " + thumbnailPath);
            }
            else
            {
			logger.warn("cannot delete empty thumbnail dir " + thumbnailPath);
            }
        }
    }

}
