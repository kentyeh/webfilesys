package de.webfilesys.viewhandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.webfilesys.ViewHandlerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Prettyprints JSON files using the Google GSON library.
 * 
 * @author Frank Hoehnel
 */
public class JsonViewHandler implements ViewHandler {
    private static final Logger logger = LogManager.getLogger(JsonViewHandler.class);
	@Override
	public void process(String filePath, ViewHandlerConfig viewHandlerConfig, HttpServletRequest req,
			HttpServletResponse resp) {

		try (FileReader jsonReader = new FileReader(new File(filePath))){
			processJson(resp, jsonReader);
		} catch (FileNotFoundException e) {
			logger.error("JSON file not found: " + e);
		} catch (IOException e){
                    logger.error("JSON file not found: " + e);
                }
	}

	private void processJson(HttpServletResponse resp, InputStreamReader jsonIn) {
		BufferedReader jsonReader = null;

		try {
			jsonReader = new BufferedReader(jsonIn);
			
			PrintWriter output = resp.getWriter();

			output.println("<html>");
			output.println("<head>");
			output.println("<title>WebFileSys JSON viewer</title>");
			output.println("</head>");
			output.println("<body>");
			output.println("<pre>");

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			try {
				JsonObject el = JsonParser.parseReader​(jsonReader).getAsJsonObject();
				output.println(gson.toJson(el));			
			} catch (JsonSyntaxException jsonEx) {
				output.println("<span style=\"color:red\">The file does not contain valid JSON data.</span>");
				if (logger.isDebugEnabled()) {
					logger.debug("invalid JSON data: " + jsonEx.toString());
				}
			}
			
			output.println("</pre>");
			output.println("</body>");
			output.println("</html>");

			output.flush();
		} catch (IOException e) {
			logger.error("JSON formatting failed: " + e);
		}
	}

	/**
	 * Create the HTML response for viewing the given file contained in a ZIP
	 * archive..
	 * 
	 * @param zipFilePath
	 *            path of the ZIP entry
	 * @param zipIn
	 *            the InputStream for the file extracted from a ZIP archive
	 * @param req
	 *            the servlet request
	 * @param resp
	 *            the servlet response
	 */
        @Override
	public void processZipContent(String zipFilePath, InputStream zipIn, ViewHandlerConfig viewHandlerConfig,
			HttpServletRequest req, HttpServletResponse resp) {
		InputStreamReader jsonReader = new InputStreamReader(zipIn);

		processJson(resp, jsonReader);
	}

	/**
	 * Does this ViewHandler support reading the file from an input stream of a
	 * ZIP archive?
	 * 
	 * @return true if reading from ZIP archive is supported, otherwise false
	 */
        @Override
	public boolean supportsZipContent() {
		return true;
	}

}
