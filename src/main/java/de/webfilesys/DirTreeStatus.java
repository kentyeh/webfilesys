package de.webfilesys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class DirTreeStatus 
{
    private HashMap<String, Boolean> expandedDirs = null;
    
    private HashMap<String, Long> subdirNameLengthSumMap = null;

    public DirTreeStatus()
    {
    	expandedDirs = new HashMap<>();
    	subdirNameLengthSumMap = new HashMap<>();
    }
    
    public void expandDir(String path)
    {
        expandedDirs.put(path, true); 
    }

    public void collapseDir(String path)
    {
        expandedDirs.remove(path);

        ArrayList<String> collapsedSubdirs = new ArrayList<>();

        for (String dirName : expandedDirs.keySet()) {
            if (dirName.indexOf(path) == 0) {
                collapsedSubdirs.add(dirName);
            }
        }

        for (String collapsedSubdir : collapsedSubdirs) {
            expandedDirs.remove(collapsedSubdir);
        }
    }

    public void collapseAll()
    {
        expandedDirs.clear();
    }

    public void expandPath(String path)
    {
        StringTokenizer pathParser=new StringTokenizer(path,File.separator);

        StringBuilder partOfPath=new StringBuilder();
        
        boolean firstToken=true;
        
        if (File.separatorChar=='/')
        {
            expandedDirs.put("/", true); 
            firstToken=false;
        }

        while (pathParser.hasMoreTokens())
        {
            String dirName=pathParser.nextToken();

            if (firstToken)
            {
                partOfPath.append(dirName);
                expandedDirs.put(partOfPath.toString() + File.separator, true); 
                firstToken=false;
            }
            else
            {
                partOfPath.append(File.separatorChar);
                partOfPath.append(dirName);
                expandedDirs.put(partOfPath.toString(), true); 
            }
        }
    }

    public ArrayList<String> getExpandedFolders() {
    	ArrayList<String> expandedFolders = new ArrayList<>();
    	
    	for (String path : expandedDirs.keySet()) {
    		Boolean expanded = expandedDirs.get(path);
    		if ((expanded != null) && expanded) {
    			expandedFolders.add(path);
    		}
    	}
    	return expandedFolders;
    }
    
    public boolean dirExpanded(String path)
    {
        return(expandedDirs.get(path)!=null);
    }
    
    public void setSubdirNameLengthSum(String path, long nameLengthSum) {
    	subdirNameLengthSumMap.put(path, nameLengthSum);
    }
    
    public HashMap<String, Long> getSubdirNameLengthSumMap() {
    	return subdirNameLengthSumMap;
    }
    
    public long getSubdirNameLenghtSum(String path) {
    	Long nameLengthSum = subdirNameLengthSumMap.get(path);
    	if (nameLengthSum == null) {
    		return (-1);
    	}
    	return nameLengthSum;
    }
}
