package de.webfilesys.gui.user;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.gui.xsl.XslUserSettingsHandler;
import de.webfilesys.mail.EmailUtils;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class SelfChangeUserRequestHandler extends UserRequestHandler {
    private static final Logger logger = LogManager.getLogger(SelfChangeUserRequestHandler.class);
	public SelfChangeUserRequestHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}

        @Override
	protected void process() {
		if (!checkWriteAccess()) {
			return;
		}

		String login = uid;

		StringBuilder errorMsg = new StringBuilder();

		String temp = null;

		String password = getParameter("password");
		String pwconfirm = getParameter("pwconfirm");

		if ((password != null) && (password.trim().length() > 0) ||
			(pwconfirm != null) && (pwconfirm.trim().length() > 0)) {
			if ((password == null) || (password.trim().length() < 5)) {
				temp=getResource("error.passwordlength","the minimum password length is 5 characters");
				errorMsg.append(temp).append("<br/>");
			} else {
				if (password.indexOf(' ')>0) {
					temp = getResource("error.spacesinpw","the password must not contain spaces");
					errorMsg.append(temp).append("<br/>");
				} else {
					if ((pwconfirm == null) || (!pwconfirm.equals(password))) {
						temp = getResource("error.pwmissmatch","the password and the password confirmation are not equal");
						errorMsg.append(temp).append("<br/>");
					}
				}
			}
		}

		String ropassword = getParameter("ropassword");
		String ropwconfirm = getParameter("ropwconfirm");

		if (ropassword == null) {
			ropassword = "";
		} else {
			ropassword = ropassword.trim();
		}

		if (ropwconfirm == null) {
			ropwconfirm  = "";
		} else {
			ropwconfirm = ropwconfirm.trim();
		}

		if ((ropassword.length() > 0) || (ropwconfirm.length() > 0)) {
			if (ropassword.length() < 5) {
				temp=getResource("error.passwordlength", "the minimum password length is 5 characters");
				errorMsg.append(temp).append("<br/>");
			} else {
				if (ropassword.indexOf(' ') >= 0) {
					temp=getResource("error.spacesinpw", "the password must not contain spaces");
					errorMsg.append(temp).append("<br/>");
				} else {
					if (!ropassword.equals(ropwconfirm)) {
						temp=getResource("error.pwmissmatch", "password and password confirmation do not match");
						errorMsg.append(temp).append("<br/>");
					}
				}
			}
		}

		String email = getParameter("email");

		if ((email == null) || (!EmailUtils.emailSyntaxOk(email))) {
			temp = getResource("error.email", "a valid e-mail address is required");
			errorMsg.append(temp).append("<br/>");
		}

		if (errorMsg.length()>0) {
			(new XslUserSettingsHandler(req, resp, session, output, uid, errorMsg.toString())).handleRequest();

			return;
		}
		
		TransientUser changedUser = userMgr.getUser(login);
		
		if (changedUser == null) {
            logger.error("user for update not found: " + login);
			errorMsg.append("user for update not found: ").append(login);
			(new XslUserSettingsHandler(req, resp, session, output, uid, errorMsg.toString())).handleRequest();
			return;
		}
		
		if (!CommonUtils.isEmpty(password)) {
			changedUser.setPassword(password);
		}

		if (!CommonUtils.isEmpty(ropassword)) {
			changedUser.setReadonlyPassword(ropassword);
		}

		changedUser.setEmail(email);

		String firstName = getParameter("firstName");

		if (!CommonUtils.isEmpty(firstName)) {
			changedUser.setFirstName(firstName);
		}

		String lastName = getParameter("lastName");

		if (!CommonUtils.isEmpty(lastName)) {
			changedUser.setLastName(lastName);
		}

		String phone = getParameter("phone");

		if (!CommonUtils.isEmpty(phone)) {
			changedUser.setPhone(phone);
		}

		String userLanguage = getParameter("language");

		if (!CommonUtils.isEmpty(userLanguage)) {
			changedUser.setLanguage(userLanguage);
		}

		String css = getParameter("css");

		if (!CommonUtils.isEmpty(css)) {
			changedUser.setCss(css);
		}

		try {
			userMgr.updateUser(changedUser);
		} catch (UserMgmtException ex) {
            logger.error("failed to update user " + login, ex);
			errorMsg.append("failed to update user " + login);
			(new XslUserSettingsHandler(req, resp, session, output, uid, errorMsg.toString())).handleRequest();
			return;
		}
		
        output.println("<html>");
        output.println("<head>");
        output.println("<script type=\"text/javascript\">");
        output.println("parent.parent.location.href = '"+ req.getContextPath() + "/servlet';");
        output.println("</script>");
        output.println("</html>");
        output.flush();
	}

}
