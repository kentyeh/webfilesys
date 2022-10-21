package de.webfilesys.gui.user;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import de.webfilesys.LanguageManager;
import de.webfilesys.gui.CSSManager;
import de.webfilesys.gui.xsl.XslFileListHandler;
import de.webfilesys.user.TransientUser;
import de.webfilesys.user.UserMgmtException;
import de.webfilesys.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Frank Hoehnel
 */
public class UserSettingsRequestHandler extends UserRequestHandler {
    private static final Logger logger = LogManager.getLogger(UserSettingsRequestHandler.class);
	public UserSettingsRequestHandler(
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

        String password = getParameter("password");
        
        if (password == null) {
        	userForm(null);
        	
        	return;
        }
        
		String login=uid;

		StringBuilder errorMsg=new StringBuilder();

		String temp=null;

		String oldPassword=getParameter("oldpw");

		if ((oldPassword!=null) && (oldPassword.trim().length() > 0)) {
			if (!userMgr.checkPassword(login,oldPassword)) {
				temp=getResource("error.invalidpassword","the current password is invalid");
				errorMsg.append(temp).append("\\n");
			}
		}

		String pwconfirm=getParameter("pwconfirm");

		if (((password!=null) && (password.trim().length() > 0)) ||
			((pwconfirm!=null) && (pwconfirm.trim().length() > 0))) {
			if ((oldPassword==null) || (oldPassword.trim().length()==0)) {
				temp=getResource("error.invalidpassword","the current password is invalid");
				errorMsg.append(temp).append("\\n");
			}

			password=password.trim();

			if (password.length() < 5) {
				temp=getResource("error.passwordlength","the minimum password length is 5 characters");
				errorMsg.append(temp).append("\\n");
			} else {
				if (password.indexOf(' ')>0) {
					temp=getResource("error.spacesinpw","the password must not contain spaces");
					errorMsg.append(temp).append("\\n");
				} else {
					if ((pwconfirm==null) || (!pwconfirm.equals(password))) {
						temp=getResource("error.pwmissmatch","the password and the password confirmation are not equal");
						errorMsg.append(temp).append("\\n");
					}
				}
			}
		}

		String ropassword=getParameter("ropassword");
		String ropwconfirm=getParameter("ropwconfirm");

		if (((ropassword!=null) && (ropassword.trim().length() > 0)) ||
			((ropwconfirm!=null) && (ropwconfirm.trim().length() > 0))) {
			if ((oldPassword==null) || (oldPassword.trim().length()==0)) {
				temp=getResource("error.invalidpassword","the current password is invalid");
				errorMsg.append(temp).append("\\n");
			}

			if (ropassword==null) {
				ropassword="";
			} else {
				ropassword=ropassword.trim();
			}

			if (ropwconfirm==null) {
				ropwconfirm="";
			} else {
				ropwconfirm=ropwconfirm.trim();
			}

			if ((ropassword.length() > 0) || (ropwconfirm.length() > 0)) {
				if (ropassword.length() < 5) {
					temp=getResource("error.passwordlength","the minimum password length is 5 characters");
					errorMsg.append(temp).append("\\n");
				} else {
					if (ropassword.indexOf(' ') >= 0) {
						temp=getResource("error.spacesinpw","the password must not contain spaces");
						errorMsg.append(temp).append("\\n");
					} else {
						if (!ropassword.equals(ropwconfirm)) {
							temp=getResource("error.pwmissmatch","password and password confirmation do not match");
							errorMsg.append(temp).append("\\n");
						}
					}
				}
			}
		}

		if (errorMsg.length()>0) {
			userForm(errorMsg.toString());
			return;
		}

		TransientUser changedUser = userMgr.getUser(login);
		
		if (changedUser == null) {
            logger.error("user for update not found: " + login);
			errorMsg.append("user for update not found: ").append(login);
			userForm(errorMsg.toString());
			return;
		}

		if (!CommonUtils.isEmpty(password)) {
			changedUser.setPassword(password);
		}

		if (!CommonUtils.isEmpty(ropassword)) {
			changedUser.setReadonlyPassword(ropassword);
		}

		String newLang = getParameter("language");
		if (!CommonUtils.isEmpty(newLang)) {
			changedUser.setLanguage(newLang);
		}
		
		String css = getParameter("css");

		if (!CommonUtils.isEmpty(css)) {
			changedUser.setCss(css);
		}

		try {
			userMgr.updateUser(changedUser);
		} catch (UserMgmtException ex) {
            logger.error("failed to update user " + login, ex);
			errorMsg.append("failed to update user ").append(login);
			userForm(errorMsg.toString());
			return;
		}
		
		(new XslFileListHandler(req, resp, session, output, uid)).handleRequest();
	}
	
