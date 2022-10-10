package de.webfilesys;

import java.io.File;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MP3ExtractorThread extends Thread
{
    private static final Logger logger = LogManager.getLogger(MP3ExtractorThread.class);

    public String musicFileMasks[]={"*.mp3"};

    String basePath=null;

    int numExtracted=0;

    public MP3ExtractorThread(String actPath)
    {
        basePath=actPath;
    }

    @Override
    public void run()
    {
        setPriority(1);

        long startTime=System.currentTimeMillis();
        
        updateMP3Info(basePath);
        
        if (numExtracted > 0)
        {
            long endTime=System.currentTimeMillis();

            logger.debug("MP3ExtractorThread extracted tags from " + numExtracted + " files (" + (endTime-startTime) + " ms)");
        }
    }

    public void updateMP3Info(String currentPath)
    {
        String actPath=currentPath;

        if (!actPath.endsWith(File.separator))
        {
            actPath=actPath + File.separator;
        }

        FileSelector fileSelector=new FileSelector(actPath,FileComparator.SORT_BY_FILENAME);

        FileSelectionStatus selectionStatus=fileSelector.selectFiles(musicFileMasks,2048,null,null);

        ArrayList<String> selectedFiles=selectionStatus.getSelectedFileNames();

        if ((selectedFiles!=null) && (selectedFiles.size()>0))
        {
            for (String selectedFile : selectedFiles) {
                String mp3FileName = actPath + (String) selectedFile;
                MetaInfManager metaInfMgr=MetaInfManager.getInstance();
                String description=metaInfMgr.getDescription(mp3FileName);
                if ((description==null) || (description.trim().length()==0))
                {
                    MP3V2Info mp3Info = new MP3V2Info(mp3FileName);

                    if (mp3Info.isTagIncluded())
                    {
                        StringBuilder songInfo=new StringBuilder();

                        String tmp=mp3Info.getArtist();

                        if (tmp!=null)
                        {
                            songInfo.append(tmp);
                            songInfo.append(": ");
                        }
                        
                        tmp=mp3Info.getTitle();

                        if (tmp!=null)
                        {
                            songInfo.append(tmp);
                        }

                        tmp=mp3Info.getAlbum();

                        if (tmp!=null)
                        {
                            songInfo.append(" (");
                            songInfo.append(tmp);
                            songInfo.append(")");
                        }

                        tmp=mp3Info.getPublishYear();

                        if (tmp!=null)
                        {
                            songInfo.append(" ");
                            songInfo.append(tmp);
                        }

                        tmp=mp3Info.getGenre();

                        if (tmp!=null)
                        {
                            songInfo.append(" [");
                            songInfo.append(tmp);
                            songInfo.append("]");
                        }
                        
                        tmp=mp3Info.getComment();

                        if (tmp!=null)
                        {
                            songInfo.append(" ");
                            songInfo.append(tmp);
                        }

                        metaInfMgr.setDescription(mp3FileName,songInfo.toString().replace((char) 0, ' '));

                        numExtracted++;
                    }
                }
            }
        }
    }

}


