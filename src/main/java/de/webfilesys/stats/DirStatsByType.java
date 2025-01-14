package de.webfilesys.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class DirStatsByType {
	
	private HashMap<String,TypeCategory> typeMap = null;
	
	private long filesInTree = 0L;
	
	private long treeFileSize = 0L;
	
    private long fileNumCategoryMax = 0L;

    private long sizeSumCategoryMax = 0L;
	
	public DirStatsByType(String rootPath) 
	{
		typeMap = new HashMap<>();
		
	    fileNumCategoryMax = 0L;

	    sizeSumCategoryMax = 0L;

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
                        String fileExt = getFileExt(fileList1.getName());
                        addToStats(fileExt.toLowerCase(), fileList1.length());
                        filesInTree++;
                        treeFileSize += fileList1.length();
                    }
                }
            }
	}
	
	private String getFileExt(String fileName) {
		int lastSepIdx = fileName.lastIndexOf('.');
		
		if ((lastSepIdx < 0) || (lastSepIdx == fileName.length() - 1)) {
			return "";
		}
		
		return(fileName.substring(lastSepIdx + 1));
	}
	
	private void addToStats(String fileExt, long fileSize) {
		TypeCategory typeCat = typeMap.get(fileExt);
		
		if (typeCat == null) {
			typeCat = new TypeCategory(fileExt);
			typeMap.put(fileExt, typeCat);
		}
		
		typeCat.addFile(fileSize);
	}
	
	private void calculatePercentage() {
		Iterator<TypeCategory> iter = typeMap.values().iterator();
		
		while (iter.hasNext()) {
            TypeCategory typeCat = iter.next();		
            typeCat.setFileNumPercent((int) (typeCat.getFileNum() * 100L / filesInTree));
			typeCat.setSizePercent(treeFileSize);
			
            if (typeCat.getFileNum() > fileNumCategoryMax) {
                fileNumCategoryMax = typeCat.getFileNum();   
            }
            if (typeCat.getSizeSum() > sizeSumCategoryMax) {
                sizeSumCategoryMax = typeCat.getSizeSum();   
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
     * @return List of TypeCategory objects.
     */
    public ArrayList getResults()
    {
		ArrayList sortList = new ArrayList();
        sortList.addAll(typeMap.values());		
        
        Collections.sort(sortList, new TypeCategoryComparator(TypeCategoryComparator.SORT_BY_FILE_NUM));

        return sortList;
	}
	
}
