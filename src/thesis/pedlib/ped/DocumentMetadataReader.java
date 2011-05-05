package thesis.pedlib.ped;

import java.util.Arrays;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;


public class DocumentMetadataReader {
	
	private static final String TITLE = "title";
	private static final String LANGUAGE = "language";
	private static final String AUTHOR = "author";
	private static final String DESCRIPTION = "description";
	private static final String SUBJECT = "subject";
	private static final String DATE = "date";
	private static final String CUSTMETA = "custmeta";
	private static final String[] entries = {
            TITLE, LANGUAGE, AUTHOR, DESCRIPTION, SUBJECT, DATE, CUSTMETA
        };
	
	private static List<String> m_ValidEntries = Arrays.asList(entries);
	
    
	static public Metadata read(Resource packageResource){
		Metadata meta = new Metadata();
    	try{
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser parser = factory.newPullParser();
    		parser.setInput(packageResource.getInputStream(), null);

    		int type;
    		String name = "";
    		boolean valid = false;
    		boolean done = false;
    		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && !done) {
    			
    			if (type == XmlPullParser.START_TAG) {
    				name = parser.getName();
                    if (m_ValidEntries.contains(name)) {
                        valid = true;
                    } else {
                        valid = false;
                    }
    			}
                    else if (type == XmlPullParser.TEXT && valid) {
                        String text = parser.getText();
                        if (name.equals(TITLE)) {
                            meta.setTitle(text);
                        } else if (name.equals(LANGUAGE)) {
                            meta.setLanguage(text);
                        } else if (name.equals(AUTHOR)) {
                            meta.addAuthor(text);
                        } else if (name.equals(DESCRIPTION)) {
                            meta.setDescription(text);
                        } else if (name.equals(SUBJECT)) {
                            meta.setSubject(text);
                        } else if (name.equals(DATE)) {
                            meta.setDate(text);
                        } else if (name.equals(CUSTMETA)) {
                        	String namespace = parser.getAttributeNamespace(0);
                        	String metaName  = parser.getAttributeValue(namespace, "metaName");
                        	String metaValue = parser.getAttributeValue(namespace, "metaValue");
                        	meta.addOtherMeta(metaName, metaValue);
                        }
                        valid = false;
                    }
    			else if(type == XmlPullParser.END_TAG){
    				if(parser.getName().equalsIgnoreCase("metadata"))
    					done = true;
    			}
    		}
    		
    	}catch (Exception ex) {
            Log.e("MetadataReader Exception parsing metadata", ex.getMessage());
        }
		
		return meta;
	}
}
