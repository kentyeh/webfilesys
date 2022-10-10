package de.webfilesys.stats;

import java.io.File;
import java.util.ArrayList;

public class DirStatsByAge {

	private ArrayList<AgeCategory> ageCategories = null;
	
	private long filesInTree = 0L;
	
	private long treeFileSize = 0L;
	
    private long fileNumCategoryMax = 0L;

    private long sizeSumCategoryMax = 0L;
	
	public DirStatsByAge() 
	{
		ageCategories = new ArrayList();
		filesInTree = 0L;
		treeFileSize = 0L;
        fileNumCategoryMax = 0L;
        sizeSumCategoryMax = 0L;
	}

	public void addAgeCategory(AgeCategory newCategory) 
	{
	    ageCategories.add(newCategory);
	}
	
	public void determineStatistics(String rootPath)
	{
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
                        addToStats(fileList1.lastModified(), fileList1.length());
                        filesInTree++;
                        treeFileSize += fileList1.length();
                    }
                }
            }
	}
	
	private void addToStats(long lastModified, long fileSize) {
		
	    long fileAge = System.currentTimeMillis() - lastModified;
	    
		boolean stop = false;
		
		for (int i = ageCategories.size() - 1; (!stop) && (i >= 0); i--) {
		    AgeCategory ageCat = (AgeCategory) ageCategories.get(i);
			
		    if ((fileAge > ageCat.getAgeInMillis()) || (i == 0))
		    {
				ageCat.addFile(fileSize);
				stop = true;
			}
		}
	}
	
    private void calculatePercentage() {
            for (AgeCategory ageCat : ageCategories) {
                if (filesInTree == 0) {
                    ageCat.setFileNumPercent(0);
                } else {
                    ageCat.setFileNumPercent((int) (ageCat.getFileNum() * 100L / filesInTree));
                }   ageCat.setSizePercent(treeFileSize);
                if (ageCat.getFileNum() > fileNumCategoryMax) {
                    fileNumCategoryMax = ageCat.getFileNum();   
                }   if (ageCat.getSizeSum() > sizeSumCategoryMax) {
                    sizeSumCategoryMax = ageCat.getSizeSum();   
            }
            }
    }
    
    public long getFileNumCategoryMax() {
        return fileNumCategoryMax;
    }

    public long getSizeSumCategoryMax() {
        return sizeSumCategoryMax;
    }
	
	/**
	 * @return List of SizeCategory objects.
	 */
	public ArrayList getResults()
	{
	    return ageCategories;
	}
	
}
