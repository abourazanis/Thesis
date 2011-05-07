package thesis.pedlib.util;

import java.util.HashMap;
import java.util.Map;

public class MediaTypeService {
	
	public static final MediaType XHTML = new MediaType("application/xhtml+xml", ".xhtml", new String[] {".htm", ".html", ".xhtml"});
	public static final MediaType XML = new MediaType("application/xml", ".xml");
	public static final MediaType PED = new MediaType("application/ped+zip", ".ped");
	public static final MediaType JPG = new MediaType("image/jpeg", ".jpg", new String[] {".jpg", ".jpeg"});
	public static final MediaType PNG = new MediaType("image/png", ".png");
	public static final MediaType GIF = new MediaType("image/gif", ".gif");
	public static final MediaType CSS = new MediaType("text/css", ".css");
	public static final MediaType SVG = new MediaType("image/svg+xml", ".svg");
	public static final MediaType TTF = new MediaType("application/x-truetype-font", ".ttf");
	public static final MediaType OPENTYPE = new MediaType("font/opentype", ".otf");
	
	public static MediaType[] mediatypes = new MediaType[] {
		XHTML, XML, PED, JPG, PNG, GIF, CSS, SVG, TTF, OPENTYPE
	};
	
	public static Map<String, MediaType> mediaTypesByName = new HashMap<String, MediaType>();
	static {
		for(int i = 0; i < mediatypes.length; i++) {
			mediaTypesByName.put(mediatypes[i].getName(), mediatypes[i]);
		}
	}
	
	public static boolean isBitmapImage(MediaType mediaType) {
		return mediaType == JPG || mediaType == PNG || mediaType == GIF;
	}
	
	/**
	 * Gets the MediaType based on the file extension.
	 * Null of no matching extension found.
	 * 
	 * @param filename
	 * @return
	 */
	public static MediaType determineType(String filename) {
		for(int i = 0; i < mediatypes.length; i++) {
			MediaType mediatype = mediatypes[i];
			for(String extension: mediatype.getExtensions()) {
				if(filename.toLowerCase().endsWith(extension)){
					return mediatype;
				}
			}
		}
		return null;
	}

	public static MediaType getMediaTypeByName(String mediaTypeName) {
		return mediaTypesByName.get(mediaTypeName);
	}

}
