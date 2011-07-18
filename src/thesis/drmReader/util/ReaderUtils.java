package thesis.drmReader.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.ResourceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReaderUtils {

	public static String getModifiedDocument(Book book, Resource resource,
			int width, int height, String cacheDir)
			throws UnsupportedEncodingException, SAXException, IOException,
			ParserConfigurationException {
		String result = null;
		Document doc = ResourceUtil.getAsDocument(resource);

		/*
		 * 2. handle dom tree
		 */
		NodeList nodeList = doc.getElementsByTagName("*");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Document.ELEMENT_NODE) {

				if ("head".equalsIgnoreCase(node.getNodeName())) {
				}
				/*
				 * <body> Element
				 * 
				 * 1. insert div for columns 2. set font size
				 */
				else if ("body".equalsIgnoreCase(node.getNodeName())) {
					Element bodyElement = (Element) node;
					NodeList bodyChildList = bodyElement.getChildNodes();

					Element divReaderElement = doc.createElement("div");
					divReaderElement.setAttribute("id", "reader");
					divReaderElement.setAttribute("style", "width:" + width
							+ "px;height:" + height + "px;");
					bodyElement.appendChild(divReaderElement);

					Element divWrapElement = doc.createElement("div");
					divWrapElement.setAttribute("id", "wrapper");
					divWrapElement
							.setAttribute(
									"style",
									"position:absolute;"
											+ "top:0px;"
											+ "left:0px;"
											+ "right:0px;"
											+ "bottom:0px;"
											+ "z-index:1;"
											+ "-webkit-transition-property = '-webkit-transform'; "
											+ "-webkit-transition-duration = 1ms; "
											+ "-webkit-transition-timing-function = linear; "
											+ "-webkit-transition-delay=0ms; "
											+ "-webkit-transform-style= preserve-3d;"
											+ "-webkit-transform:translateX(0px);");
					divReaderElement.appendChild(divWrapElement);

					Element divPaddElement = doc.createElement("div");
					divPaddElement.setAttribute("id", "padder");
					divPaddElement.setAttribute("style", "position:absolute;"
							+ "top:0em;" + "left:0em;" + "right:0em;"
							+ "bottom:0em;");
					divWrapElement.appendChild(divPaddElement);

					Element divColElement = doc.createElement("div");
					divColElement.setAttribute("id", "content");
					divColElement.setAttribute("style",
							"-webkit-column-fill: 'auto';"
									+ "-webkit-text-size-adjust: 'none';"
									+ "-webkit-column-gap:0;"
									+ "-webkit-column-width:"
									+ (width - (width * 0.11)) + "px;"
									+ "min-width:200%;" + "position:absolute;"
									+ "top:0px;" + "bottom:0px;");

					for (int j = 0; j < bodyChildList.getLength(); j++) {
						Node bodyChild = (Node) bodyChildList.item(j);
						divColElement.appendChild(bodyChild);
					}

					divPaddElement.appendChild(divColElement);

					// 2. clear attributes
					bodyElement.removeAttribute("xml:lang");

				}
				/*
				 * <img> Element
				 * 
				 * 1. image max size
				 */
				else if ("img".equalsIgnoreCase(node.getNodeName())) {
					Element imgElement = (Element) node;

					String imageRef = imgElement.getAttribute("src");
					Resource imageResource = book.getResources().getByHref(
							imageRef);

					String destFile = cacheDir
							+ imageResource.getId()
							+ imageResource.getMediaType()
									.getDefaultExtension();

					if (imageResource != null) {
						InputStream in = imageResource.getInputStream();
						OutputStream out = new FileOutputStream(destFile);
						IOUtil.copy(in, out);
					}
					imgElement.setAttribute("src",
							"content://thesis.drmReader.reader" + destFile);
				} else if ("link".equalsIgnoreCase(node.getNodeName())) {
					Element cssElement = (Element) node;
					if (cssElement.getAttribute("rel").equalsIgnoreCase(
							"stylesheet")) {// yep we are in a css link
						String href = cssElement.getAttribute("href");
						Resource cssResource = book.getResources().getByHref(
								href);
						String destFile = cacheDir
								+ cssResource.getId()
								+ cssResource.getMediaType()
										.getDefaultExtension();

						if (cssResource != null) {
							InputStream in = cssResource.getInputStream();
							OutputStream out = new FileOutputStream(destFile);
							IOUtil.copy(in, out);
						}
						cssElement.setAttribute("href",
								"content://thesis.drmReader.reader" + destFile);
					}

				}
			}
		}

		/*
		 * 3. DOM to string
		 */
		StringWriter outText = new StringWriter();
		StreamResult sr = new StreamResult(outText);

		Properties oprops = new Properties();
		oprops.put(OutputKeys.METHOD, "html");
		// oprops.put("indent-amount", "4");

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = tf.newTransformer();
			trans.setOutputProperties(oprops);
			trans.transform(new DOMSource(doc), sr);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		result = outText.toString();

		return result;
	}

}
