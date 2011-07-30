package thesis.drmReader.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class EpubsContract {
	
	interface EpubsColumns{
		final static String TITLE = "title";
		final static String SUBJECT = "subject";
		final static String DESCRIPTION = "description";
		final static String LANGUAGE = "lang";
		final static String FILENAME = "filename";
		final static String COVERDATA = "coverData";
		/** This column is NOT in this table, it is for reference purposes only. */
        final static String REF_AUTHOR_ID = "authors_id";
        /** This column is NOT in this table, it is for reference purposes only. */
        final static String REF_PUBLISHER_ID = "publishers_id";
	}
	
	interface AuthorsColumns{
		final static String FIRSTNAME = "firstname";
		final static String LASTNAME = "lastname";
	}
	
	interface PublishersColumns{
		final static String FIRSTNAME = "name";
	}
	
//	interface EpubPublishersColumns{
//		final static String EPUB_ID = "_epubId";
//		final static String PUBLISHER_ID = "_publisherId";	
//	}
//	
//	interface EpubAuthorsColumns{
//		final static String EPUB_ID = "_epubId";
//		final static String AUTHOR_ID = "_authorId";
//	}
	
	 interface EpubSearchColumns {
	        final static String _EPUBID = "epubid";
	        final static String TITLE = Epubs.TITLE;
	        final static String DESCRIPTION = Epubs.DESCRIPTION;
	        final static String SUBJECT = Epubs.SUBJECT;
	    }
	
	public static final String CONTENT_AUTHORITY = "thesis.drmreader";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_EPUBS = "epubs";
    public static final String PATH_AUTHORS = "authors";
    public static final String PATH_PUBLISHERS = "publishers";
    public static final String PATH_OFPUBLISHER = "ofpublisher";
    public static final String PATH_OFAUTHOR = "ofauthor";
    public static final String PATH_OFEPUB = "ofepub";
    
    public static final String PATH_EPUBSEARCH = "epubsearch";
    public static final String PATH_SEARCH = "search";
    
    public static final String PATH_RENEWFTSTABLE = "renewftstable";
    
    public static class Epubs implements BaseColumns,EpubsColumns{
    	public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_EPUBS)
                .build();
    	
    	/** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.drmreader.epub";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.drmreader.epub";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = EpubsColumns.TITLE + " ASC";

        public static Uri buildEpubUri(String epubId) {
            return CONTENT_URI.buildUpon().appendPath(epubId).build();
        }
        
        public static Uri buildEpubsOfAuthorUri(String authorId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFAUTHOR).appendPath(authorId).build();
        }
        
        public static Uri buildEpubsOfPublisherUri(String publisherId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFPUBLISHER).appendPath(publisherId).build();
        }

        public static String getEpubId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }
    
    public static class Authors implements BaseColumns,AuthorsColumns{
    	public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUTHORS)
                .build();
    	
    	/** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.drmreader.author";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.drmreader.author";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = AuthorsColumns.LASTNAME + " ASC";

        public static Uri buildAuthorUri(String authorId) {
            return CONTENT_URI.buildUpon().appendPath(authorId).build();
        }
        
        public static Uri buildAuthorsOfEpubUri(String epubId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFEPUB).appendPath(epubId).build();
        }

        public static String getAuthorId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }
    
    public static class Publishers implements BaseColumns,PublishersColumns{
    	public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PUBLISHERS)
                .build();
    	
    	/** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.drmreader.publisher";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.drmreader.publisher";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = PublishersColumns.FIRSTNAME + " ASC";

        public static Uri buildPublisherUri(String publisherId) {
            return CONTENT_URI.buildUpon().appendPath(publisherId).build();
        }
        
        public static Uri buildPublishersOfEpubUri(String epubId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFEPUB).appendPath(epubId).build();
        }

        public static String getPublisherId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }
    
    public static class EpubSearch implements EpubSearchColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPUBSEARCH).build();

        public static final Uri CONTENT_URI_SEARCH = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPUBSEARCH).appendPath(PATH_SEARCH).build();
        
        public static final Uri CONTENT_URI_RENEWFTSTABLE = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_RENEWFTSTABLE).build();

        public static Uri buildEpubIdUri(String rowId) {
            return CONTENT_URI.buildUpon().appendPath(rowId).build();
        }

        public static String getEpubId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }
    
    private EpubsContract(){}

}
