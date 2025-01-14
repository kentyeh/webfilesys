package de.webfilesys.gui.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.webfilesys.MetaInfManager;
import de.webfilesys.gui.xsl.XslFileListHandler;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AES encryption for a single file.
 * @author Frank Hoehnel
 */
public class EncryptFileRequestHandler extends UserRequestHandler
{
    private static final Logger logger = LogManager.getLogger(EncryptFileRequestHandler.class);
    public static byte[] CHECKSUM = new byte[] {0x10, 0x20, 0x30, 0x40, 0x50, 0x60};
    
	protected HttpServletRequest req = null;

	protected HttpServletResponse resp = null;

	public EncryptFileRequestHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
        
        this.req = req;
        
        this.resp = resp;
	}

        @Override
	protected void process()
	{
		if (!checkWriteAccess())
		{
			return;
		}

		String fileName = getParameter("fileName");

		if ((fileName == null) || (fileName.trim().length() == 0))
		{
		    logger.error("parameter fileName missing");
		    return;
		}
		
		String cryptoKey = getParameter("cryptoKey");
		
        if ((cryptoKey == null) || (cryptoKey.trim().length() == 0))
        {
            logger.error("parameter cryptoKey missing");
            return;
        }
		
        String currentPath = getCwd();
        
        encryptFile(currentPath, fileName, cryptoKey);
        
		MetaInfManager.getInstance().removeMetaInf(currentPath, fileName);
		
		// TODO: remove thumbnails
		/*
		String thumbnailPath = ThumbnailThread.getThumbnailPath(oldFilePath);
				
		File thumbnailFile = new File(thumbnailPath);
				
		if (thumbnailFile.exists())
		{
			if (!thumbnailFile.delete())
			{
				logger.debug("cannot remove thumbnail file " + thumbnailPath);
			}
		}
		*/

		setParameter("actpath", getCwd());

		setParameter("mask","*");
		
        (new XslFileListHandler(req, resp, session, output, uid)).handleRequest();
	}
	
	private void encryptFile(String currentPath, String fileName, String cryptoKey)
	{
	    SecretKeySpec secretKey = new SecretKeySpec(getMD5(cryptoKey), "AES");
	    
        byte[] iv = new byte[]
                    {
                        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
                    };

        Cipher ecipher = null;
        
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        try
        {
            ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // CBC requires an initialization vector
            ecipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e)
        {
            logger.error("failed to create cipher for AES encryption", e);
        }
	    
	    File sourceFile = new File(currentPath, fileName);
	    
	    if ((!sourceFile.exists()) || (!sourceFile.isFile()) || (!sourceFile.canRead()))
	    {
	        logger.error(fileName + " is not a readable file");
	        return;
	    }
	    
        String targetFileName = fileName + ".aes";
        
	    File targetFile = new File(currentPath, targetFileName);

	    int i = 0;
	    
	    while (targetFile.exists())
	    {
	        targetFileName = fileName + "_" + i + ".aes";
	        
	        targetFile = new File(currentPath, targetFileName);
	        
	        i++;
	    }
	
        try(FileInputStream fileIn = new FileInputStream(sourceFile);
                FileOutputStream fileOut = new FileOutputStream(targetFile);
                CipherOutputStream encryptedOut = new CipherOutputStream(fileOut, ecipher))
	    {
	        encryptedOut.write(CHECKSUM, 0, CHECKSUM.length);
	        
            byte[] buff = new byte[1024];

            int numRead = 0;
            
            while ((numRead = fileIn.read(buff)) >= 0){
                encryptedOut.write(buff, 0, numRead);
            }
	        
	    }
	    catch (FileNotFoundException fnfEx)
	    {
	        logger.error(fnfEx);
	    }
	    catch (IOException ioex)
	    {
	        logger.error(ioex);
	    }
	}
	
	private static byte[] getMD5(String input){
        try
        {
            byte[] bytesOfMessage = input.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(bytesOfMessage);
        }  
        catch (UnsupportedEncodingException | NoSuchAlgorithmException e)
        {
             return null;
        }
    }
	
}
