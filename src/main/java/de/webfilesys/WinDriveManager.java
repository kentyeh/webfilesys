package de.webfilesys;

import java.io.File;
import java.util.HashMap;

import javax.swing.filechooser.FileSystemView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WinDriveManager extends Thread
{
    private static final Logger logger = LogManager.getLogger(WinDriveManager.class);
	private static WinDriveManager instance = null;  
	
    private HashMap<Integer,String> driveLabels = null;
    
    private HashMap<Integer,String> driveTypes = null;

    private FileSystemView fsView = null;     
    
	private WinDriveManager() {
		driveLabels = new HashMap<>(1);
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
	
        @Override
    public synchronized void run()
    {
        logger.debug("DriveQueryThread started");
    	
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
                logger.debug("DriveQueryThread ready for shutdown");
            }
        }
    }

    public synchronized void queryDrives()
    {
        HashMap<Integer, String> newDriveLabels = new HashMap<>(30);

        HashMap<Integer, String> newDriveTypes = new HashMap<>(30);

        File fileSysRoots[]=File.listRoots();

            for (File fileSysRoot : fileSysRoots) {
                String fileSysRootName = fileSysRoot.getAbsolutePath();
                char driveLetter = fileSysRootName.charAt(0);
                int driveNum = (driveLetter - 'A') + 1;
                String label = queryDriveLabel(fileSysRootName);
                if (label == null)
                {
                    label = "";
                }   newDriveLabels.put(driveNum, label);
                String driveType = queryDriveType(fileSysRootName);
                if (driveType == null)
                {
                    driveType = "";
                }   newDriveTypes.put(driveNum, driveType);
            }

        driveLabels = newDriveLabels;
        
        driveTypes = newDriveTypes;
    } 
    
    private String queryDriveLabel(String driveString)
    {
    	File driveFile = new File(driveString);
    	
    	return fsView.getSystemDisplayName(driveFile);
    }

    private String queryDriveType(String driveString)
    {
    	File driveFile = new File(driveString);
    	
    	return fsView.getSystemTypeDescription(driveFile);
    }
    
    public String getDriveLabel(int drive)
    {
        return ((String) driveLabels.get(drive));
    }

    public String getDriveType(int drive)
    {
        return ((String) driveTypes.get(drive));
    }
}

