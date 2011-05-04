package thesis.pedlib.ped;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.pedlib.util.MediaType;
import thesis.pedlib.util.MediaTypeService;
import android.util.Log;

public class DocumentReader {

	private static final String ITEM = "item";
	private static final String ID = "id";
	private static final String HREF = "href";
	private static final String MEDIATYPE = "media-type";

	public static void read(Resource packageResource, Document doc,
			Map<String, Resource> resourcesByHref){
		String packageHref = packageResource.getHref();
		resourcesByHref = fixHrefs(packageHref, resourcesByHref);

		// Docs sometimes use non-identifier ids. We map these here to legal
		// ones
		Map<String, String> idMapping = new HashMap<String, String>();

		Resources resources = readManifest(packageResource, resourcesByHref,
				idMapping);
		doc.setResources(resources);
		readCover(packageResource, doc);//TODO:implement it
		doc.setMetadata(DocumentMetadataReader.read(packageResource));
		doc.setTOC(DocumentTOCReader.read(packageResource));

		// if we did not find a cover page then we make the first page of the
		// book the cover page
		
		//TODO:IMPLEMENT IT
		/*
		if (doc.getCoverPage() == null && doc.getSpine().size() > 0) {
			doc.setCoverPage(doc.getSpine().getResource(0));
		}
		*/
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
				resource.setHref(href.substring(lastSlashPos + 1));
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
			parser.setInput(packageResource.getInputStream(), null);

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
							// log.error(e.getMessage());
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
					break;
				}
				type = parser.next();
			}
		} catch (Exception ex) {
			Log.e("TOCParser", "Exception parsing toc", ex);
		}

		return result;
	}
	
	/**
	 * Find all resources that have something to do with the coverpage and the cover image.
	 * Search the meta tags and the guide references
	 * 
	 * @param packageDocument
	 * @return
	 */
	// package
	static Set<String> findCoverHrefs(Resource packageResource) {
		
		Set<String> result = new HashSet<String>();
		/*******************
		
		// try and find a meta tag with name = 'cover' and a non-blank id
		String coverResourceId = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
											OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover,
											OPFAttributes.content);

		if (StringUtils.isNotBlank(coverResourceId)) {
			String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
					OPFTags.item, OPFAttributes.id, coverResourceId,
					OPFAttributes.href);
			if (StringUtils.isNotBlank(coverHref)) {
				result.add(coverHref);
			} else {
				result.add(coverResourceId); // maybe there was a cover href put in the cover id attribute
			}
		}
		// try and find a reference tag with type is 'cover' and reference is not blank
		String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
											OPFTags.reference, OPFAttributes.type, OPFValues.reference_cover,
											OPFAttributes.href);
		if (StringUtils.isNotBlank(coverHref)) {
			result.add(coverHref);
		}
		
		****************************/
		return result;
	}
	
	/**
	 * Finds the cover resource in the packageDocument and adds it to the book if found.
	 * Keeps the cover resource in the resources map
	 * @param packageResource
	 * @param document
	 * @param resources
	 * @return
	 */
	private static void readCover(Resource packageResource, Document doc) {
		
		Collection<String> coverHrefs = findCoverHrefs(packageResource);
		for (String coverHref: coverHrefs) {
			Resource resource = doc.getResources().getByHref(coverHref);
			if (resource == null) {
				//log.error("Cover resource " + coverHref + " not found");
				continue;
			}
			if (resource.getMediaType() == MediaTypeService.XHTML) {
				//doc.setCoverPage(resource);
			} else if (MediaTypeService.isBitmapImage(resource.getMediaType())) {
				//doc.setCoverImage(resource);
			}
		}
	}

}
