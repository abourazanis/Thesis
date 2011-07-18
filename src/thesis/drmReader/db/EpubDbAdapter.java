package thesis.drmReader.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.util.ResourceUtil;
import thesis.drmReader.BookLink;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class EpubDbAdapter {

	private Context context;
	private EpubDbHelper dbHelper;
	private SQLiteDatabase database;

	public EpubDbAdapter(Context context) {
		this.context = context;
	}

	public EpubDbAdapter open() throws SQLException {
		dbHelper = new EpubDbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	public synchronized long createEpub(BookLink epub, String filePath, byte[] coverImage) {
		Metadata meta = epub.getMeta();
		ContentValues initialValues = createContentEpubValues(
				epub.getCoverUrl(), meta.getSubjects().get(0), meta
						.getDescriptions().get(0), meta.getLanguage(),
				meta.getFirstTitle(), filePath, coverImage);
		long epubID = database.insert(EpubDbHelper.EpubsTable.tableName, null,
				initialValues);

		for (Author auth : meta.getAuthors()) {
			long authID = createAuthor(auth.getFirstname(), auth.getLastname());
			ContentValues values = new ContentValues();
			values.put(EpubDbHelper.EpubAuthorsTable.epubID, epubID);
			values.put(EpubDbHelper.EpubAuthorsTable.authorID, authID);
			database.insert(EpubDbHelper.EpubAuthorsTable.tableName, null,
					values);
		}

		for (String publisher : meta.getPublishers()) {
			long pubID = createPublisher(publisher);
			ContentValues values = new ContentValues();
			values.put(EpubDbHelper.EpubPublishersTable.epubID, epubID);
			values.put(EpubDbHelper.EpubPublishersTable.publisherID, pubID);
			database.insert(EpubDbHelper.EpubPublishersTable.tableName, null,
					values);
		}

		return epubID;
	}
	
	
	public synchronized void deleteEpub(BookLink epub){
		Metadata meta = epub.getMeta();
		for (Author auth : meta.getAuthors()) {
			long authID = createAuthor(auth.getFirstname(), auth.getLastname());
			database.delete(EpubDbHelper.EpubAuthorsTable.tableName, EpubDbHelper.EpubAuthorsTable.authorID + " = " + authID + " AND " + EpubDbHelper.EpubAuthorsTable.epubID + " = " + epub.getId(), null);
		}

		for (String publisher : meta.getPublishers()) {
			long pubID = createPublisher(publisher);
			database.delete(EpubDbHelper.EpubPublishersTable.tableName, EpubDbHelper.EpubPublishersTable.publisherID + " = " + pubID + " AND " + EpubDbHelper.EpubPublishersTable.epubID + " = " + epub.getId(), null);
		}
		
		database.delete(EpubDbHelper.EpubsTable.tableName, EpubDbHelper.EpubsTable.id + " = " + epub.getId(), null);
		
	}

	public synchronized long createAuthor(String firstname, String lastname) {
		ContentValues values = new ContentValues();
		values.put(EpubDbHelper.AuthorsTable.firstname, firstname);
		values.put(EpubDbHelper.AuthorsTable.lastname, lastname);

		Cursor mCursor = database.query(EpubDbHelper.AuthorsTable.tableName,
				new String[] { EpubDbHelper.AuthorsTable.id,
						EpubDbHelper.AuthorsTable.firstname,
						EpubDbHelper.AuthorsTable.lastname },
				EpubDbHelper.AuthorsTable.firstname + " = '" + firstname + "' AND "
						+ EpubDbHelper.AuthorsTable.lastname + " = '" + lastname + "'",
				null, null, null, null);
		if (mCursor.moveToFirst() == false){
			mCursor.close();
			return database.insert(EpubDbHelper.AuthorsTable.tableName, null,
					values);
		}else {
			long res = mCursor.getLong(0);
			mCursor.close();
			return res;
		}

	}

	public synchronized long createPublisher(String firstname) {
		ContentValues values = new ContentValues();
		values.put(EpubDbHelper.PublishersTable.firstname, firstname);
		Cursor mCursor = database.query(EpubDbHelper.PublishersTable.tableName,
				new String[] { EpubDbHelper.PublishersTable.id,
						EpubDbHelper.PublishersTable.firstname },
				EpubDbHelper.PublishersTable.firstname + " = '" + firstname +"'", null,
				null, null, null);
		if (mCursor.moveToFirst() == false){
			mCursor.close();
			return database.insert(EpubDbHelper.PublishersTable.tableName,
					null, values);
		}else {
			long res =  mCursor.getLong(0);
			mCursor.close();
			return res;
		}
	}
	
	public String getEpubLocation(String epubId){
		String result = null;
		Cursor mCursor = database.query(EpubDbHelper.EpubsTable.tableName,
				new String[] { EpubDbHelper.EpubsTable.filename}, EpubDbHelper.EpubsTable.id + " = " + epubId, null, null,
				null, null);
		if(mCursor.moveToFirst())
			result = mCursor.getString(0);
		
		mCursor.close();
		return result;
	}

	public List<BookLink> getEpubs() {
		ArrayList<BookLink> list = new ArrayList<BookLink>();

		Cursor mCursor = database.query(EpubDbHelper.EpubsTable.tableName,
				new String[] { EpubDbHelper.EpubsTable.id,// 0
						EpubDbHelper.EpubsTable.coverURL,// 1
						EpubDbHelper.EpubsTable.description,// 2
						EpubDbHelper.EpubsTable.lang,// 3
						EpubDbHelper.EpubsTable.subject,// 4
						EpubDbHelper.EpubsTable.title,//5
						EpubDbHelper.EpubsTable.filename,//6
						EpubDbHelper.EpubsTable.coverData,}, null, null, null,// 7
				null, null);

		while (mCursor.moveToNext()) {
			BookLink book = new BookLink();
			String epubID = mCursor.getString(0);
			book.setId(epubID);
			book.setCoverUrl(mCursor.getString(1));

			Metadata meta = new Metadata();
			meta.addDescription(mCursor.getString(2));
			meta.setLanguage(mCursor.getString(3));
			ArrayList<String> subs = new ArrayList<String>();
			subs.add(mCursor.getString(4));
			meta.setSubjects(subs);
			meta.addTitle(mCursor.getString(5));
			if(!mCursor.isNull(7))
				meta.setCoverImage(new Resource(mCursor.getBlob(7),book.getCoverUrl()));

			Cursor authCursor = database.query(
					EpubDbHelper.EpubAuthorsTable.tableName,
					new String[] { EpubDbHelper.EpubAuthorsTable.authorID },
					EpubDbHelper.EpubAuthorsTable.epubID + " = " + epubID, null,
					null, null, null);
			while(authCursor.moveToNext()){
				Author auth = getAuthor(authCursor.getLong(0));
				if(auth != null) meta.addAuthor(auth);
			}
			
			Cursor pubCursor = database.query(
					EpubDbHelper.EpubPublishersTable.tableName,
					new String[] { EpubDbHelper.EpubPublishersTable.publisherID },
					EpubDbHelper.EpubPublishersTable.epubID + " = " + epubID, null,
					null, null, null);
			while(pubCursor.moveToNext()){
				String pub = getPublisher(pubCursor.getLong(0));
				if(pub != null) meta.addPublisher(pub);
			}

			book.setMeta(meta);
			list.add(book);
			
			authCursor.close();
			pubCursor.close();
		}

		mCursor.close();
		return list;
	}

	public Author getAuthor(long authorID) {
		Author auth = null;
		Cursor mCursor = database.query(EpubDbHelper.AuthorsTable.tableName,
				new String[] { EpubDbHelper.AuthorsTable.firstname,
						EpubDbHelper.AuthorsTable.lastname },
				EpubDbHelper.AuthorsTable.id + " = " + String.valueOf(authorID),
				null, null, null, null);
		if (mCursor.moveToFirst()) {
			auth = new Author(mCursor.getString(0), mCursor.getString(1));
		}
		
		mCursor.close();
		return auth;
	}

	public String getPublisher(long publisherID) {
		String pub = null;
		Cursor mCursor = database.query(
				EpubDbHelper.PublishersTable.tableName,
				new String[] { EpubDbHelper.PublishersTable.firstname },
				EpubDbHelper.PublishersTable.id + " = "
						+ String.valueOf(publisherID), null, null, null, null);
		if (mCursor.moveToFirst()) {
			pub = mCursor.getString(0);
		}
		
		mCursor.close();
		return pub;
	}

	private ContentValues createContentEpubValues(String coverURL,
			String subject, String description, String language, String title, String filename, byte[] coverImage) {
		ContentValues values = new ContentValues();
		values.put(EpubDbHelper.EpubsTable.coverURL, coverURL);
		values.put(EpubDbHelper.EpubsTable.subject, subject);
		values.put(EpubDbHelper.EpubsTable.description, description);
		values.put(EpubDbHelper.EpubsTable.lang, language);
		values.put(EpubDbHelper.EpubsTable.title, title);
		values.put(EpubDbHelper.EpubsTable.filename, filename);
		values.put(EpubDbHelper.EpubsTable.coverData, coverImage);
		return values;
	}
}
