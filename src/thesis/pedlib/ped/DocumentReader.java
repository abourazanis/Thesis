package thesis.pedlib.ped;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.DocumentLink;
import thesis.pedlib.util.MediaType;
import thesis.pedlib.util.MediaTypeService;
import thesis.pedlib.util.ResourceUtil;
import android.util.Log;

public class DocumentReader {

	private static final String ITEM = "item";
	private static final String ID = "id";
	private static final String HREF = "href";
	private static final String MEDIATYPE = "media-type";

	public static void read(Resource packageResource, Document doc,
			Map<String, Resource> resourcesByHref) {
		String packageHref = packageResource.getHref();
		resourcesByHref = fixHrefs(packageHref, resourcesByHref);

		// Docs sometimes use non-identifier ids. We map these here to legal
		// ones
		Map<String, String> idMapping = new HashMap<String, String>();

		Resources resources = readManifest(packageResource, resourcesByHref,
				idMapping);
		doc.setResources(resources);
		doc.setMetadata(DocumentMetadataReader.read(packageResource));
		readCover(packageResource, doc);
		doc.setTOC(DocumentTOCReader.read(packageResource));

	}

	public static void getPreview(Resource packageResource, DocumentLink doc) {

		Metadata meta = DocumentMetadataReader.read(packageResource);
		doc.setAuthor(meta.getAuthors().get(0));
		doc.setSubject(meta.getSubject());
		doc.setTitle(meta.getTitle());
		readCover(packageResource, doc);
	}

	/**
	 * Strips off the package prefixes up to the href of the packageHref.
	 * 
	 * Example: If the packageHref is "DOC/document.xml" then a resource href
	 * like "DOC/foo/bar.html" will be turned into "foo/bar.html"
	 * 
	 * @param packageHref
	 * @param resourcesByHref
	 * @return
	 */
	private static Map<String, Resource> fixHrefs(String packageHref,
			Map<String, Resource> resourcesByHref) {
		int lastSlashPos = packageHref.lastIndexOf('/');
		if (lastSlashPos < 0) {
			return resourcesByHref;
		}
		Map<String, Resource> result = new HashMap<String, Resource>();
		for (Resource resource : resourcesByHref.values()) {
			String href = resource.getHref();
			if ((href != null && href != "") || href.length() > lastSlashPos) {
				href = href.substring(lastSlashPos + 1);
				resource.setHref(href);
			}
			result.put(href, resource);
		}
		return result;
	}

	/**
	 * Reads the manifest containing the resource ids, hrefs and mediatypes.
	 * 
	 * @param packageDocument
	 * @param packageHref
	 * @param epubReader
	 * @param book
	 * @param resourcesByHref
	 * @return a Map with resources, with their id's as key.
	 */
	private static Resources readManifest(Resource packageResource,
			Map<String, Resource> resourcesByHref, Map<String, String> idMapping) {

		Resources result = new Resources();
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			// parser.setInput(packageResource.getInputStream(), "UTF-8");
			parser.setInput(packageResource.getReader());

			int type = parser.next();
			String name, id, href, mediaTypeName;
			id = name = href = mediaTypeName = "";
			boolean done = false;

			while (type != XmlPullParser.END_DOCUMENT && !done) {
				switch (type) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(ITEM)) {
						String namespace = parser.getAttributeNamespace(0);
						id = parser.getAttributeValue(namespace, ID);
						href = parser.getAttributeValue(namespace, HREF);
						mediaTypeName = parser.getAttributeValue(namespace,
								MEDIATYPE);
						try {
							href = URLDecoder.decode(href, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							Log.e("UnsupportedEncodingException",
									e.getMessage());
						}
					}
					break;
				case XmlPullParser.END_TAG:
					Resource resource = resourcesByHref.remove(href);
					if (resource != null) {
						resource.setId(id);
						MediaType mediaType = MediaTypeService
								.getMediaTypeByName(mediaTypeName);
						if (mediaType != null) {
							resource.setMediaType(mediaType);
						}
						result.add(resource);
						idMapping.put(id, resource.getId());
					}
					if (parser.getName().equalsIgnoreCase("manifest"))
						done = true;
					break;
				}
				type = parser.next();
			}
		} catch (Exception ex) {
			Log.e("Exception parsing manifest", ex.getMessage());
		}

		return result;
	}

	/**
	 * Find all resources that have something to do with the cover image. Search
	 * the meta tags
	 * 
	 * @param packageResource
	 * @return
	 */
	static Set<String> findCoverHrefs(Resource packageResource) {

		Set<String> result = new HashSet<String>();

		// try and find a meta tag with name = 'cover' and a non-blank id
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(packageResource.getReader());

			int type;
			String name = "";
			boolean done = false;
			boolean valid = false;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
					&& !done) {

				if (type == XmlPullParser.START_TAG) {
					name = parser.getName();
					if (name.equals("cover")) {
						valid = true;
					} else {
						valid = false;
					}
				} else if (type == XmlPullParser.TEXT && valid) {
					String text = parser.getText();
					if (name.equals("cover")) {
						result.add(text);
					}
					valid = false;
				} else if (type == XmlPullParser.END_TAG) {
					if (parser.getName().equalsIgnoreCase("metadata"))
						done = true;
				}
			}

		} catch (Exception ex) {
			Log.e("DocumentReader Exception parsing cover", ex.getMessage());
		}

		return result;
	}

	/**
	 * Finds the cover resource in the packageResource and adds it to the doc
	 * if found. Keeps the cover resource in the resources map
	 * 
	 * @param packageResource
	 * @param doc
	 * @param pedFilePath
	 * @return
	 */
	private static void readCover(Resource packageResource, DocumentLink doc) {

		Collection<String> coverHrefs = findCoverHrefs(packageResource);
		for (String coverHref : coverHrefs) {
			Resource resource = null;
			try {
				resource = ResourceUtil.getResourceFromPed(doc.getId(), "DOC/"
						+ coverHref);
			} catch (IOException e) {
				Log.e("readCover", e.getMessage());
			}
			if (resource == null) {
				continue;
			}

			if (MediaTypeService.isBitmapImage(resource.getMediaType())) {
				doc.setCoverResource(resource);
				doc.setCoverUrl(resource.getHref());
			}
		}
	}
	
	private static void readCover(Resource packageResource, Document doc) {
		
		Collection<String> coverHrefs = findCoverHrefs(packageResource);
		for (String coverHref: coverHrefs) {
			Resource resource = doc.getResources().getByHref(coverHref);
			if (resource == null) {
				continue;
			}
			if (MediaTypeService.isBitmapImage(resource.getMediaType())) {
				doc.setCoverImage(resource);
			}
		}
	}

}
