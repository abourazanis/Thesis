package thesis.drmReader.db;

import java.util.ArrayList;

import thesis.drmReader.db.DrmReaderDataBase.Tables;
import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.EpubSearch;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.db.EpubsContract.Publishers;
import thesis.drmReader.util.SelectionBuilder;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class DrmReaderProvider extends ContentProvider {

	private static final String TAG = "DrmReaderProvider";

	private static final UriMatcher sUriMatcher = buildUriMatcher();
	public static final int EPUBS = 100;
	public static final int EPUBS_ID = 101;
	public static final int EPUBS_OFAUTHOR = 102;
	public static final int EPUBS_OFPUBLISHER = 103;
	public static final int AUTHORS = 200;
	public static final int AUTHORS_OFEPUB = 201;
	public static final int AUTHORS_ID = 202;
	public static final int PUBLISHERS = 300;
	public static final int PUBLISHERS_OFEPUB = 301;
	public static final int PUBLISHERS_ID = 302;
	
	private static final int EPUBSEARCH = 400;

    private static final int EPUBSEARCH_ID = 401;

    private static final int SEARCH_SUGGEST = 800;
	private static final int RENEW_FTSTABLE = 900;

	/**
	 * Build and return a {@link UriMatcher} that catches all {@link Uri}
	 * variations supported by this {@link ContentProvider}.
	 */
	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = EpubsContract.CONTENT_AUTHORITY;

		// Epubs
		matcher.addURI(authority, EpubsContract.PATH_EPUBS, EPUBS);
		matcher.addURI(authority, EpubsContract.PATH_EPUBS + "/"
				+ EpubsContract.PATH_OFAUTHOR + "/*", EPUBS_OFAUTHOR);
		matcher.addURI(authority, EpubsContract.PATH_EPUBS + "/"
				+ EpubsContract.PATH_OFPUBLISHER + "/*", EPUBS_OFPUBLISHER);
		matcher.addURI(authority, EpubsContract.PATH_EPUBS + "/*", EPUBS_ID);

		// Authors
		matcher.addURI(authority, EpubsContract.PATH_AUTHORS, AUTHORS);
		matcher.addURI(authority, EpubsContract.PATH_AUTHORS + "/"
				+ EpubsContract.PATH_OFEPUB + "/*", AUTHORS_OFEPUB);
		matcher.addURI(authority, EpubsContract.PATH_AUTHORS + "/*", AUTHORS_ID);

		// Publishers
		matcher.addURI(authority, EpubsContract.PATH_PUBLISHERS, PUBLISHERS);
		matcher.addURI(authority, EpubsContract.PATH_PUBLISHERS + "/"
				+ EpubsContract.PATH_OFEPUB + "/*", PUBLISHERS_OFEPUB);
		matcher.addURI(authority, EpubsContract.PATH_PUBLISHERS + "/*",
				PUBLISHERS_ID);
		
		// Search
        matcher.addURI(authority, EpubsContract.PATH_EPUBSEARCH + "/"
                + EpubsContract.PATH_SEARCH, EPUBSEARCH);
        matcher.addURI(authority, EpubsContract.PATH_EPUBSEARCH + "/*", EPUBSEARCH_ID);

        // Suggestions
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        // Ops
        matcher.addURI(authority, EpubsContract.PATH_RENEWFTSTABLE, RENEW_FTSTABLE);

		return matcher;
	}

	private DrmReaderDataBase mOpenHelper;

	/** {@inheritDoc} */
	@Override
	public boolean onCreate() {
		final Context context = getContext();
		mOpenHelper = new DrmReaderDataBase(context);
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case EPUBS:
			return Epubs.CONTENT_TYPE;
		case EPUBS_ID:
			return Epubs.CONTENT_ITEM_TYPE;
		case EPUBS_OFAUTHOR:
			return Epubs.CONTENT_TYPE;
		case EPUBS_OFPUBLISHER:
			return Epubs.CONTENT_TYPE;
		case AUTHORS:
			return Authors.CONTENT_TYPE;
		case AUTHORS_OFEPUB:
			return Authors.CONTENT_TYPE;
		case AUTHORS_ID:
			return Authors.CONTENT_ITEM_TYPE;
		case PUBLISHERS:
			return Publishers.CONTENT_TYPE;
		case PUBLISHERS_OFEPUB:
			return Publishers.CONTENT_TYPE;
		case PUBLISHERS_ID:
			return Publishers.CONTENT_ITEM_TYPE;
		 case SEARCH_SUGGEST:
             return SearchManager.SUGGEST_MIME_TYPE;
         case RENEW_FTSTABLE:
             // however there is nothing returned
             return Epubs.CONTENT_TYPE;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	/** {@inheritDoc} */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case EPUBS: {
			db.insertOrThrow(Tables.EPUBS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Epubs.buildEpubUri(values.getAsString(Epubs._ID));
		}
		case AUTHORS: {
			db.insertOrThrow(Tables.AUTHORS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Authors.buildAuthorUri(values.getAsString(Authors._ID));
		}
		case PUBLISHERS: {
			db.insertOrThrow(Tables.PUBLISHERS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Publishers.buildPublisherUri(values
					.getAsString(Publishers._ID));
		}
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

	/** {@inheritDoc} */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		 final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

	        final int match = sUriMatcher.match(uri);
	        switch (match) {
	            case RENEW_FTSTABLE: {
	                DrmReaderDataBase.onRenewFTSTable(db);
	                return null;
	            }
	            case EPUBSEARCH: {
	                if (selectionArgs == null) {
	                    throw new IllegalArgumentException(
	                            "selectionArgs must be provided for the Uri: " + uri);
	                }
	                return DrmReaderDataBase.search(selectionArgs[0], db);
	            }
	            case SEARCH_SUGGEST: {
	                if (selectionArgs == null) {
	                    throw new IllegalArgumentException(
	                            "selectionArgs must be provided for the Uri: " + uri);
	                }
	                return DrmReaderDataBase.getSuggestions(selectionArgs[0], db);
	            }
	            default: {
	                // Most cases are handled with simple SelectionBuilder
	                final SelectionBuilder builder = buildExpandedSelection(uri, match);
	                Cursor query = builder.where(selection, selectionArgs).query(db, projection,
	                        sortOrder);
	                query.setNotificationUri(getContext().getContentResolver(), uri);
	                return query;
	            }
	        }
	}

	/** {@inheritDoc} */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSimpleSelection(uri);
		int retVal = builder.where(selection, selectionArgs).update(db, values);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	/** {@inheritDoc} */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSimpleSelection(uri);
		int retVal = builder.where(selection, selectionArgs).delete(db);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	/**
	 * Apply the given set of {@link ContentProviderOperation}, executing inside
	 * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
	 * any single one fails.
	 */
	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			final int numOperations = operations.size();
			final ContentProviderResult[] results = new ContentProviderResult[numOperations];
			for (int i = 0; i < numOperations; i++) {
				results[i] = operations.get(i).apply(this, results, i);
			}
			db.setTransactionSuccessful();
			return results;
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Build a simple {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually enough to support {@link #insert},
	 * {@link #update}, and {@link #delete} operations.
	 */
	private SelectionBuilder buildSimpleSelection(Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case EPUBS: {
			return builder.table(Tables.EPUBS_JOIN_PUBLISHERS_AUTHORS)
					.mapToTable(Epubs._ID, Tables.EPUBS);
		}
		case EPUBS_ID: {
			final String epubId = Epubs.getEpubId(uri);
			return builder.table(Tables.EPUBS).where(Epubs._ID + "=?", epubId);
		}
		case EPUBS_OFAUTHOR: {
			final String authorId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS).where(
					Epubs.REF_AUTHOR_ID + "=?", authorId);
		}
		case EPUBS_OFPUBLISHER: {
			final String publisherId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS).where(
					Epubs.REF_PUBLISHER_ID + "=?", publisherId);
		}
		case AUTHORS:{
			return builder.table(Tables.AUTHORS);
		}
		
		case AUTHORS_ID: {
			final String authorId = Authors.getAuthorId(uri);
			return builder.table(Tables.AUTHORS).where(Authors._ID + "=?",
					authorId);
		}
		case AUTHORS_OFEPUB: {
			final String epubId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS_JOIN_AUTHORS)
					.mapToTable(Epubs._ID, Tables.EPUBS)
					.where(Epubs._ID + "=?", epubId);
		}
		case PUBLISHERS:{
			return builder.table(Tables.PUBLISHERS);
		}
		case PUBLISHERS_ID: {
			final String publisherId = Publishers.getPublisherId(uri);
			return builder.table(Tables.PUBLISHERS).where(
					Publishers._ID + "=?", publisherId);
		}
		case PUBLISHERS_OFEPUB: {
			final String epubId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS_JOIN_PUBLISHERS)
					.mapToTable(Authors._ID, Tables.AUTHORS)
					.where(Epubs._ID + "=?", epubId);
		}
		case EPUBSEARCH_ID: {
            final String rowid = EpubSearch.getEpubId(uri);
            return builder.table(Tables.EPUBS_SEARCH).where(EpubSearch._EPUBID + "=?",
                    rowid);
        }
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

	/**
	 * Build an advanced {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually only used by {@link #query}, since it
	 * performs table joins useful for {@link Cursor} data.
	 */
	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
		case EPUBS: {
			return builder.table(Tables.EPUBS_JOIN_PUBLISHERS_AUTHORS)
					.mapToTable(Epubs._ID, Tables.EPUBS);
//					.mapToTable(Authors.FIRSTNAME, Tables.AUTHORS)
//					.mapToTable(Authors.LASTNAME, Tables.AUTHORS)
//					.mapToTable(Publishers.FIRSTNAME, Tables.PUBLISHERS);
		}
		case EPUBS_ID: {
			final String epubId = Epubs.getEpubId(uri);
			return builder.table(Tables.EPUBS).where(Epubs._ID + "=?", epubId);
		}
		case EPUBS_OFAUTHOR: {
			final String authorId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS).where(
					Epubs.REF_AUTHOR_ID + "=?", authorId);
		}
		case EPUBS_OFPUBLISHER: {
			final String publisherId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS).where(
					Epubs.REF_PUBLISHER_ID + "=?", publisherId);
		}
		case AUTHORS:{
			return builder.table(Tables.AUTHORS);
		}
		case AUTHORS_ID: {
			final String authorId = Authors.getAuthorId(uri);
			return builder.table(Tables.AUTHORS).where(Authors._ID + "=?",
					authorId);
		}
		case AUTHORS_OFEPUB: {
			final String epubId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS_JOIN_AUTHORS)
					.mapToTable(Epubs._ID, Tables.EPUBS)
					.where(Epubs._ID + "=?", epubId);
		}
		case PUBLISHERS:{
			return builder.table(Tables.PUBLISHERS);
		}
		case PUBLISHERS_ID: {
			final String publisherId = Publishers.getPublisherId(uri);
			return builder.table(Tables.PUBLISHERS).where(
					Publishers._ID + "=?", publisherId);
		}
		case PUBLISHERS_OFEPUB: {
			final String epubId = uri.getPathSegments().get(2);
			return builder.table(Tables.EPUBS_JOIN_PUBLISHERS)
					.mapToTable(Authors._ID, Tables.AUTHORS)
					.where(Epubs._ID + "=?", epubId);
		}
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

}
