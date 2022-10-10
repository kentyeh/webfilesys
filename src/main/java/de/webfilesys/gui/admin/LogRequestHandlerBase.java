package de.webfilesys.gui.admin;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;

/**
 * @author Frank Hoehnel
 */
public abstract class LogRequestHandlerBase extends AdminRequestHandler {

    /**
     * name of the WebFileSys logger defined in log4j.xml
     */
    public static final String WEBFILESYS_LOGGER_NAME = "de.webfilesys";

    /**
     * name of the DailyRollingFileAppender defined in log4j.xml for the
     * de.webfilesys logger
     */
    public static final String APPENDER_NAME = "WebFileSysLogAppender";

    public LogRequestHandlerBase(HttpServletRequest req, HttpServletResponse resp,
            HttpSession session, PrintWriter output, String uid) {
        super(req, resp, session, output, uid);
    }

    protected String getSystemLogFilePath() {
        LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
        String prepath = null;
        for (Logger log : logContext.getLoggers()) {
            if (log.getName().startsWith(WEBFILESYS_LOGGER_NAME)) {
                for (Map.Entry<String, Appender> entry : log.getAppenders().entrySet()) {
                    if (entry.getValue() instanceof AbstractOutputStreamAppender) {
                        AbstractOutputStreamAppender aosa = (AbstractOutputStreamAppender) entry.getValue();
                        String path = aosa.getManager().getName();
                        if (APPENDER_NAME.contains(entry.getKey()) && path.contains(String.valueOf(java.io.File.pathSeparatorChar))) {
                            return path;
                        } else if (path.contains(String.valueOf(java.io.File.pathSeparatorChar))) {
                            prepath = path;
                        }
                    }
                }
            }
        }
        return prepath;
    }
}
