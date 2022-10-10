package de.webfilesys.gui.user;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.Constants;
import de.webfilesys.FileLink;
import de.webfilesys.MetaInfManager;
import de.webfilesys.WebFileSys;
import de.webfilesys.graphics.CameraExifData;
import de.webfilesys.graphics.RotateFilter;
import de.webfilesys.graphics.ScaledImage;
import de.webfilesys.graphics.ThumbnailThread;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.MimeTypeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class ThumbnailRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(ThumbnailRequestHandler.class);
	protected HttpServletResponse resp = null;
	
	public ThumbnailRequestHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
        
        this.resp = resp;
	}

        @Override
	protected void process()
	{
		String imgFileName = getParameter("imgFile");

		String currentPath = getCwd();
		
		if (CommonUtils.isEmpty(currentPath)) {
			return;
		}

		String imgPath;

		boolean isLink = (getParameter("link") != null);
		if (isLink) {
			FileLink link = MetaInfManager.getInstance().getLink(currentPath, imgFileName);
			if (link != null) {
				imgPath = link.getDestPath();
				if (!accessAllowed(imgPath)) {
					logger.warn("unauthorized access to file " + imgPath);
					try {
						resp.sendError(HttpServletResponse.SC_FORBIDDEN);
					} catch (IOException ioex) {
					}
					return;
				}
			} else {
				logger.warn("invalid link: " + imgFileName);
				try {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (IOException ioex) {
				}
				return;
			}
		} else {
			imgPath = CommonUtils.joinFilesysPath(currentPath, imgFileName);
		}
		
		String thumbnailPath = ThumbnailThread.getThumbnailPath(imgPath);

		File thumbnailFile = new File(thumbnailPath);
		if (thumbnailFile.exists() && thumbnailFile.isFile() && thumbnailFile.canRead()) {
			serveImageFromFile(thumbnailPath, true);
			return;
		}
		
		try {
			ScaledImage scaledImage = new ScaledImage(imgPath, Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE);

			if (scaledImage.getImageType() != ScaledImage.IMG_TYPE_JPEG) {
				serveImageFromFile(imgPath, false);
				return;
			}
			
			if ((scaledImage.getRealWidth() < 500) && (scaledImage.getRealHeight() < 500)) {
				serveImageFromFile(imgPath, false);
				return;
			}
			
			CameraExifData exifData = new CameraExifData(imgPath);

			if (exifData.getThumbnailLength() <= 0) {
				serveImageFromFile(imgPath, false);
				return;
			}
			
			if (exifData.getOrientation() > 1) {
				if (exifData.getThumbnailOrientation() == exifData.getOrientation()) {
					serveImageFromRotatedExifThumb(imgPath, exifData);
					return;
				}
			}
			
			if (picOrientationAndThumbOrientationRotated(scaledImage, exifData)) {
				// orientation flag on picture equals orientation flag on thumbnail and is 6 (rotated)
				serveImageFromRotatedExifThumb(imgPath, exifData);
				return;
			}
			
			int orientationMissmatchResult = checkOrientationMissmatch(scaledImage, exifData);
			
			logger.debug("orientationMissmatch for file " + imgFileName + " : " + orientationMissmatchResult);

			if (orientationMissmatchResult < 0) {
				serveImageFromFile(imgPath, false);
				return;
			}

			if (orientationMissmatchResult == 1) {
				serveImageFromRotatedExifThumb(imgPath, exifData);
				return;
			}

			if (exifData.getOrientation() == 3) {
				// rotated by 180 degree
				serveImageFromRotatedExifThumb(imgPath, exifData);
			}
			
			serveImageFromExifThumb(imgPath, exifData);
				
		} catch (IOException io1) {
			logger.error("failed to create scaled image " + imgPath, io1);
			serveImageFromFile(imgPath, false);
		}
	}
	
	private void serveImageFromFile(String imgPath, boolean isThumbnail) {
		
        File fileToSend = new File(imgPath);
        
        if (fileToSend.exists() && fileToSend.isFile() && (fileToSend.canRead())) {
        	
    		resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
    		resp.setDateHeader("expires", 0l); 
    		
    		String mimeType = MimeTypeMap.getInstance().getMimeType(imgPath);
    		
    		resp.setContentType(mimeType);
        	
        	long fileSize = fileToSend.length();
        	
        	resp.setContentLength((int) fileSize);

        	byte buffer[] = null;
        	
            if (fileSize < 16192) {
                buffer = new byte[16192];
            } else {
                buffer = new byte[65536];
            }
        	
        	try (OutputStream byteOut = resp.getOutputStream();
                        FileInputStream fileInput =new FileInputStream(fileToSend)){
        		int bytesRead = 0;
        		long bytesWritten = 0;
        		
                while ((bytesRead = fileInput.read(buffer)) >= 0) {
                    byteOut.write(buffer, 0, bytesRead);
                    bytesWritten += bytesRead;
                }

                if (bytesWritten != fileSize) {
                    logger.warn(
                        "only " + bytesWritten + " bytes of " + fileSize + " have been written to output");
                } 

                byteOut.flush();
                
                buffer = null;

                if (!isThumbnail) {
            		if (WebFileSys.getInstance().isDownloadStatistics()) {
            			MetaInfManager.getInstance().incrementDownloads(imgPath);
            		}
                }
        	} catch (IOException ioEx) {
            	logger.warn(ioEx);
            }
        } else {
        	logger.error(imgPath + " is not a readable file");
        }
	}
	
	private void serveImageFromExifThumb(String imgPath, CameraExifData exifData) {
		
		byte imgData[] = exifData.getThumbnailData();

		if (imgData == null) {
			logger.error("img data from exif thumb are null for image " + imgPath);
			return;
		}

		resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
		resp.setDateHeader("expires", 0l); 
		
		int docLength = exifData.getThumbnailLength();

		resp.setContentLength(docLength);
		
		String mimeType = MimeTypeMap.getInstance().getMimeType("*.jpg");
		
		resp.setContentType(mimeType);

		try (OutputStream byteOut = resp.getOutputStream()){
			byteOut.write(imgData, 0, docLength);
			byteOut.flush();
		} catch (IOException ioEx) {
        	logger.warn("failed to server image from exif thumb for file " + imgPath, ioEx);
        }
	}
	
	private void serveImageFromRotatedExifThumb(String imgPath, CameraExifData exifData) {
		
		byte imgData[] = exifData.getThumbnailData();

		if (imgData == null) {
			logger.error("img data from exif thumb are null for image " + imgPath);
			return;
		}

		resp.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
		resp.setDateHeader("expires", 0l); 
		
		String mimeType = MimeTypeMap.getInstance().getMimeType("*.png");
		
		resp.setContentType(mimeType);
        
        int orientation = exifData.getOrientation();

        double degree;

        if ((orientation == 6)) {
            degree = 270;
        } else if (orientation == 8) {
            degree = 90;
        } else if (orientation == 3) {
            degree = 180;
        } else {
            degree = 0;
        }
        
        int newWidth;
        int newHeight;
        if (orientation == 3) {
            newWidth = exifData.getThumbWidth();
            newHeight = exifData.getThumbHeight();
        } else {
            newWidth = exifData.getThumbHeight();
            newHeight = exifData.getThumbWidth();
        }
        
        Image origImage = Toolkit.getDefaultToolkit().createImage(imgData);

        Canvas dummyComponent = new Canvas();

        MediaTracker tracker = new MediaTracker(dummyComponent);
        tracker.addImage (origImage,0);

        try {
            tracker.waitForAll();
        } catch(InterruptedException intEx1) {
            logger.warn("rotateImage: " + intEx1);
        }
        
        tracker.removeImage(origImage);
        
        ImageFilter filter = new RotateFilter((Math.PI / 180) * degree);
        ImageProducer producer = new FilteredImageSource(origImage.getSource(), filter);
        Image rotatedImg = dummyComponent.createImage(producer);

        tracker.addImage (rotatedImg,1);

        try {
            tracker.waitForAll();
        } catch(InterruptedException intEx2) {
           logger.error("rotateImage: " + intEx2);
        }

        tracker.removeImage(rotatedImg);

        origImage.flush();
        
        BufferedImage bufferedImg = null;

        Graphics g = null;

        try {
            bufferedImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

            g = bufferedImg.getGraphics();

            g.drawImage(rotatedImg,0,0,null);

            g.dispose();

            rotatedImg.flush();

            byte[] pngBytes;
            com.keypoint.PngEncoder pngEncoder = new com.keypoint.PngEncoderB(bufferedImg);

            OutputStream out = resp.getOutputStream();

            pngBytes = pngEncoder.pngEncode();
            
            if (pngBytes == null) {
                logger.warn("failed to create rotated exif thumbnail image, PNG Encoder : null image");
            } else {
                resp.setContentLength(pngBytes.length);
                out.write(pngBytes);
            }
            
            out.flush();
        } catch (IOException ioex1) {
            logger.error("failed to rotate exif thumbnail image: " + ioex1);
        } catch (OutOfMemoryError memErr) {
            logger.error("not enough memory to complete exif thumbnail image rotation");
        } finally {
            bufferedImg.flush();
            g.dispose();
        }
	}
	
	private boolean picOrientationAndThumbOrientationRotated(ScaledImage scaledImage, CameraExifData exifData) {
		if (scaledImage.getRealWidth() > scaledImage.getRealHeight()) {
			if ((exifData.getOrientation() == 6) && (exifData.getThumbnailOrientation() == 6)) {
				return true;
			}
			if ((exifData.getOrientation() == 8) && (exifData.getThumbnailOrientation() == 8)) {
				return true;
			}
		}
		return false;
	}
	
	private int checkOrientationMissmatch(ScaledImage scaledImage, CameraExifData exifData) {
        boolean orientationMissmatch = false;

    	if (scaledImage.getRealWidth() < scaledImage.getRealHeight()) {
            // portrait orientation
            
			if (exifData.getThumbOrientation() != CameraExifData.ORIENTATION_PORTRAIT) {
				orientationMissmatch = true;
			}
		} else if (scaledImage.getRealWidth() > scaledImage.getRealHeight()) {
            // landscape orientation
            
            if (exifData.getThumbOrientation() != CameraExifData.ORIENTATION_LANDSCAPE) {
                orientationMissmatch = true;
            }
        }

        if (orientationMissmatch) {
            
            if (exifData.getOrientation() == 1) {
                // orientation value of exif data suggests that no rotation is required
                // but orientation of exif thumbnail does not match orientation of
                // the JPEG picture
                // some camera models that have no orientation sensor 
                // set the orientation value to 1
            	
            	/*
        		logger.debug("Exif thumbnail not usable because of wrong orientation data");
        		*/

	        	return (-1);
            }
            
        	return 1;
        }
        
    	return 0;
	}
}
