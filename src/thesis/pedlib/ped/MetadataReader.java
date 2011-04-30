package thesis.pedlib.ped;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class MetadataReader {
	
	public static final String TITLE = "title";
	public static final String LANGUAGE = "language";
    public static final String AUTHOR = "author";
    public static final String DESCRIPTION = "description";
    public static final String SUBJECT = "subject";
    public static final String DATE = "date";
    public static final String CUSTMETA = "custmeta";
    String[] entries = {
            TITLE, LANGUAGE, AUTHOR, DESCRIPTION, SUBJECT, DATE, CUSTMETA
        };
	
    private String pedFilePath;
    private String containerPath;
    private Metadata meta;
	private static List<String> m_ValidEntries;
    
	MetadataReader(String pedPath){
		pedFilePath = pedPath;
		if (m_ValidEntries == null) {
            m_ValidEntries = Arrays.asList(entries);
        }
	}
	
	Metadata getMetadata(){
		return meta;
	}
	
	String getContainerPath(){
		return containerPath;
	}
	
	public boolean read(){
		
		if(pedFilePath == null) return true;
    	
    	File file = new File(pedFilePath);
    	if(!file.exists() || !pedFilePath.toLowerCase().endsWith(".ped")) return false;
    	
    	ZipFile zip = null;
    	try{
    		zip = new ZipFile(file);
    		ZipEntry container = zip.getEntry("META-INF/container.xml");
    		if(container == null) return false;
    		InputStream inp = zip.getInputStream(container);
    		
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser parser = factory.newPullParser();
    		parser.setInput(inp, null);
    		
    		int type;
    		//find and read the rootfile value
    		//then set it as the next file to be parsed by the parser
    		while((type = parser.next()) != XmlPullParser.END_DOCUMENT){
    			if (type == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("rootfile".equalsIgnoreCase(name)) {
                        String nameSpace = parser.getAttributeNamespace(0);
                        String value = parser.getAttributeValue(nameSpace, "full-path");
                        if (value != null) {
                            ZipEntry entry = zip.getEntry(value);
                            InputStream inp1 = zip.getInputStream(entry);
                            parser.setInput(inp1, null);
                            containerPath = value;
                            break;
                        }
                    }
                }
    		}
    		
    		String name = "";
    		boolean valid = false;
    		while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
    			//pass the meta close tag
    			if (type == XmlPullParser.END_TAG) {
                    name = parser.getName();
                    if ("metadata".equalsIgnoreCase(name)) {
                        break;
                    }
                }
    			
    			if (type == XmlPullParser.START_TAG) {
    				name = parser.getName();
                    if (m_ValidEntries.contains(name)) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                    
                    if (type == XmlPullParser.TEXT && valid) {
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
    			}
    		}
    	}catch (Exception ex) {
            Log.e("MetadataReader", "Exception parsing metadata", ex);
            return false;
        }
		
		return true;
	}
}
