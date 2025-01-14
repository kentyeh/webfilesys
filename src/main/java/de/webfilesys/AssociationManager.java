package de.webfilesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import de.webfilesys.util.PatternComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles associations of application programs to file types.
 * @author fho
 */
public class AssociationManager {
    public static final String ASSOCIATION_CONFIG_FILE = "fileTypeAssociations.conf";    
	private static final Logger logger = LogManager.getLogger(AssociationManager.class);

	private static AssociationManager assocMgr = null;
	
	private Properties associationMap;
	
	private AssociationManager() {
		associationMap = new Properties();

        File assocFile = new File(WebFileSys.getInstance().getConfigBaseDir() + File.separator + ASSOCIATION_CONFIG_FILE);
        if (assocFile.exists() && assocFile.isFile() && assocFile.canRead()) {
            try (FileInputStream fin = new FileInputStream(assocFile)){
            	
            	associationMap.load(fin);
            	
            	logger.debug("filetype associations loaded from " + ASSOCIATION_CONFIG_FILE);
            } catch (IOException e) {
                logger.error("cannot load filetype associations from " + ASSOCIATION_CONFIG_FILE, e);
            } 
        } else {
            logger.error("cannot load filetype associations from " + ASSOCIATION_CONFIG_FILE);
        }
	}
	
	public static AssociationManager getInstance() {
		if (assocMgr == null) {
			assocMgr = new AssociationManager();
		}
		return assocMgr;
	}
	
	/**
	 * Returns the path of the application program which is assigned to the filetype of the given filename.
	 * @param filename the file name
	 * @return path of application program or null, if no association found
	 */
    public String getAssociatedProgram(String filename) {
        Enumeration<Object> keys = associationMap.keys();

        while (keys.hasMoreElements()) {
            String filePattern = (String) keys.nextElement();

            if (PatternComparator.patternMatch(filename, filePattern)) {
                return (associationMap.getProperty(filePattern));
            }
        }

        return (null);
    }

}
