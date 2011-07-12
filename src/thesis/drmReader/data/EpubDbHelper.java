package thesis.drmReader.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EpubDbHelper extends SQLiteOpenHelper {

	private static final String DEBUG_TAG = "EpubDatabase";
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "epubLibrary";

	private static final String CREATE_TABLE_EPUBS = "CREATE TABLE "
			+ EpubsTable.tableName + " ( " + EpubsTable.id
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + EpubsTable.coverURL
			+ " TEXT, " + EpubsTable.title + " TEXT NOT NULL, "
			+ EpubsTable.filename + " TEXT NOT NULL, "
			+ EpubsTable.coverData + " BLOB, "
			+ EpubsTable.subject + " TEXT, " + EpubsTable.description
			+ " TEXT," + EpubsTable.lang + " TEXT );  ";

	private static final String CREATE_TABLE_AUTHORS = "CREATE TABLE "
			+ AuthorsTable.tableName + " ( " + AuthorsTable.id
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + AuthorsTable.firstname
			+ " TEXT NOT NULL," + AuthorsTable.lastname + " TEXT NOT NULL );";

	private static final String CREATE_TABLE_PUBLISHERS = "CREATE TABLE "
			+ PublishersTable.tableName + " ( " + PublishersTable.id
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + PublishersTable.firstname
			+ " TEXT NOT NULL ); ";

	private static final String CREATE_TABLE_EPUB_AUTHORS = "CREATE TABLE "
			+ EpubAuthorsTable.tableName + " ( " + EpubAuthorsTable.epubID
			+ " INTEGER REFERENCES " + EpubsTable.tableName + "(" + EpubsTable.id + "),"
			+ EpubAuthorsTable.authorID + " INTEGER REFERENCES " +AuthorsTable.tableName+ "("
			+ AuthorsTable.id + ")" + " ,PRIMARY KEY(" + EpubAuthorsTable.epubID
			+ ", " + EpubAuthorsTable.authorID + ") ); ";

	private static final String CREATE_TABLE_EPUB_PUBLISHERS = "CREATE TABLE "
			+ EpubPublishersTable.tableName + " ( "
			+ EpubPublishersTable.epubID + " INTEGER REFERENCES " + EpubsTable.tableName + "("
			+ EpubsTable.id + ")," + EpubPublishersTable.publisherID
			+ " INTEGER REFERENCES " + PublishersTable.tableName + "(" + PublishersTable.id + ")"
			+ " ,PRIMARY KEY(" + EpubPublishersTable.epubID + ","
			+ EpubPublishersTable.publisherID + ") ); ";


	public EpubDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	public EpubDbHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_EPUBS);
		db.execSQL(CREATE_TABLE_AUTHORS);
		db.execSQL(CREATE_TABLE_PUBLISHERS);
		db.execSQL(CREATE_TABLE_EPUB_AUTHORS);
		db.execSQL(CREATE_TABLE_EPUB_PUBLISHERS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DEBUG_TAG,
				"Upgrading database. Existing contents will be lost. ["
						+ oldVersion + "]->[" + newVersion + "]");
		db.execSQL("DROP TABLE IF EXISTS " + EpubPublishersTable.tableName
				+ "; " + "DROP TABLE IF EXISTS " + EpubAuthorsTable.tableName
				+ "; " + "DROP TABLE IF EXISTS " + PublishersTable.tableName
				+ "; " + "DROP TABLE IF EXISTS " + AuthorsTable.tableName
				+ "; " + "DROP TABLE IF EXISTS " + EpubsTable.tableName + ";");
		onCreate(db);

	}

	public class EpubsTable {
		final static String tableName = "epubs";
		final static String id = "_id";
		final static String coverURL = "coverURL";
		final static String title = "title";
		final static String subject = "subject";
		final static String description = "description";
		final static String lang = "lang";
		final static String filename = "filename";
		final static String coverData = "coverData";
	}

	public class AuthorsTable {
		final static String tableName = "authors";
		final static String id = "_id";
		final static String firstname = "firstname";
		final static String lastname = "lastname";
	}

	public class PublishersTable {
		final static String tableName = "publishers";
		final static String id = "_id";
		final static String firstname = "firstname";
	}

	public class EpubPublishersTable {
		final static String tableName = "epubPublishers";
		final static String epubID = "_epubId";
		final static String publisherID = "_publisherId";
	}

	public class EpubAuthorsTable {
		final static String tableName = "epubAuthors";
		final static String epubID = "_epubId";
		final static String authorID = "_authorId";
	}

}
