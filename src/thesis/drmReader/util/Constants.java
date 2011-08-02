package thesis.drmReader.util;

public class Constants {
	
	public final static int LIST_DOCUMENTS = 0;
	public final static int IMPORT_DOCUMENT = 1;
	public static final int IMPORT_REQUEST = 100;
	
	public static final int IMPORT_DOCUMENT_ERROR = 101;
	public static final int IMPORT_DOCUMENT_FILENOTFOUND = 102;
	public static final int IMPORT_DOCUMENT_INVALIDKEY = 103;
	
	public final static int SORT_DIALOG = 150;
	
	public final static int DOWNLOAD_DOCUMENT = 2;
	public final static int DOWNLOAD_DOCLIST = 3;
	public final static int DOWNLOAD_DOCLIST_ALERT = 4;
	public final static int DOWNLOAD_DOCUMENT_ALERT = 5;
	
	public static final int OFFLINE = 201;
	public final static int HTTP_RESPONSE_OK = 202;
	
	
	//keys
	public final static String KEY_EPUBSORTORDERARCHIVE = "epubArchiveSorting"; 
	
	
	public final static String URL = "http://10.0.2.2:8080/thesis.server/rest/epubs";
	public final static String GETDOCURL = "http://10.0.2.2:8080/thesis.server/rest/epubs/";

}
