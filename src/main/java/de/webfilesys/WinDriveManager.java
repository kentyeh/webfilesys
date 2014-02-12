package de.webfilesys;

import java.io.File;
import java.util.HashMap;

import javax.swing.filechooser.FileSystemView;

import org.apache.log4j.Logger;

public class WinDriveManager extends Thread
{
	private static WinDriveManager instance = null;  
	
    private HashMap<Integer,String> driveLabels = null;
    
    private FileSystemView fsView = null;     
    
	private WinDriveManager() {
		driveLabels = new HashMap<Integer,String>(1);
		fsView = FileSystemView.getFileSystemView();
		this.start();
	}
	
	public static WinDriveManager getInstance()
	{
		if (instance == null)
		{
			instance = new WinDriveManager();
		}
		
		return instance;
	}
	
    public synchronized void run()
    {
    	if (Logger.getLogger(getClass()).isDebugEnabled())
    	{
        	Logger.getLogger(getClass()).debug("DriveQueryThread started");
    	}
    	
        setPriority(1);

        boolean stop = false;

        while (!stop)
        {
            queryDrives();

            try
            {
                this.wait(60000);  // 60 sec
            }
            catch(InterruptedException e)
            {
                stop = true;
                Logger.getLogger(getClass()).debug("DriveQueryThread ready for shutdown");
            }
        }
    }

    public synchronized void queryDrives()
    {
        HashMap<Integer, String> newDriveLabels = new HashMap<Integer, String>(30);

        File fileSysRoots[]=File.listRoots();

        for (int i=0;i<fileSysRoots.length;i++)
        {
            String fileSysRootName=fileSysRoots[i].getAbsolutePath();

            String label=null;

            label = queryDriveLabel(fileSysRootName);

            if (label == null)
            {
                label = "";
            }

            char driveLetter = fileSysRootName.charAt(0);

            int driveNum = (driveLetter - 'A') + 1;

            newDriveLabels.put(new Integer(driveNum),label);
        }

        driveLabels = newDriveLabels;
    } 
    
    private String queryDriveLabel(String driveString)
    {
    	File driveFile = new File(driveString);
    	
    	return fsView.getSystemDisplayName(driveFile);
    }

    public String getDriveLabel(int drive)
    {
        return ((String) driveLabels.get(new Integer(drive)));
    }
}

