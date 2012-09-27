package thesis.drmReader.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.util.List;
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
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.ResourceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import thesis.sec.Decrypter;

public class ReaderUtils {

	public static String getModifiedDocument(Book book, Resource resource,
			int width, int height, String cacheDir, Decrypter decrypter) throws UnsupportedEncodingException,
			SAXException, IOException, ParserConfigurationException,
			InvalidKeyException {
		String result = null;
		Document doc = ResourceUtil.getAsDocument(resource);
		boolean isCover = isCoverResource(resource, book);

		/*
		 * 2. handle dom tree
		 */
		NodeList nodeList = doc.getElementsByTagName("*");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Document.ELEMENT_NODE) {

				if ("head".equalsIgnoreCase(node.getNodeName())) {
					Element headElement = (Element) node;

					if (isCover) {
						addCSSLink(doc, headElement, "css/cover.css");
					} else {
						addJavaScriptLink(doc, headElement,
								"monocle/monocore.js");
						addJavaScriptLink(doc, headElement,
								"javascript/interface.js");

						addCSSLink(doc, headElement, "css/monocore.css");
						addCSSLink(doc, headElement, "css/reader.css");
					}

				}
				/*
				 * <body> Element
				 * 
				 * 1. insert div for columns 2. set font size
				 */
				else if ("body".equalsIgnoreCase(node.getNodeName())) {
					Element bodyElement = (Element) node;
					NodeList bodyChildList = bodyElement.getChildNodes();

					// 1. insert div for monocle
					Element divElement = doc.createElement("div");
					divElement.setAttribute("id", "reader");
					if (isCover) {
						divElement.setAttribute("style", "width:" + width
								+ "px; height:" + height + "px;");
					}

					for (int j = 0; j < bodyChildList.getLength(); j++) {
						Node bodyChild = (Node) bodyChildList.item(j);
						divElement.appendChild(bodyChild);
					}

					bodyElement.appendChild(divElement);

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

					String destFile = null;
					if (imageResource != null) {
						Resource imageResourceDec = decrypter
								.decrypt(imageResource);

						destFile = cacheDir
								+ imageResourceDec.getId()
								+ imageResourceDec.getMediaType()
										.getDefaultExtension();

						InputStream in = imageResourceDec.getInputStream();
						OutputStream out = new FileOutputStream(destFile);
						IOUtil.copy(in, out);
					}
					imgElement.setAttribute("src",
							"content://thesis.drmReader.reader" + destFile);

				} else if ("link".equalsIgnoreCase(node.getNodeName())) {
					Element cssElement = (Element) node;
					if (cssElement.getAttribute("rel").equalsIgnoreCase(
							"stylesheet")) {
						// we are in a css link
						String href = cssElement.getAttribute("href");
						Resource cssResource = book.getResources().getByHref(
								href);

						String destFile = null;

						if (cssResource != null) {
							Resource cssResourceDec = decrypter
									.decrypt(cssResource);
							destFile = cacheDir
									+ cssResourceDec.getId()
									+ cssResourceDec.getMediaType()
											.getDefaultExtension();

							InputStream in = cssResourceDec.getInputStream();
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

	/**
	 * Add External Javascript Src
	 * 
	 * @param doc
	 * @param headElement
	 * @param path
	 */
	private static void addJavaScriptLink(Document doc, Element headElement,
			String path) {
		Element scriptElement = doc.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setAttribute("src", "url('file:///android_asset/" + path
				+ "')");
		headElement.appendChild(scriptElement);
		headElement.appendChild(doc.createTextNode("\n"));
	}

	/**
	 * Add External CSS Src
	 * 
	 * @param doc
	 * @param headElement
	 * @param path
	 */
	private static void addCSSLink(Document doc, Element headElement,
			String path) {
		Element cssElement = doc.createElement("link");
		cssElement.setAttribute("rel", "stylesheet");
		cssElement.setAttribute("type", "text/css");
		cssElement.setAttribute("href", "url('file:///android_asset/" + path
				+ "')");
		headElement.appendChild(cssElement);
		headElement.appendChild(doc.createTextNode("\n"));
	}

	public static boolean isCoverResource(Resource resource, Book book) {
		return resource.getId().equalsIgnoreCase(book.getCoverPage().getId());
	}

	public static String getChapterName(Book book, Resource chapter) {
		List<TOCReference> tocTitles = book.getTableOfContents()
				.getTocReferences();
		String name = null;
		for (TOCReference ref : tocTitles) {
			if (ref.getResourceId().equalsIgnoreCase(chapter.getId()))
				name = ref.getTitle();
		}

		return name;
	}

}
