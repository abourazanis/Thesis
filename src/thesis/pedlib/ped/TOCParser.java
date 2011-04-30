package thesis.pedlib.ped;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class TOCParser {
	
	public static final String ENTRIES = "Entries";
	public static final String ITEM = "item";
	public static final String LABEL = "label";
	public static final String CONTENT = "content";
	
	public static final String ID = "id";
	public static final String ORDER = "order";
	public static final String SRC = "src";
	
	
	private Document doc;
	
	TOCParser(Document doc){
		this.doc = doc;
	}
	
	
	public TOC parse(){
		
		TOC toc = null;
    	File file = new File(doc.getPedPath());
    	
    	ZipFile zip = null;
    	try{
    		zip = new ZipFile(file);
    		
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser parser = factory.newPullParser();
    		ZipEntry entry = zip.getEntry(doc.getContainerPath());
            InputStream inp1 = zip.getInputStream(entry);
            parser.setInput(inp1, null);
    		int type = parser.getEventType();
    		
    		TOCEntry currentItem = null;
    		String name = "";
    		boolean done = false;
    		while (type != XmlPullParser.END_DOCUMENT && !done){
                switch (type){
                    case XmlPullParser.START_DOCUMENT:
                        toc = new TOC();
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(ITEM)){
                        	String namespace = parser.getAttributeNamespace(0);
                        	String id = parser.getAttributeValue(namespace, ID);
                        	int order = Integer.parseInt(parser.getAttributeValue(namespace, ORDER));
                            currentItem = new TOCEntry(id, order);
                        } else if (currentItem != null){
                            if (name.equalsIgnoreCase(LABEL)){
                                currentItem.setLabel(parser.nextText());
                            } else if (name.equalsIgnoreCase(CONTENT)){
                            	String namespace = parser.getAttributeNamespace(0);
                            	String src = parser.getAttributeValue(namespace, SRC);
                            	currentItem.setSrc(src);
                            }    
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase(ITEM) &&  currentItem != null){
                            toc.addItem(currentItem);
                        } else if (name.equalsIgnoreCase(ENTRIES)){
                            done = true;
                        }
                        break;
                }
                type = parser.next();
    		}
    	}catch (Exception ex) {
            Log.e("TOCParser", "Exception parsing toc", ex);
        }
		
		return toc;
	}

}
