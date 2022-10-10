package de.webfilesys.util;

import java.io.Writer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

public class XmlUtil
{
    private static final Logger logger = LogManager.getLogger(XmlUtil.class);
    public static Element getChildByTagName(Element e,String tagname)
    {
        if (e==null )
        {
            return null;
        }

        NodeList children=e.getElementsByTagName(tagname);

        if ((children==null) || (children.getLength()==0))
        {
            return(null);
        }

        return((Element) children.item(0));
    }

    public static String getChildText(Element e,String tagname )
    {
        return getElementText(getChildByTagName(e,tagname));
    }

    public static String getElementText(Element e)
    {
        if (e==null)
        {
            return("");
        }

        NodeList children=e.getChildNodes();

        if (children==null)
        {
            return("");
        }

        StringBuilder text = new StringBuilder();

        int listLength=children.getLength();

        for (int i=0;i<listLength;i++)
        {
            Node node=children.item(i);

            int nodeType = node.getNodeType();

            if ((nodeType==Node.TEXT_NODE) || (nodeType==Node.CDATA_SECTION_NODE))
            {
                String nodeValue=node.getNodeValue();
                if (nodeValue!=null)
                {
                    text.append(nodeValue);
                }
            }
        }

        return(text.toString().trim());
    }

    public static void setElementText(Element e,String newValue)
    {
        setElementText(e,newValue,false);
    }

    /**
     * For compatibility reasons the following is required:
     * If the value of a text node is to be changed, but a CDATA section with this name
     * already exists, the CDATA section is removed an a text node is created or changed.
     *
     * If the value of a CDATA section is to be changed, but a text node with this name
     * already exists, the text node is removed an a CDATA section is created or changed.
     *
     */
    public static void setElementText(Element e,String newValue,boolean cdata)
    {
        if (e==null)
        {
            return;
        }

        Node node=null;

        NodeList children=e.getChildNodes();

        if (children!=null)
        {
            Node childToRemove=null;
            boolean changed=false;

            int listLength=children.getLength();

            for (int i=0;i<listLength;i++)
            {
                node=children.item(i);

                int nodeType=node.getNodeType();

                if (nodeType==Node.TEXT_NODE)
                {
                    if (cdata)
                    {
                        childToRemove=node;
                    }
                    else
                    {
                        node.setNodeValue(newValue);
                        changed=true;
                    }
                }

                if (nodeType==Node.CDATA_SECTION_NODE)
                {
                    if (!cdata)
                    {
                        childToRemove=node;
                    }
                    else
                    {
                        node.setNodeValue(newValue);
                        changed=true;
                    }

                }
            }

            if (childToRemove!=null)
            {
                // System.out.println("removing child " + childToRemove.getNodeValue());
                childToRemove.setNodeValue("");
                e.removeChild(childToRemove);
            }

            if (changed)
            {
                return;
            }
        }

        Document doc=e.getOwnerDocument();

        if (cdata)
        {
            node=doc.createCDATASection(newValue);
        }
        else
        {
            node=doc.createTextNode(newValue);
        }

        e.appendChild(node);
    }


    public static void setChildText(Element e,String tagname,String newValue)
    {
        setChildText(e,tagname,newValue,false);
    }

    public static void setChildText(Element e,String tagname,String newValue,boolean cdata)
    {
        Element child=getChildByTagName(e,tagname);

        if (child==null)
        {
            Document doc=e.getOwnerDocument();
            child=doc.createElement(tagname);
            e.appendChild(child);
        }

        setElementText(child,newValue,cdata);
    }

    public static void removeAllChilds(Element parentElem)
    {
        NodeList children = parentElem.getChildNodes();

        if (children != null)
        {
            int listLength = children.getLength();
            for (int i = listLength - 1; i >= 0; i--)
            {
                Element child = (Element) children.item(i);
                parentElem.removeChild(child);
            }
        }
    }
    
    public static void removeChild(Element parentElem, String tagName) {
        Element firstChild = getChildByTagName(parentElem, tagName);
        if (firstChild != null) {
            parentElem.removeChild(firstChild);
        }
    }
    
    public static void writeToStream(Element rootElement,Writer outputWriter)
    {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0 LS 3.0");
            if(impl == null){
                logger.error("No DOMImplementation found !");
            }else{
                LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/TR/REC-xml");
                LSSerializer serializer = impl.createLSSerializer();
                LSOutput output = impl.createLSOutput();
                output.setEncoding("UTF-8");
                output.setCharacterStream(outputWriter);
                serializer.write(rootElement, output);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
              logger.error(ex.getMessage());
        }
    }

public static void writeToStream(Document doc, Writer outputWriter)
{
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0 LS 3.0");
            if(impl == null){
                logger.error("No DOMImplementation found !");
            }else{
                LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/TR/REC-xml");
                LSSerializer serializer = impl.createLSSerializer();
                LSOutput output = impl.createLSOutput();
                output.setEncoding("UTF-8");
                output.setCharacterStream(outputWriter);
                serializer.write(doc, output);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
              logger.error(ex.getMessage());
        }
}
}