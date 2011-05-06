package thesis.pedlib.ped;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class DocumentTOCReader {

	public static final String ENTRIES = "entries";
	public static final String ITEM = "item";
	public static final String LABEL = "label";
	public static final String CONTENT = "content";

	public static final String ID = "id";
	public static final String ORDER = "order";
	public static final String SRC = "src";

	static public TOC read(Resource packageResource) {

		TOC toc = null;
		try {

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();

			parser.setInput(packageResource.getInputStream(), null);
			int type = parser.getEventType();

			TOCEntry currentItem = null;
			String name = "";
			boolean done = false;
			boolean isInTOC = false;
			while (type != XmlPullParser.END_DOCUMENT && !done) {
				switch (type) {
				case XmlPullParser.START_DOCUMENT:
					toc = new TOC();
					break;
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(ENTRIES)) {
						isInTOC = true;
					}
						if (name.equalsIgnoreCase(ITEM) && isInTOC) {
							String namespace = parser.getAttributeNamespace(0);
							String id = parser.getAttributeValue(namespace, ID);
							String o = parser.getAttributeValue(namespace, ORDER);
							int order = Integer.parseInt(o);
							currentItem = new TOCEntry(id, order);
						} else if (currentItem != null) {
							if (name.equalsIgnoreCase(LABEL)) {
								currentItem.setLabel(parser.nextText());
							} else if (name.equalsIgnoreCase(CONTENT)) {
								String namespace = parser
										.getAttributeNamespace(0);
								String src = parser.getAttributeValue(
										namespace, SRC);
								currentItem.setSrc(src);
							}
						}
					
					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(ITEM) && currentItem != null) {
						toc.addItem(currentItem);
					} else if (name.equalsIgnoreCase(ENTRIES)) {
						done = true;
					}
					break;
				}
				type = parser.next();
			}
		} catch (Exception ex) {
			Log.e("Exception parsing toc", ex.getMessage());
		}

		return toc;
	}

}
