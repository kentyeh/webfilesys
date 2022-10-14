package de.webfilesys;

import java.io.File;

import de.webfilesys.graphics.ThumbnailThread;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubdirExistTester extends Thread {
    private static final Logger logger = LogManager.getLogger(SubdirExistTester.class);
	private CopyOnWriteArrayList<QueueElem> queue = null;

	boolean shutdownFlag = false;

	private static SubdirExistTester instance = null;

	private SubdirExistTester() {
		queue = new CopyOnWriteArrayList<>();
		shutdownFlag = false;
	}

	public static boolean instanceCreated() {
		return (instance != null);
	}

	public static SubdirExistTester getInstance() {
		if (instance == null) {
			instance = new SubdirExistTester();
			instance.start();
		}

		return (instance);
	}

        @Override
	public void run() {
		logger.info("SubdirExistTester started");

		Thread.currentThread().setPriority(1);

		while (!shutdownFlag) {
			while (!queue.isEmpty()) {
				for(Iterator<QueueElem> itor= queue.iterator();itor.hasNext();){
					testForExistingSubdirs(itor.next());

					itor.remove();
				}
                        }
			try {
				synchronized (this) {
					wait();
                                }
			} catch (InterruptedException intEx) {
				shutdownFlag = true;
			}
		}

		logger.info("SubdirExistTester shutting down");
	}

	public synchronized void queuePath(String path, int scope, boolean forceRescan) {
		queue.add(new QueueElem(path, scope, forceRescan));
		notify();
	}

	private void testForExistingSubdirs(QueueElem queueElem) {

		String path = queueElem.getPath();
		
		Integer subdirExist = SubdirExistCache.getInstance().existsSubdir(path);

		if (queueElem.isForceRescan() || (subdirExist == null)) {
	        File rootDir = new File(path);

	        File[] rootFileList = rootDir.listFiles();
	        if (rootFileList != null) {
	        	boolean hasSubdirs = false;
	        	
	            for (int i = 0; (!hasSubdirs) && (i < rootFileList.length); i++) {
	                File tempFile = rootFileList[i];

	                if (tempFile.isDirectory()) {
						if (!tempFile.getName().equals(ThumbnailThread.THUMBNAIL_SUBDIR)) {
							hasSubdirs = true;
						}
	                }
	            }
	            if (hasSubdirs) {
	             	SubdirExistCache.getInstance().setExistsSubdir(path, 1);
	            } else {
	            	SubdirExistCache.getInstance().setExistsSubdir(path, 0);
	            }
	        } else {
	        	// TODO: set ExistsSubdir to null ?
	        	SubdirExistCache.getInstance().setExistsSubdir(path, 0);
	        }
		}
	}

	public class QueueElem {
		private String path = null;

		private int scope = 0;

		private boolean forceRescan = false;
		
		public QueueElem(String path, int scope, boolean forceRescan) {
			this.path = path;
			this.scope = scope;
			this.forceRescan = forceRescan;
		}

		public String getPath() {
			return (path);
		}

		public int getScope() {
			return (scope);
		}
		
		public boolean isForceRescan() {
			return forceRescan;
		}
	}
}
