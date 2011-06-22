package thesis.pedlib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.xml.sax.InputSource;
import thesis.pedlib.ped.Resource;

public class ResourceUtil {

	public static String calculateHref(File rootDir, File currentFile)
			throws IOException {
		String result = currentFile.getName().toString()
				.substring(rootDir.getName().toString().length() + 1);
		result += ".html";
		return result;
	}

	public static Resource createResource(File file) throws IOException {
		if (file == null) {
			return null;
		}
		MediaType mediaType = MediaTypeService.determineType(file.getName());
		byte[] data = new byte[((int) file.length())];
		new FileInputStream(file).read(data);
		Resource result = new Resource(data, mediaType);
		return result;
	}

	/**
	 * Creates a resource with as contents a html page with the given title.
	 * 
	 * @param title
	 * @param href
	 * @return
	 */
	public static Resource createResource(String title, String href) {
		String content = "<html><head><title>" + title
				+ "</title></head><body><h1>" + title + "</h1></body></html>";
		return new Resource(content.getBytes(), href, MediaTypeService.XHTML);
	}

	/**
	 * Creates a resource out of the given zipEntry and zipInputStream.
	 * 
	 * @param zipEntry
	 * @param zipInputStream
	 * @return
	 * @throws IOException
	 */
	public static Resource createResource(ZipEntry zipEntry,
			ZipInputStream zipInputStream) throws IOException {

		return new Resource(zipInputStream, zipEntry.getName());

	}

	/**
	 * Creates a resource out of the given zipEntry and zipInputStream.
	 * 
	 * @param zipEntry
	 * @param InputStream
	 * @return
	 * @throws IOException
	 */
	public static Resource createResource(ZipEntry zipEntry,
			InputStream inputStream) throws IOException {

		return new Resource(inputStream, zipEntry.getName());

	}

	public static Resource getResourceFromPed(String pedFilePath,
			String resourceHref) throws IOException {

		ZipFile file = new ZipFile(pedFilePath);
		ZipEntry zipEntry = file.getEntry(resourceHref);
		InputStream inp = file.getInputStream(zipEntry);
		return createResource(zipEntry, inp);

	}

	/**
	 * Gets the contents of the Resource as an InputSource in a null-safe
	 * manner.
	 * 
	 */
	public static InputSource getInputSource(Resource resource)
			throws IOException {
		if (resource == null) {
			return null;
		}
		Reader reader = resource.getReader();
		if (reader == null) {
			return null;
		}
		InputSource inputSource = new InputSource(reader);
		return inputSource;
	}

	/**
	 * Reads parses the xml therein and returns the result as a Document
	 */
	// public static Document getAsDocument(Resource resource, EpubProcessor
	// epubProcessor) throws UnsupportedEncodingException, SAXException,
	// IOException, ParserConfigurationException {
	// return getAsDocument(resource, epubProcessor.createDocumentBuilder());
	// }

	/**
	 * Reads the given resources inputstream, parses the xml therein and returns
	 * the result as a Document
	 * 
	 * @param resource
	 * @param documentBuilderFactory
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	// public static Document getAsDocument(Resource resource, DocumentBuilder
	// documentBuilder) throws UnsupportedEncodingException, SAXException,
	// IOException, ParserConfigurationException {
	// InputSource inputSource = getInputSource(resource);
	// if (inputSource == null) {
	// return null;
	// }
	// Document result = documentBuilder.parse(inputSource);
	// result.setXmlStandalone(true);
	// return result;
	// }

}
