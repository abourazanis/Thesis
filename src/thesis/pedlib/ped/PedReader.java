package thesis.pedlib.ped;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.pedlib.util.ResourceUtil;

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

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(containerResource.getInputStream(), null);

			int type;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
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
			}
		} catch (Exception e) {
			// logging
		}

		return result;
	}
	
	private Resource processPackageResource(String packageResourceHref, Document doc, Map<String, Resource> resources) {
		Resource packageResource = resources.remove(packageResourceHref);
		DocumentReader.read(packageResource, doc, resources);
		
		return packageResource;
	}

}