	protected void userForm(String errorMsg) {
		String login = uid;

		TransientUser user = userMgr.getUser(login);
		if (user == null) {
        	logger.error("user not found: " + login);
        	return;
		}

		output.println("<html>");
		output.println("<head>");

		if (errorMsg!=null) {
			javascriptAlert(errorMsg);
		}

		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/common.css\">");
		output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ req.getContextPath() +"/styles/skins/" + userMgr.getCSS(uid) + ".css\">");

		output.println("</head>");
		output.println("<body>");

		headLine(getResource("label.settings","Settings"));

		output.println("<br>");

		output.println("<form accept-charset=\"utf-8\" method=\"post\" action=\""+ req.getContextPath() + "/servlet\">");
		output.println("<input type=\"hidden\" name=\"command\" value=\"changePw\">");

		output.println("<table class=\"dataForm\" style=\"width:100%\">");
		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.login","userid/login") + "</td>");
		output.println("<td class=\"formParm2\">" + login + "</td>");
		output.println("</tr>");

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.oldpassword","current password") + "</td>");
		output.println("<td class=\"formParm2\"><input type=\"password\" name=\"oldpw\" maxlength=\"30\"/></td>");
		output.println("</tr>");

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.newpassword","new password") + "</td>");
		output.println("<td class=\"formParm2\"><input type=\"password\" name=\"password\" maxlength=\"30\" /></td>");
		output.println("</tr>");

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.passwordconfirm","password confirmation") + "</td>");
		output.println("<td class=\"formParm2\"><input type=\"password\" name=\"pwconfirm\" maxlength=\"30\" /></td>");
		output.println("</tr>");

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.ropassword","read-only password") + "</td>");
		output.println("<td class=\"formParm2\"><input type=\"password\" name=\"ropassword\" maxlength=\"30\" /></td>");
		output.println("</tr>");

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.ropwconfirm","read-only password confirmation") + "</td>");
		output.println("<td class=\"formParm2\"><input type=\"password\" name=\"ropwconfirm\" maxlength=\"30\" /></td>");
		output.println("</tr>");

        output.println("<tr>");
        output.println("<td class=\"formParm1\">" + getResource("label.language", "language") + "</td>");
		output.println("<td class=\"formParm2\"><select id=\"language\" name=\"language\" size=\"1\">");

		ArrayList<String> languages = LanguageManager.getInstance().getAvailableLanguages();

		for (String lang : languages) {
			output.print("<option");

			if (lang.equals(language)) {
				output.print(" selected=\"selected\"");
			}

			output.println(">" + lang + "</option>");
		}
		
		output.println("</select></td>");
        output.println("</tr>");
		
        String userCss = null;
        
        if (errorMsg != null) {
        	userCss = getParameter("css");
        } else {
			userCss = user.getCss();

			if (userCss == null) {
				userCss = CSSManager.DEFAULT_LAYOUT;
			}
        }

		ArrayList<String> cssList = CSSManager.getInstance().getAvailableCss();

		output.println("<tr>");
		output.println("<td class=\"formParm1\">" + getResource("label.css","layout") + "</td>");
		output.println("<td class=\"formParm2\">");
		output.println("<select name=\"css\" size=\"1\">");

            for (String css : cssList) {
                if (!css.equals("mobile")) {
                    output.print("<option");
                    
                    if (css.equals(userCss)) {
                        output.print(" selected=\"true\"");
                    }
                    
                    output.println(">" + css + "</option>");
                }
            }
		output.println("</select>");
		output.println("</td>");
		output.println("</tr>");

		output.println("<tr><td colspan=2>&nbsp;</td></tr>");
		
		output.println("<tr>");
		output.println("<td class=\"formButton\">");
		output.println("<input type=\"submit\" name=\"changebutton\" value=\"" + getResource("button.save","&nbsp;Save&nbsp;") + "\">");
		output.println("</td>");
		output.println("<td class=\"formButton\" style=\"text-align:right\">");
		output.println("<input type=\"button\" value=\"" + getResource("button.cancel","&nbsp;Cancel&nbsp;") + "\" onclick=\"window.location.href='"+ req.getContextPath() + "/servlet?command=listFiles';\">");
		output.println("</td>");
		output.println("</tr>");    

		output.println("</table>");

		output.println("</form>");

		output.println("</body></html>");
		output.flush();
	}
}
