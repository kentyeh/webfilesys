package de.webfilesys.graphics;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import de.webfilesys.Constants;
import de.webfilesys.FileComparator;
import de.webfilesys.FileSelectionStatus;
import de.webfilesys.FileSelector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThumbnailCreatorBase
{
    private static final Logger logger = LogManager.getLogger(ThumbnailCreatorBase.class);
    public static final int SCALE_MIN_SIZE = 300;
    
    public static final String THUMBNAIL_SUBDIR = "_thumbnails";

    public String imgFileMasks[]={"*.jpg","*.jpeg","*.png"};

    public static final int SCOPE_FILE = 1;
    public static final int SCOPE_DIR  = 2;
    public static final int SCOPE_TREE = 3;
    
    protected void createScaledImage(String imgFileName)
    {
        ScaledImage scaledImg = null;

        try
        {
            scaledImg = new ScaledImage(imgFileName, Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE);
        }
        catch (IOException ioex)
        {
            logger.error(ioex);
            return;
        }
        
        if (scaledImg.getRealWidth() + scaledImg.getRealHeight() < 2 * SCALE_MIN_SIZE)
        {
            return;
        }

        CameraExifData exifData = null;

        if (scaledImg.getImageType() == ScaledImage.IMG_TYPE_JPEG)
        {
            exifData = new CameraExifData(imgFileName);

            if (exifData.getThumbnailLength() > 0)
            {
            	return;
            }
        }

        String thumbDirPath = imgFileName.substring(0,imgFileName.lastIndexOf(File.separatorChar)+1) + THUMBNAIL_SUBDIR;

        File thumbDir = new File(thumbDirPath);

        if (!thumbDir.exists())
        {
            if (!thumbDir.mkdir())
            {
                logger.error("cannot create thumbnail dir: " + thumbDir);
                return;                   
            }
        }
        else
        {
            if ((!thumbDir.isDirectory()) || (!thumbDir.canWrite()))
            {
                logger.error("cannot write to thumbnail dir: " + thumbDir);
                return;                   
            }
        }   

        String thumbFileName = getThumbnailPath(imgFileName);

        int scaledWidth = scaledImg.getScaledWidth();
        int scaledHeight = scaledImg.getScaledHeight();
        
        long startTime = System.currentTimeMillis();
        
        Image origImage = Toolkit.getDefaultToolkit().createImage(imgFileName);

        Canvas dummyComponent = new Canvas();

        MediaTracker tracker = new MediaTracker(dummyComponent);
        tracker.addImage(origImage, 0);

        try {
            tracker.waitForAll();
        } catch (Exception ex) {
            logger.warn("failed to load original image for thumbnail creation", ex);
        }

        tracker.removeImage(origImage);
        
        BufferedImage bufferedImg = new BufferedImage(scaledImg.getRealWidth(), scaledImg.getRealHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics g = bufferedImg.getGraphics();
        g.drawImage(origImage, 0, 0, dummyComponent);

        bufferedImg = ImageTransform.getScaledInstance(bufferedImg, scaledWidth, scaledHeight,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        
        if (scaledImg.getImageType() == ScaledImage.IMG_TYPE_JPEG)
        {
			if (exifData.getOrientation() == 6) {
				bufferedImg = ImageTransformUtil.rotateImage(bufferedImg, 270);
			} else if (exifData.getOrientation() == 8) {
				bufferedImg = ImageTransformUtil.rotateImage(bufferedImg, 90);
			} else if (exifData.getOrientation() == 3) {
				bufferedImg = ImageTransformUtil.rotateImage(bufferedImg, 180);
			}
        	
            // JPEG thumbnails
            boolean success = false;
            
            try (BufferedOutputStream thumbOut = new BufferedOutputStream(new FileOutputStream(thumbFileName)))
            {
                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter imgWriter = iter.next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(thumbOut);
                imgWriter.setOutput(ios);
                ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(0.9f);
                imgWriter.write(null, new IIOImage(bufferedImg, null, null), iwparam);
                ios.flush();
                imgWriter.dispose();
                long endTime = System.currentTimeMillis();
                logger.debug("JPEG Thumbnail created for " + imgFileName + " (" + (endTime - startTime) + " ms)");
                success = true;
            }
            catch (IOException ioex) 
            {
                logger.error("error writing thumbnail file " + thumbFileName, ioex);
            }
            catch (OutOfMemoryError memEx)
            {
                logger.error("insufficient memory for thumbnail creation", memEx);
            }

            if (!success) {
                cleanupThumbnail(thumbFileName);
            }
        }
        else  
        {
            // PNG thumbnails
            
            byte[] pngBytes;
            com.keypoint.PngEncoder pngEncoder = new com.keypoint.PngEncoder(bufferedImg);

            /*
            pngEncoder.setCompressionLevel(9);
            pngEncoder.setFilter(com.keypoint.PngEncoder.FILTER_UP);
            */

            boolean success = false;
            
            try (BufferedOutputStream thumbOut = new BufferedOutputStream(new FileOutputStream(thumbFileName)))
            {

                pngBytes = pngEncoder.pngEncode();

                if (pngBytes == null)
                {
                    logger.warn("PNG Encoder : Null image");
                }
                else
                {
                    thumbOut.write(pngBytes);
                }
                
                thumbOut.flush();

                long endTime = System.currentTimeMillis();
                logger.debug("PNG Thumbnail created for " + imgFileName + " (" + (endTime - startTime) + " ms)");
                
                success = true;
            }
            catch (IOException ioex)
            {
                logger.error("cannot create PNG thumbnail for " + imgFileName, ioex);
            }

            if (!success) {
                cleanupThumbnail(thumbFileName);
            }
            
            pngBytes = null;

            // end PNG
        }
        
        g.dispose();
        bufferedImg.flush();

        origImage.flush();
    }
    
    public boolean thumbnailUpToDate(String imgPath)
    {
        File imgFile=new File(imgPath);
        if (!imgFile.exists())
        {
            return(true);
        }

        String thumbFileName=getThumbnailPath(imgPath);

        File thumbnailFile=new File(thumbFileName);

        boolean upToDate=true;

        if (!thumbnailFile.exists())
        {
            upToDate=false;
        }
        else
        {
            long thumbDate=thumbnailFile.lastModified();
            long imgDate=imgFile.lastModified();

            if (imgDate>thumbDate)
            {
                upToDate=false;
            }
        }

        return(upToDate);
    }
    
    public boolean createThumbnail(String imagePath)
    {
        if (!thumbnailUpToDate(imagePath))
        {
            createScaledImage(imagePath);
        }

        return(true);
    }
    
    public void updateThumbnails(String currentPath,boolean recurse)
    {
        String actPath=currentPath;

        if (!actPath.endsWith(File.separator))
        {
            actPath=actPath + File.separator;
        }

        if (recurse)
        {
            // depth first - to prevent recursive thumbnail directories

            File dirFile=new File(currentPath);

            String fileList[]=dirFile.list();

            if ((fileList!=null) && (fileList.length > 0))
            {
                for (String fileName : fileList) {
                    String fullPath=actPath + fileName;

                    File tempFile=new File(fullPath);

                    if (tempFile.exists() && tempFile.isDirectory())
                    {
                        updateThumbnails(fullPath,true);
                    }
                }
            }
        }

        FileSelector fileSelector=new FileSelector(actPath,FileComparator.SORT_BY_FILENAME);

        FileSelectionStatus selectionStatus=fileSelector.selectFiles(imgFileMasks,2048,null,null);

        ArrayList<String> selectedFiles=selectionStatus.getSelectedFileNames();

        if ((selectedFiles!=null) && (selectedFiles.size()>0))
        {
            for (String selectedFile : selectedFiles) {
                String actFileName = (String) selectedFile;
                if (!thumbnailUpToDate(actPath + actFileName))
                {
                    createScaledImage(actPath + actFileName);
                }
            }
        }
    }
    
    protected void cleanupThumbnail(String thumbFileName)
    {
        File tempFile = new File(thumbFileName);

        if (!tempFile.delete())
        {
            logger.error("cannot cleanup thumbnail file " + thumbFileName);
        }
    }

    public static String getThumbnailPath(String imgPath)
    {
        int sepIdx = imgPath.lastIndexOf(File.separator);

        if (sepIdx < 0)
        {
            logger.error("incorrect image path: " + imgPath);
            return(null); 
        }

        String basePath=imgPath.substring(0,sepIdx+1);

        String thumbPath=basePath + THUMBNAIL_SUBDIR + File.separator;

        String imgFileName=imgPath.substring(sepIdx+1);

        return(thumbPath + imgFileName);
    }
}
