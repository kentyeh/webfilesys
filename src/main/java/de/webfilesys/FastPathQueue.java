package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastPathQueue {
        private static final Logger logger = LogManager.getLogger(FastPathQueue.class);
        
	public static final String FAST_PATH_DIR = "fastpath";

	private String fastPathFileName = null;

	static final int MAX_QUEUE_SIZE = 25;

	private ArrayList<String> pathQueue = null;

	FastPathQueue(String userid) {
		fastPathFileName = WebFileSys.getInstance().getConfigBaseDir() + "/" + FAST_PATH_DIR + "/" + userid + ".dat";

		if (!loadFromFile()) {
			pathQueue = new ArrayList<>(MAX_QUEUE_SIZE);
		}
	}

	private boolean loadFromFile() {

		boolean success = false;
		
		ObjectInputStream fastPathFile = null;

		try {
			fastPathFile = new ObjectInputStream(new FileInputStream(fastPathFileName));
			pathQueue = (ArrayList<String>) fastPathFile.readObject();
			fastPathFile.close();
			success = true;
		} catch (FileNotFoundException ioe) {
			logger.debug(ioe);
		} catch (ClassNotFoundException | IOException | ClassCastException cnfe) {
			logger.warn(cnfe);
		} finally {
			if (fastPathFile != null) {
				try {
					fastPathFile.close();
				} catch (Exception ex) {
				}
			}
		}

		return (success);
	}

	public void saveToFile() {
		File fastPathDir = new File(WebFileSys.getInstance().getConfigBaseDir() + "/" + FAST_PATH_DIR);

		if (!fastPathDir.exists()) {
			if (!fastPathDir.mkdirs()) {
				logger.warn("cannot create fastpath directory " + fastPathDir);
			}

			return;
		}

		ObjectOutputStream fastPathFile = null;

		try {
			fastPathFile = new ObjectOutputStream(new FileOutputStream(fastPathFileName));
			fastPathFile.writeObject(pathQueue);
			fastPathFile.flush();
		} catch (IOException ioEx) {
			logger.warn(ioEx);
		} finally {
			if (fastPathFile != null) {
				try {
					fastPathFile.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	public synchronized void queuePath(String pathName) {
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

		pathQueue.remove(pathName);

		pathQueue.add(0, pathName);

		if (pathQueue.size() > MAX_QUEUE_SIZE) {
			pathQueue.remove(pathQueue.size() - 1);
		}
	}

	public ArrayList<String> getPathList() {
		return (pathQueue);
	}

}