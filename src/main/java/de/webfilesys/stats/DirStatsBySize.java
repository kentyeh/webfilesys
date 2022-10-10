package de.webfilesys.stats;

import java.io.File;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirStatsBySize {
    private static final Logger logger = LogManager.getLogger(DirStatsBySize.class);
	
	private static final long SIZE_MAX = 4L * 1024L * 1024L * 1024L;

	private static final long KB = 1024;
	private static final long MB = 1024 * KB;
	private static final long GB = 1024 * MB;
	
	private static final int SIZE_STEP_FACTOR = 4;
	
	private ArrayList<SizeCategory> sizeCategories = null;
	
	private long filesInTree = 0L;
	
	private long treeFileSize = 0L;
	
	public DirStatsBySize(String rootPath) 
	{
		sizeCategories = new ArrayList<>();
		
		long minSize = 0;
		long maxSize = 1024;
		
		for (int i = 0; maxSize <= SIZE_MAX; i++) 
		{
			sizeCategories.add(new SizeCategory(minSize, maxSize));
			minSize = maxSize + 1;
			maxSize = maxSize * SIZE_STEP_FACTOR;
		}
		
		filesInTree = 0L;
		
		walkThroughFolderTree(rootPath);
		
		calculatePercentage();
	}

	private void walkThroughFolderTree(String path)
	{
		File dirFile = new File(path);
		
		if ((!dirFile.exists()) ||(!dirFile.isDirectory()) || (!dirFile.canRead())) {
			return;
		}
		
		File fileList[] = dirFile.listFiles();
		
		if (fileList == null) {
			return;
		}
		
            for (File fileList1 : fileList) {
                if (fileList1.isDirectory()) {
                    walkThroughFolderTree(fileList1.getAbsolutePath());
                } else {
                    if (fileList1.isFile()) {
                        addToStats(fileList1.length());
                        filesInTree++;
                        treeFileSize += fileList1.length();
                    }
                }
            }
	}
	
	private void addToStats(long fileSize) {
		
		boolean stop = false;
		
		for (int i = 0; (!stop) && (i < sizeCategories.size()); i++) {
			SizeCategory sizeCat = (SizeCategory) sizeCategories.get(i);
			
			if ((fileSize >= sizeCat.getMinSize()) && (fileSize <= sizeCat.getMaxSize())) {
				sizeCat.addFile(fileSize);
				stop = true;
			}
		}
	}
	
	private void calculatePercentage() {
            for (SizeCategory sizeCat : sizeCategories) {
                if (filesInTree == 0) {
                    sizeCat.setFileNumPercent(0);
                } else {
                    sizeCat.setFileNumPercent((int) (sizeCat.getFileNum() * 100L / filesInTree));
                }
                sizeCat.setSizePercent(treeFileSize);
            }
	}
	
	/**
	 * @return List of SizeCategory objects.
	 */
	public ArrayList getResults()
	{
	    return sizeCategories;
	}
	
	public long getFileNumCategoryMax() {
        long fileNumMax = 0L;
        
            for (SizeCategory sizeCat : sizeCategories) {
                if (sizeCat.getFileNum() > fileNumMax) {
                    fileNumMax = sizeCat.getFileNum();
                }
            }
	    
	    return fileNumMax;
	}

    public long getSizeSumCategoryMax() {
        long sizeSumMax = 0L;
        
            for (SizeCategory sizeCat : sizeCategories) {
                if (sizeCat.getSizeSum() > sizeSumMax) {
                    sizeSumMax = sizeCat.getSizeSum();
                }
            }
        
        return sizeSumMax;
    }
	
	private void showResults() 
	{
            for (SizeCategory sizeCat : sizeCategories) {
                logger.info(formatSizeForDisplay(sizeCat.getMinSize()) + " - " + formatSizeForDisplay(sizeCat.getMaxSize()) + " : " + sizeCat.getFileNum() + " (" + sizeCat.getFileNumPercent() + "% / " + sizeCat.getSizePercent() + "%)");
            }
	}
	
	private String formatSizeForDisplay(long sizeVal) 
	{
		StringBuilder formattedSize = new StringBuilder();
		
		long formatVal = sizeVal;
		
		if (formatVal >= GB) 
		{
			formattedSize.append(formatVal / GB);
			formattedSize.append(".");
			formatVal = formatVal % GB;
			formattedSize.append(formatVal / MB);
			formattedSize.append(" GB");
		} 
		else 
		{
			if (formatVal >= MB) 
			{
				formattedSize.append(formatVal / MB);
				formattedSize.append(".");
				formatVal = formatVal % MB;
				formattedSize.append(formatVal / KB);
				formattedSize.append(" MB");
			} 
			else 
			{
				if (formatVal >= KB) 
				{
					formattedSize.append(formatVal / KB);
					formattedSize.append(".");
					formatVal = formatVal % KB;
					formattedSize.append(formatVal);
					formattedSize.append(" KB");
				} 
				else
				{
					formattedSize.append(formatVal);
					formattedSize.append(" Byte");
				}
			}
		}
		
		return formattedSize.toString();
	}
}
