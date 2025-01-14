package de.webfilesys.gui.ajax;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import de.webfilesys.stats.DirStatsByType;
import de.webfilesys.stats.TypeCategory;
import de.webfilesys.util.CommonUtils;
import de.webfilesys.util.XmlUtil;

/**
 * @author Frank Hoehnel
 */
public class XmlFileTypeStatsHandler extends XmlRequestHandlerBase
{
	public XmlFileTypeStatsHandler(
			HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid)
	{
        super(req, resp, session, output, uid);
	}
	  
        @Override
	protected void process()
	{
		String currentPath = getParameter("actpath");

		DirStatsByType dirStats = new DirStatsByType(currentPath);
		
		ArrayList<TypeCategory> statisticResults = dirStats.getResults();
		
		Element treeStatsElement = doc.createElement("treeStats");
			
		doc.appendChild(treeStatsElement);

		XmlUtil.setChildText(treeStatsElement, "css", userMgr.getCSS(uid), false);
		XmlUtil.setChildText(treeStatsElement, "currentPath", currentPath, false);
		XmlUtil.setChildText(treeStatsElement, "relativePath", this.getHeadlinePath(currentPath), false);
		XmlUtil.setChildText(treeStatsElement, "shortPath", CommonUtils.shortName(this.getHeadlinePath(currentPath), 60), false);
		
        addMsgResource("stats.typeWinTitle", getResource("stats.typeWinTitle","Directory Statistics by File Type"));
        addMsgResource("stats.fileType", getResource("stats.fileType","file type"));
        addMsgResource("stats.fileNum", getResource("stats.fileNum","# files"));
        addMsgResource("stats.percentFileNum", getResource("stats.percentFileNum","% file num"));
        addMsgResource("stats.sizeSum", getResource("stats.sizeSum","size sum"));
        addMsgResource("stats.percentSizeSum", getResource("stats.percentSizeSum","% size"));

		addMsgResource("button.closewin", getResource("button.closewin","Close Window"));

		Element sizeStatsElem = doc.createElement("sizeStats");
        treeStatsElement.appendChild(sizeStatsElem);
        
        DecimalFormat numFormat = new DecimalFormat("#,###,###,###");

        long fileNumCategoryMax = dirStats.getFileNumCategoryMax();
        
        long sizeSumCategoryMax = dirStats.getSizeSumCategoryMax();
        
            for (TypeCategory typeCat : statisticResults) {
                Element clusterElem = doc.createElement("cluster");
                XmlUtil.setChildText(clusterElem, "fileType", CommonUtils.shortName(typeCat.getFileExt(), 16));
                XmlUtil.setChildText(clusterElem, "fileNum", numFormat.format(typeCat.getFileNum()));
                XmlUtil.setChildText(clusterElem, "sizeSum", numFormat.format(typeCat.getSizeSum()));
                XmlUtil.setChildText(clusterElem, "numberPercent", Integer.toString(typeCat.getFileNumPercent()));
                XmlUtil.setChildText(clusterElem, "sizePercent", Integer.toString(typeCat.getSizePercent()));
                XmlUtil.setChildText(clusterElem, "fileNumPercentOfMax", Long.toString((typeCat.getFileNum() * 100L) / fileNumCategoryMax));
                XmlUtil.setChildText(clusterElem, "sizeSumPercentOfMax", Long.toString((typeCat.getSizeSum() * 100L) / sizeSumCategoryMax));
                sizeStatsElem.appendChild(clusterElem);
            }
		
        processResponse();
    }
	
}