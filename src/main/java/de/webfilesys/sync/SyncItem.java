package de.webfilesys.sync;

import java.util.Date;

public class SyncItem
{
    public static final int DIFF_TYPE_MISSING_TARGET_FILE = 1;
    public static final int DIFF_TYPE_MISSING_SOURCE_FILE = 2;
    public static final int DIFF_TYPE_MISSING_TARGET_DIR  = 3;
    public static final int DIFF_TYPE_MISSING_SOURCE_DIR  = 4;
    public static final int DIFF_TYPE_SIZE                = 5;
    public static final int DIFF_TYPE_MODIFICATION_TIME   = 6;
    public static final int DIFF_TYPE_SIZE_TIME           = 7;
    public static final int DIFF_TYPE_ACCESS_RIGHTS       = 8;

    private SyncFileInfo source = null;

    private SyncFileInfo target = null;

    private String fileName = null;
    
    private int diffType = (-1);
    
    private int id = 0;
    
    public SyncItem(int id)
    {
        this.id = id;
        
        source = new SyncFileInfo();
        target = new SyncFileInfo();
    }
    
    public int getId()
    {
        return id;
    }
    
    public SyncFileInfo getSource()
    {
        return source;
    }
    
    public SyncFileInfo getTarget()
    {
        return target;
    }
    
    public void setFileName(String newVal)
    {
        fileName = newVal;
    }
    
    public String getFileName()
    {
        return fileName;
    }

    public void setDiffType(int newVal)
    {
        diffType = newVal;
    }
    
    public int getDiffType()
    {
        return diffType;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        
        buff.append("SyncItem [id=");
        buff.append(id);
        buff.append(", type=");
        buff.append(diffType);

        buff.append(", fileName=");
        buff.append(fileName);

        buff.append(", source=(");
        buff.append(getSource());
        buff.append("), target=(");
        buff.append(getTarget());

        buff.append(")]");
        
        return buff.toString();
    }
    
    public String getDisplayString()
    {
        StringBuilder buff = new StringBuilder();

        if (diffType == DIFF_TYPE_MISSING_TARGET_FILE)
        {
            buff.append("target file missing: ").append(source.getPath());
        }
        else if (diffType == DIFF_TYPE_MISSING_SOURCE_FILE)
        {
            buff.append("source file missing: ").append(target.getPath());
        }
        else if (diffType == DIFF_TYPE_MISSING_TARGET_DIR)
        {
            buff.append("target folder missing: ").append(source.getPath());
        }
        else if (diffType == DIFF_TYPE_MISSING_SOURCE_DIR)
        {
            buff.append("source folder missing: ").append(target.getPath());
        }
        else if (diffType == DIFF_TYPE_SIZE)
        {
            buff.append("different size: ").append(source.getPath()).append(" source: ").append(getSource().getSize()).append(" target: ").append(getTarget().getSize());
        }
        else if (diffType == DIFF_TYPE_MODIFICATION_TIME)
        {
            buff.append("different modification time: ").append(source.getPath()).append(" source: ").append(new Date(getSource().getModificationTime())).append(" target: ").append(new Date(getTarget().getModificationTime()));
        }
        else if (diffType == DIFF_TYPE_SIZE_TIME)
        {
            buff.append("different size and modification time: ").append(source.getPath()).append(" source: ").append(getSource().getSize()).append(" target: ").append(getTarget().getSize()).append(" source: ").append(new Date(getSource().getModificationTime())).append(" target: ").append(new Date(getTarget().getModificationTime()));
        }
        else if (diffType == DIFF_TYPE_ACCESS_RIGHTS)
        {
            buff.append("different access rights: ").append(source.getPath()).append(" source: ").append(source.getCanRead()).append(",").append(source.getCanWrite()).append(" target: ").append(target.getCanRead()).append(",").append(target.getCanWrite());
        }
        
        return buff.toString();
    }
}
