package thesis.pedlib.ped;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.pedlib.util.ResourceUtil;
import android.util.Log;


public class PedReader {

	public Document readPed(ZipInputStream in, String encoding)
			throws IOException {
		Document result = new Document();
		Map<String, Resource> resources = readResources(in, encoding);
		handleMimeType(result, resources);
		String packageResourceHref = getPackageResourceHref(result, resources);
		Resource packageResource = processPackageResource(packageResourceHref,
				result, resources);
		result.setDocResource(packageResource);

		return result;
	}

	public Document getPedPreview(String pedFilePath, String encoding) {
		Document result = new Document();
		String packageResourceHref = getPackageResourceHref(pedFilePath);
		Resource packageResource = processPackageResource(packageResourceHref,pedFilePath,
				result);
		result.setDocResource(packageResource);

		return result;
	}

	private Map<String, Resource> readResources(ZipInputStream in,
			String defaultHtmlEncoding) throws IOException {
		Map<String, Resource> result = new HashMap<String, Resource>();
		for (ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in
				.getNextEntry()) {
			if (zipEntry.isDirectory()) {
				continue;
			}
			Resource resource = ResourceUtil.createResource(zipEntry, in);
			result.put(resource.getHref(), resource);
		}
		return result;
	}

	private void handleMimeType(Document result, Map<String, Resource> resources) {
		resources.remove("mimetype");
	}

	private String getPackageResourceHref(Document doc,
			Map<String, Resource> resources) {
		String defaultResult = "DOC/document.xml";
		String result = defaultResult;

		Resource containerResource = resources.remove("META-INF/container.xml");
		if (containerResource == null) {
			return result;
		}

		try {
			result = getPackageHref(containerResource.getReader());
		} catch (IOException e) {
			Log.e("reader", e.getMessage());
		}

		return result;
	}

	private String getPackageResourceHref(String pedFilePath) {

		String defaultResult = "DOC/document.xml";
		String result = defaultResult;

		try {
			Resource resource = ResourceUtil.getResourceFromPed(pedFilePath,"META-INF/container.xml");
			if(resource == null){
				return result;
			}
			result = getPackageHref(resource.getReader());
		} catch (IOException e) {
			Log.e("reader", e.getMessage());
		}

		return result;
	}

	private String getPackageHref(Reader reader) {
		String defaultResult = "DOC/document.xml";
		String result = defaultResult;

		try {

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(reader);

			int type = parser.next();
			while (type != XmlPullParser.END_DOCUMENT) {
				if (type == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if ("rootfile".equalsIgnoreCase(name)) {
						String nameSpace = parser.getAttributeNamespace(0);
						String value = parser.getAttributeValue(nameSpace,
								"full-path");
						if (value != null && value != "")
							result = value;
						break;
					}
				}
				type = parser.next();
			}
		} catch (Exception e) {
			Log.e("PedReader getPackageResourceHref", e.getMessage());
		}

		return result;
	}

	private Resource processPackageResource(String packageResourceHref,
			Document doc, Map<String, Resource> resources) {
		Resource packageResource = resources.remove(packageResourceHref);
		DocumentReader.read(packageResource, doc, resources);

		return packageResource;
	}

	private Resource processPackageResource(String packageResourceHref,String pedFilePath,
			Document doc) {
		Resource packageResource = null;
		try{
		packageResource = ResourceUtil.getResourceFromPed(pedFilePath,packageResourceHref);
		DocumentReader.getPreview(packageResource, doc, pedFilePath);

		}catch(Exception e){
			Log.e("processPackageResource",e.getMessage());
		}
		
		return packageResource;
	}

}
