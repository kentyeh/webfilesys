package de.webfilesys;

import java.io.File;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FileComparator implements Comparator {
    public static final int SORT_BY_FILENAME = 1; 
    public static final int SORT_BY_CASESENSITIVE = 2; 
    public static final int SORT_BY_EXTENSION = 3; 
    public static final int SORT_BY_SIZE = 4; 
    public static final int SORT_BY_DATE = 5; 

    public static final int SORT_FILES_BY_NAME = 5; 
    
    ConcurrentHashMap<String, Long> sizeCache = null;
    ConcurrentHashMap<String, Long> dateCache = null;

    int sortBy;
    String path;

    public FileComparator() {
    	sortBy = SORT_FILES_BY_NAME;
    }
    
    public FileComparator(String path,int sortBy) {
        if (path.endsWith(File.separator)) {
            this.path = path;
        } else {
            this.path = path + File.separator;
        }

        this.sortBy = sortBy;

        if (sortBy == SORT_BY_SIZE) {
            sizeCache = new ConcurrentHashMap<>();
        }

        if (sortBy == SORT_BY_DATE) {
            dateCache = new ConcurrentHashMap<>();
        }
    }

    public int compare(Object o1, Object o2) {
        if (!o2.getClass().equals(o1.getClass())) {
            throw new ClassCastException();
        }
        
        File file1 = null;
        File file2 = null;

        if (sortBy == SORT_FILES_BY_NAME) {
        	file1 = (File) o1;
        	file2 = (File) o2; 
            return(file1.getName().toUpperCase().compareTo(file2.getName().toUpperCase()));
        }
        
        String fileName1 = (String) o1;
        String fileName2 = (String) o2;

        if (sortBy == SORT_BY_FILENAME) {
            return(fileName1.toUpperCase().compareTo(fileName2.toUpperCase()));
        }

        if (sortBy == SORT_BY_CASESENSITIVE) {
            return(fileName1.compareTo(fileName2));
        }

        if (sortBy == SORT_BY_EXTENSION) {
            String ext1="";
            String ext2="";

            int extIdx = fileName1.lastIndexOf(".");
            if (extIdx >= 0) {
                ext1 = fileName1.substring(extIdx);
            }
            
            extIdx = fileName2.lastIndexOf(".");
            if (extIdx >= 0) {
                ext2 = fileName2.substring(extIdx);
            }

            return(ext1.toUpperCase().compareTo(ext2.toUpperCase()));
        }

        if (sortBy == SORT_BY_SIZE) {
            long fileSize1;
            long fileSize2;

            Long file1Size = sizeCache.get(path + fileName1);
            
            if (file1Size == null) {
                file1 = new File(path + fileName1);
            
                fileSize1 = file1.length();

                sizeCache.put(path + fileName1, fileSize1);
            } else {
                fileSize1 = file1Size;
            }

            Long file2Size = sizeCache.get(path + fileName2);
            
            if (file2Size == null) {
                file2 = new File(path + fileName2);
            
                fileSize2 = file2.length();

                sizeCache.put(path + fileName2, fileSize2);
            } else {
                fileSize2 = file2Size;
            }

            if (fileSize1 < fileSize2) {
                return(1);
            }
            
            if (fileSize1 > fileSize2) {
                return(-1);
            }

            return(0);
        }
        
        if (sortBy == SORT_BY_DATE) {
            long fileDate1;
            long fileDate2;

            Long file1Date = dateCache.get(path + fileName1);
            
            if (file1Date == null) {
                file1 = new File(path + fileName1);
            
                fileDate1 = file1.lastModified();

                dateCache.put(path + fileName1, fileDate1);
            } else {
                fileDate1 = file1Date;
            }
            
            Long file2Date = dateCache.get(path + fileName2);
            
            if (file2Date == null) {
                file2 = new File(path + fileName2);
            
                fileDate2 = file2.lastModified();

                dateCache.put(path + fileName2, fileDate2);
            } else {
                fileDate2 = file2Date;
            }
            
            if (fileDate1 < fileDate2) {
                return(1);
            }
            
            if (fileDate1 > fileDate2) {
                return(-1);
            }

            return(0);
        }
        
        return(0);
    }

    @Override
    public boolean equals(Object obj) {
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.sizeCache);
        hash = 31 * hash + Objects.hashCode(this.dateCache);
        hash = 31 * hash + this.sortBy;
        hash = 31 * hash + Objects.hashCode(this.path);
        return hash;
    }
}