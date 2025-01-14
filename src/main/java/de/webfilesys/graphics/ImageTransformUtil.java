package de.webfilesys.graphics;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImageTransformUtil {
    private static final Logger logger = LogManager.getLogger(ImageTransformUtil.class);
	
	public static boolean createScaledImage(String origImgPath, String scaledImgPath, int maxWidth, int maxHeight) {
		
        File origImgFile = new File(origImgPath);
        
        if ((!origImgFile.exists()) || (!origImgFile.isFile()) || (!origImgFile.canRead())) {
            logger.error("not a readable picture file: " + origImgPath);
            return false;
        }

        File scaledImgFile = new File(scaledImgPath);

        if (scaledImgFile.exists()) {
            logger.error("destination file already exists: " + scaledImgPath);
            return false;
        }

        ScaledImage scaledImg = null;

        try {
            scaledImg = new ScaledImage(origImgPath, maxWidth, maxHeight);
        } catch (IOException ioex) {
            logger.error(ioex);
            return false;
        }
        
        if ((scaledImg.getRealWidth() <= maxWidth) && (scaledImg.getRealHeight() <= maxHeight)) {
            return false;
        }

        int scaledWidth = scaledImg.getScaledWidth();
        int scaledHeight = scaledImg.getScaledHeight();
        
        long startTime = System.currentTimeMillis();

        Canvas imgObserver = new Canvas();

        Image origImage = Toolkit.getDefaultToolkit().createImage(origImgPath);

        imgObserver.prepareImage(origImage, imgObserver);

        int timeoutCounter = 900;

        while ((imgObserver.checkImage(origImage, imgObserver) & ImageObserver.ALLBITS) != ImageObserver.ALLBITS) {
            try {
                Thread.sleep(100);

                timeoutCounter--;

                if (timeoutCounter == 0) {
                    logger.error("picture load timeout for image " + origImgPath);
                    origImage.flush();
                    return false;
                }
            } catch (InterruptedException iex) {
                logger.error(iex);
                origImage.flush();
                return false;
            }
        }

        boolean success = false;
        
        BufferedImage bufferedImg = new BufferedImage(scaledImg.getRealWidth(), scaledImg.getRealHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics g = bufferedImg.getGraphics();
        g.drawImage(origImage, 0, 0, imgObserver);

        bufferedImg = ImageTransform.getScaledInstance(bufferedImg, scaledWidth, scaledHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        
        if (scaledImg.getImageType() == ScaledImage.IMG_TYPE_JPEG) {
            try (BufferedOutputStream thumbOut = new BufferedOutputStream(new FileOutputStream(scaledImgPath))){
                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter imgWriter = (ImageWriter) iter.next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(thumbOut);
                imgWriter.setOutput(ios);
                ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(0.9f);
                imgWriter.write(null, new IIOImage(bufferedImg, null, null), iwparam);
                ios.flush();
                imgWriter.dispose();
                long endTime = System.currentTimeMillis();
                logger.debug("scaled JPEG instance created for " + origImgPath + " (" + (endTime - startTime) + " ms)");
                success = true;
            } catch (IOException ioex) {
                logger.error("error writing scaled JPEG instance file " + scaledImgPath, ioex);
            } catch (OutOfMemoryError memEx) {
                logger.error("insufficient memory for scaled JPEG instance creation", memEx);
            }
        }
        else  
        {
            // PNG 
            
            byte[] pngBytes;
            com.keypoint.PngEncoder pngEncoder = new com.keypoint.PngEncoder(bufferedImg);

            /*
            pngEncoder.setCompressionLevel(9);
            pngEncoder.setFilter(com.keypoint.PngEncoder.FILTER_UP);
            */

            try (BufferedOutputStream thumbOut = new BufferedOutputStream(new FileOutputStream(scaledImgPath))){

                pngBytes = pngEncoder.pngEncode();

                if (pngBytes == null) {
                    logger.warn("PNG Encoder : Null image");
                } else {
                    thumbOut.write(pngBytes);
                }
                
                thumbOut.flush();

                long endTime = System.currentTimeMillis();
                logger.debug("scaled PNG instance created for " + origImgPath + " (" + (endTime - startTime) + " ms)");

                success = true;
            } catch (IOException ioex) {
                logger.error("cannot create scaled PNG instance for " + origImgPath, ioex);
            }

            pngBytes = null;
        }
        
        g.dispose();
        bufferedImg.flush();

        origImage.flush();
        
        return success;
	}
	
    public static BufferedImage rotateImage(BufferedImage origImage, double degree) {
    	
    	int newWidth = origImage.getWidth();
    	int newHeight = origImage.getHeight();
    	if ((degree == 90) || (degree == 270)) {
        	newWidth = origImage.getHeight();
        	newHeight = origImage.getWidth();
    	}
    	
        try {
            ImageFilter filter = new RotateFilter((Math.PI / 180) * degree);
            ImageProducer producer = new FilteredImageSource(origImage.getSource(), filter);
            Canvas dummyComponent = new Canvas();
            Image rotatedImg = dummyComponent.createImage(producer);

            MediaTracker tracker = new MediaTracker(dummyComponent);
            tracker.addImage(rotatedImg, 1);

            try {
                tracker.waitForAll();
            } catch(InterruptedException ex) {
               logger.error("failed to rotate image", ex);
            }

            tracker.removeImage(rotatedImg);

            origImage.flush();
            
            BufferedImage bufferedImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

            Graphics g = bufferedImg.getGraphics();
                
            g.drawImage(rotatedImg, 0, 0, dummyComponent);

            g.dispose();

            rotatedImg.flush();

            return bufferedImg;
        } catch (OutOfMemoryError memErr) {
            logger.error("not enough memory for image rotation", memErr);
            return null;
        }
    }

}
