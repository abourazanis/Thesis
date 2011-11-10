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

import android.util.Log;

public class ReaderUtils {

	public static String getModifiedDocument(Book book, Resource resource,
			int width, int height, String cacheDir, Decrypter decrypter)
			throws UnsupportedEncodingException, SAXException, IOException,
			ParserConfigurationException, InvalidKeyException {
		String result = null;
		Document doc = ResourceUtil.getAsDocument(resource);

		/*
		 * 2. handle dom tree
		 */
		NodeList nodeList = doc.getElementsByTagName("*");

		if (isCoverResource(resource, book)) {
			Log.d("readerUtils", "cover");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Document.ELEMENT_NODE) {
					if ("body".equalsIgnoreCase(node.getNodeName())) {
						Element bodyElement = (Element) node;
						NodeList bodyChildList = bodyElement.getChildNodes();

						Element divElement = doc.createElement("div");
						divElement.setAttribute("id", "reader");
						divElement
								.setAttribute(
										"style",
										"width:"
												+ width
												+ "px; height:"
												+ height
												+ "px; border:none; overflow:hidden;padding:0;margin:0;");

						for (int j = 0; j < bodyChildList.getLength(); j++) {
							Node bodyChild = (Node) bodyChildList.item(j);
							divElement.appendChild(bodyChild);
						}

						bodyElement.appendChild(divElement);

						// 2. clear attributes
						bodyElement.removeAttribute("xml:lang");
						bodyElement
								.setAttribute("style",
										"margin:0 0 10px 0;border:none; padding:0; line-height:1.5em;");

					} else if ("img".equalsIgnoreCase(node.getNodeName())) {
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
							Resource cssResource = book.getResources()
									.getByHref(href);

							String destFile = null;

							if (cssResource != null) {
								Resource cssResourceDec = decrypter
										.decrypt(cssResource);
								destFile = cacheDir
										+ cssResourceDec.getId()
										+ cssResourceDec.getMediaType()
												.getDefaultExtension();

								InputStream in = cssResourceDec
										.getInputStream();
								OutputStream out = new FileOutputStream(
										destFile);
								IOUtil.copy(in, out);
							}
							cssElement.setAttribute("href",
									"content://thesis.drmReader.reader"
											+ destFile);
						}

					}
				}
			}

		} else {

			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Document.ELEMENT_NODE) {

					if ("head".equalsIgnoreCase(node.getNodeName())) {
						Element headElement = (Element) node;

						// 1. append monocle library (CORE)
						addJavaScriptLink(doc, headElement,
								"monocle/monocle.js");
						addJavaScriptLink(doc, headElement, "monocle/compat.js");
						addJavaScriptLink(doc, headElement, "monocle/reader.js");
						addJavaScriptLink(doc, headElement, "monocle/book.js");
						addJavaScriptLink(doc, headElement,
								"monocle/component.js");
						addJavaScriptLink(doc, headElement, "monocle/place.js");
						addJavaScriptLink(doc, headElement, "monocle/styles.js");

						// 2. append monocle library (FLIPPERS)
						addJavaScriptLink(doc, headElement,
								"monocle/flippers/instant.js");

						// append monocle interface script
						addJavaScriptLink(doc, headElement,
								"javascript/interface.js");

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
						divElement.setAttribute("style", "width:" + width
								+ "px; height:" + height
								+ "px; border:none; overflow:hidden;");

						for (int j = 0; j < bodyChildList.getLength(); j++) {
							Node bodyChild = (Node) bodyChildList.item(j);
							divElement.appendChild(bodyChild);
						}

						bodyElement.appendChild(divElement);

						// 2. clear attributes
						bodyElement.removeAttribute("xml:lang");

						bodyElement
								.setAttribute("style",
										"margin:0 0 10px 0; padding:0; border:0;line-height:1.5em;");

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
							Resource cssResource = book.getResources()
									.getByHref(href);

							String destFile = null;

							if (cssResource != null) {
								Resource cssResourceDec = decrypter
										.decrypt(cssResource);
								destFile = cacheDir
										+ cssResourceDec.getId()
										+ cssResourceDec.getMediaType()
												.getDefaultExtension();

								InputStream in = cssResourceDec
										.getInputStream();
								OutputStream out = new FileOutputStream(
										destFile);
								IOUtil.copy(in, out);
							}
							cssElement.setAttribute("href",
									"content://thesis.drmReader.reader"
											+ destFile);
						}

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
