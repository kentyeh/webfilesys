package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastPathQueue {
        private static final Logger logger = LogManager.getLogger(FastPathQueue.class);
        
	public static final String FAST_PATH_DIR = "fastpath";

	private String fastPathFileName = null;

	static final int MAX_QUEUE_SIZE = 25;

	private ArrayList<String> pathQueue = null;
        
        private ReentrantLock lock = new ReentrantLock();

	FastPathQueue(String userid) {
		fastPathFileName = WebFileSys.getInstance().getConfigBaseDir() + "/" + FAST_PATH_DIR + "/" + userid + ".dat";

		if (!loadFromFile()) {
			pathQueue = new ArrayList<>(MAX_QUEUE_SIZE);
		}
	}

	private boolean loadFromFile() {

		boolean success = false;
		
		try (ObjectInputStream fastPathFile = new ObjectInputStream(new FileInputStream(fastPathFileName))) {
			pathQueue = (ArrayList<String>) fastPathFile.readObject();
			success = true;
		} catch (FileNotFoundException ioe) {
			logger.debug(ioe);
		} catch (ClassNotFoundException | IOException | ClassCastException cnfe) {
			logger.warn(cnfe);
		}

		return (success);
	}

	public void saveToFile() {
            try{
                this.lock.lock();
		File fastPathDir = new File(WebFileSys.getInstance().getConfigBaseDir() + "/" + FAST_PATH_DIR);

		if (!fastPathDir.exists()) {
			if (!fastPathDir.mkdirs()) {
				logger.warn("cannot create fastpath directory " + fastPathDir);
			}

			return;
		}

		try (ObjectOutputStream fastPathFile = new ObjectOutputStream(new FileOutputStream(fastPathFileName))){
			fastPathFile.writeObject(pathQueue);
			fastPathFile.flush();
		} catch (IOException ioEx) {
			logger.warn(ioEx);
		}
            } finally{
                this.lock.unlock();
            }
	}

	public void queuePath(String pathName) {
		// remove trailing separator char
		if (File.separatorChar == '/') {
			if ((pathName.length() > 1) && pathName.endsWith("/")) {
				pathName = pathName.substring(0, pathName.length() - 1);
			}
		} else {
			if ((pathName.length() > 3) && pathName.endsWith("\\")) {
				pathName = pathName.substring(0, pathName.length() - 1);
			}
		}
                try{
                    
                this.lock.lock();

		pathQueue.remove(pathName);

		pathQueue.add(0, pathName);

		if (pathQueue.size() > MAX_QUEUE_SIZE) {
			pathQueue.remove(pathQueue.size() - 1);
		}
                } finally{
                    this.lock.unlock();
                }
	}

	public ArrayList<String> getPathList() {
		return (pathQueue);
	}

}