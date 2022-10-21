package de.webfilesys.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class ServletBase extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void setSessionInfo(HttpServletRequest req, HttpSession session) {
        session.setAttribute("protocol", req.getScheme());

        session.setAttribute("clientAddress", getClientAddress(req));

        String userAgent = req.getHeader("User-Agent");

        if (userAgent != null) {
            session.setAttribute("userAgent", userAgent);
        }
    }

    private String getClientAddress(HttpServletRequest req) {
        String hostIP = req.getRemoteHost();

		if ((hostIP == null) || (hostIP.trim().length() == 0))
		{
            hostIP = req.getRemoteAddr();
        }

		return(hostIP);
    }

    protected String cp(){
        return cp(null);
    }

    /**
     * get Context Path like &quot;/webfilesys&quot;
     *
     * @param req
     * @return Context Path String
     */
    protected String cp(HttpServletRequest req) {
        if (req == null) {
            ServletContext sc = getServletContext();
            return sc == null ? "/webfilesys" : sc.getContextPath();
        } else {
            return req.getContextPath();
        }
    }
}
