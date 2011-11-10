package thesis.drmReader.db;

import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.EpubSearchColumns;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.db.EpubsContract.EpubsColumns;
import thesis.drmReader.db.EpubsContract.Publishers;
import thesis.drmReader.ui.EpubsSearchActivity.SearchQuery;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DrmReaderDataBase extends SQLiteOpenHelper {

	private static final String TAG = "DrmReaderDataBase";
	public static final String DATABASE_NAME = "epubdatabase";
	public static final int DATABASE_VERSION = 1;

	public interface Tables {
		String EPUBS = "epubs";
		String AUTHORS = "authors";
		String PUBLISHERS = "publishers";

		String EPUBS_JOIN_AUTHORS = "epubs "
				+ "LEFT OUTER JOIN authors ON epubs.authors_id=authors._id";

		String EPUBS_JOIN_PUBLISHERS = "epubs "
				+ "LEFT OUTER JOIN publishers ON epubs.publishers_id=publishers._id";

		String EPUBS_JOIN_PUBLISHERS_AUTHORS = "epubs "
				+ "LEFT OUTER JOIN authors ON epubs.authors_id=authors._id "
				+ "LEFT OUTER JOIN publishers ON epubs.publishers_id=publishers._id";

		// String EPUBS_AUTHORS = "epubs_authors";
		// String EPUBS_PUBLISHERS = "epubs_publishers";
		//
		String EPUBS_SEARCH = "epubs_search";

	}

	interface References {
		String AUTHOR_ID = "REFERENCES " + Tables.AUTHORS + "("
				+ BaseColumns._ID + ")";

		String PUBLISHER_ID = "REFERENCES " + Tables.PUBLISHERS + "("
				+ BaseColumns._ID + ")";
	}

	/**
	 * table creation queries
	 */
	private static final String CREATE_TABLE_EPUBS = "CREATE TABLE "
			+ Tables.EPUBS + " ( " + Epubs._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + Epubs.TITLE
			+ " TEXT NOT NULL, " + Epubs.FILENAME + " TEXT NOT NULL, "
			+ Epubs.COVERDATA + " BLOB, " + EpubsColumns.REF_AUTHOR_ID
			+ " TEXT " + References.AUTHOR_ID + ","
			+ EpubsColumns.REF_PUBLISHER_ID + " TEXT "
			+ References.PUBLISHER_ID + "," + Epubs.SUBJECT + " TEXT, "
			+ Epubs.DESCRIPTION + " TEXT," + Epubs.LANGUAGE + " TEXT );  ";

	private static final String CREATE_TABLE_AUTHORS = "CREATE TABLE "
			+ Tables.AUTHORS + " ( " + Authors._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + Authors.FIRSTNAME
			+ " TEXT NOT NULL," + Authors.LASTNAME + " TEXT NOT NULL );";

	private static final String CREATE_TABLE_PUBLISHERS = "CREATE TABLE "
			+ Tables.PUBLISHERS + " ( " + Publishers._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + Publishers.FIRSTNAME
			+ " TEXT NOT NULL ); ";

	private static final String CREATE_SEARCH_TABLE = "CREATE VIRTUAL TABLE "
			+ Tables.EPUBS_SEARCH + " USING FTS3(" + EpubSearchColumns.TITLE
			+ " TEXT," + EpubSearchColumns.DESCRIPTION + " TEXT,"
			+ EpubSearchColumns.SUBJECT + " TEXT" + ");";

	// private static final String CREATE_TABLE_EPUB_AUTHORS = "CREATE TABLE "
	// + Tables.EPUBS_AUTHORS + " ( " + EpubAuthorsColumns.EPUB_ID
	// + " INTEGER REFERENCES " + Tables.EPUBS + "(" + Epubs._ID + "),"
	// + EpubAuthorsColumns.AUTHOR_ID + " INTEGER REFERENCES " +Tables.AUTHORS+
	// "("
	// + Authors._ID + ")" + " ,PRIMARY KEY(" + EpubAuthorsColumns.EPUB_ID
	// + ", " + EpubAuthorsColumns.AUTHOR_ID + ") ); ";
	//
	// private static final String CREATE_TABLE_EPUB_PUBLISHERS =
	// "CREATE TABLE "
	// + Tables.EPUBS_PUBLISHERS + " ( "
	// + EpubPublishersColumns.EPUB_ID + " INTEGER REFERENCES " + Tables.EPUBS +
	// "("
	// + Epubs._ID + ")," + EpubPublishersColumns.PUBLISHER_ID
	// + " INTEGER REFERENCES " + Tables.PUBLISHERS + "(" + Publishers._ID + ")"
	// + " ,PRIMARY KEY(" + EpubPublishersColumns.EPUB_ID + ","
	// + EpubPublishersColumns.PUBLISHER_ID + ") ); ";

	public DrmReaderDataBase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_EPUBS);
		db.execSQL(CREATE_TABLE_AUTHORS);
		db.execSQL(CREATE_TABLE_PUBLISHERS);
		db.execSQL(CREATE_SEARCH_TABLE);
		// db.execSQL(CREATE_TABLE_EPUB_AUTHORS);
		// db.execSQL(CREATE_TABLE_EPUB_PUBLISHERS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database. Existing contents will be lost. ["
				+ oldVersion + "]->[" + newVersion + "]");
		// db.execSQL("DROP TABLE IF EXISTS " + Tables.EPUBS_PUBLISHERS
		// + "; " + "DROP TABLE IF EXISTS " + Tables.EPUBS_AUTHORS
		// + "; " + "DROP TABLE IF EXISTS " + Tables.PUBLISHERS
		// + "; " + "DROP TABLE IF EXISTS " + Tables.AUTHORS
		// + "; " + "DROP TABLE IF EXISTS " + Tables.EPUBS + ";");
		db.execSQL("DROP TABLE IF EXISTS " + Tables.PUBLISHERS + "; "
				+ "DROP TABLE IF EXISTS " + Tables.AUTHORS + "; "
				+ "DROP TABLE IF EXISTS " + Tables.EPUBS + ";");
		onCreate(db);
	}

	public static void onRenewFTSTable(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			db.execSQL("drop table if exists " + Tables.EPUBS_SEARCH);
			db.execSQL(CREATE_SEARCH_TABLE);
			db.execSQL("INSERT INTO " + Tables.EPUBS_SEARCH + "(docid,"
					+ Epubs.TITLE + "," + Epubs.DESCRIPTION + ","
					+ Epubs.SUBJECT + ")" + " select " + Epubs._ID + ","
					+ Epubs.TITLE + "," + Epubs.DESCRIPTION + ","
					+ Epubs.SUBJECT + " from " + Tables.EPUBS + ";");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public static Cursor search(String query, SQLiteDatabase db) {

		/*
		 * select _id,
		 * epubdescription,title,subject,lang,firstname,lastname,publisherName
		 * from (select rowid, snippet(epubs_search) as epubdescription from
		 * epubs_search where epubs_search match "Iron") join (select
		 * _id,title,subject,description,lang,authors_id,publishers_id from
		 * epubs) on _id = rowid left join (select _id as authID,
		 * firstname,lastname from authors) on authors_id = authID left join
		 * (select _id as pubID, name as publisherName from publishers) on
		 * publishers_id = pubID
		 */

		return db.rawQuery("select " + SearchQuery._ID + "," + SearchQuery.DESCRIPTION + ","
				+ SearchQuery.TITLE + "," + SearchQuery.SUBJECT + "," + SearchQuery.LANGUAGE
				+ "," + SearchQuery.AUTH_FIRSTNAME + "," + SearchQuery.AUTH_LASTNAME + "," + SearchQuery.PUBLISHER + " from " + " (select rowid, snippet("
				+ Tables.EPUBS_SEARCH + ") as " + SearchQuery.DESCRIPTION + " from "
				+ Tables.EPUBS_SEARCH + " where " + Tables.EPUBS_SEARCH
				+ " match ?)" + " join (select  " + Epubs._ID + ","
				+ Epubs.TITLE + "," + Epubs.SUBJECT + "," + Epubs.DESCRIPTION
				+ "," + Epubs.LANGUAGE + "," + Epubs.REF_AUTHOR_ID + ","
				+ Epubs.REF_PUBLISHER_ID + " from " + Tables.EPUBS + "  )"
				+ " on " + Epubs._ID + " = rowid" + " left join" + " (select "
				+ Authors._ID + " as authID," + Authors.FIRSTNAME + ","
				+ Authors.LASTNAME + " from " + Tables.AUTHORS + ")" + " on "
				+ Epubs.REF_AUTHOR_ID + " = authID " + " left join  (select "
				+ Publishers._ID + " as pubID," + Publishers.FIRSTNAME
				+ " as publisherName from " + Tables.PUBLISHERS + " )" + " on "
				+ Epubs.REF_PUBLISHER_ID + " = pubID", new String[] { "\""
				+ query + "*\"" });
	}

	public static Cursor getSuggestions(String query, SQLiteDatabase db) {
		return db.rawQuery("select " + Epubs._ID + ",epubdescription,"
				+ Epubs.TITLE + " as "
		                + SearchManager.SUGGEST_COLUMN_TEXT_1 + "," + Epubs.SUBJECT + "," + Epubs.LANGUAGE
				+ "," + Authors.FIRSTNAME + "," + Authors.LASTNAME + " , "
				+ Authors.FIRSTNAME + " ||  ' ' || " + Authors.LASTNAME + " as " + SearchManager.SUGGEST_COLUMN_TEXT_2 + " ,"
				+ Epubs._ID  +" as "
		                + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID + " from " + " (select rowid, snippet("
				+ Tables.EPUBS_SEARCH + ") as epubdescription" + " from "
				+ Tables.EPUBS_SEARCH + " where " + Tables.EPUBS_SEARCH
				+ " match ?)" + " join (select  " + Epubs._ID + ","
				+ Epubs.TITLE + "," + Epubs.SUBJECT + "," + Epubs.DESCRIPTION
				+ "," + Epubs.LANGUAGE + "," + Epubs.REF_AUTHOR_ID + " from " + Tables.EPUBS + "  )"
				+ " on " + Epubs._ID + " = rowid" + " left join" + " (select "
				+ Authors._ID + " as authID," + Authors.FIRSTNAME + ","
				+ Authors.LASTNAME + " from " + Tables.AUTHORS + ")" + " on "
				+ Epubs.REF_AUTHOR_ID + " = authID ", new String[] { "\"" + query + "*\"" });
	}

}
