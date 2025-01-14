package de.webfilesys;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class ClipBoard extends ConcurrentHashMap<String, Integer> {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "clipBoard";

    private final int CONTENT_TYPE_DIR = 1;
    private final int CONTENT_TYPE_FILE = 2;

    private final int OPERATION_COPY = 1;
    private final int OPERATION_MOVE = 2;

    private int operation = 0;

    public ClipBoard() {
        super();
    }

    public void reset() {
        this.clear();
        operation = 0;
    }

    public void addFile(String path) {
        put(path, CONTENT_TYPE_FILE);
    }

    public void addDir(String path) {
        put(path, CONTENT_TYPE_DIR);
    }

    public int getContentType(String ident) {
        Integer contentType = get(ident);
        if (contentType == null) {
            return (0);
        }

        return (contentType);
    }

    public ArrayList<String> getAllFiles() {
        return (getAllOfType(CONTENT_TYPE_FILE));
    }

    public ArrayList<String> getAllDirs() {
        return (getAllOfType(CONTENT_TYPE_DIR));
    }

    public ArrayList<String> getAllOfType(int typeToRemove) {
        ArrayList<String> allFiles = null;

        Enumeration<String> allKeys = keys();

        if (allKeys != null) {
            while (allKeys.hasMoreElements()) {
                String path = (String) allKeys.nextElement();
                int contentType = ((Integer) get(path));
                if (contentType == typeToRemove) {
                    if (allFiles == null) {
                        allFiles = new ArrayList<>();
                    }
                    allFiles.add(path);
                }
            }
        }

        if (allFiles == null) {
            return (null);
        }

        return (allFiles);
    }

    public void setOperation(int newOperation) {
        operation = newOperation;
    }

    public int getOperation() {
        return (operation);
    }

    public void setMoveOperation() {
        operation = OPERATION_MOVE;
    }

    public boolean isMoveOperation() {
        return (operation == OPERATION_MOVE);
    }

    public void setCopyOperation() {
        operation = OPERATION_COPY;
    }

    public boolean isCopyOperation() {
        return (operation == OPERATION_COPY);
    }
}
