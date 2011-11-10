
package thesis.drmReader;

import java.util.ArrayList;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.util.StringUtil;

import thesis.drmReader.db.EpubsContract;
import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.EpubSearch;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.db.EpubsContract.Publishers;
import thesis.drmReader.ui.BookLink;
import thesis.drmReader.util.Lists;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

/**
 * @author tas0s
 */
public class EpubsDatabase {
    static final String TAG = "EpubsDatabase";

    /**
     * Builds a {@link ContentProviderOperation} for inserting or updating a
     * epub.
     * 
     * @param epub
     * @param context
     * @return
     */
    public static ContentProviderOperation buildEpubOp(BookLink epub, Context context, String epubId) {
        ContentValues values = new ContentValues();
        String authorId = getAuthorId(epub.getMeta().getAuthors().get(0), context);
        String publisherId = getPublisherId(epub.getMeta().getPublishers().get(0), context);

        values = putCommonShowValues(epub, values, authorId, publisherId);

        if (!StringUtil.isNotBlank(epubId)) {
            return ContentProviderOperation.newInsert(Epubs.CONTENT_URI).withValues(values).build();
        } else {
            return ContentProviderOperation.newUpdate(Epubs.buildEpubUri(epubId))
                    .withValues(values).build();
        }
    }

    /**
     * Builds a {@link ContentProviderOperation} for inserting or updating a
     * author.
     * 
     * @param author
     * @param context
     * @return
     */
    public static ContentProviderOperation buildAuthorOp(Author author, Context context) {
        ContentValues values = new ContentValues();
        values.put(Authors.FIRSTNAME, author.getFirstname());
        values.put(Authors.LASTNAME, author.getLastname());

        return ContentProviderOperation.newInsert(Authors.CONTENT_URI).withValues(values).build();
    }

    /**
     * Builds a {@link ContentProviderOperation} for inserting a publisher
     * 
     * @param publisher
     * @param context
     * @return
     */
    public static ContentProviderOperation buildPublisherOp(String publisher, Context context) {
        ContentValues values = new ContentValues();
        values.put(Publishers.FIRSTNAME, publisher);

        return ContentProviderOperation.newInsert(Publishers.CONTENT_URI).withValues(values)
                .build();
    }

    /**
     * Adds default epub information from given BookLink object to given
     * ContentValues.
     * 
     * @param epub
     * @param values
     * @return
     */
    private static ContentValues putCommonShowValues(BookLink epub, ContentValues values,
            String authorId, String publisherId) {
        values.put(Epubs.TITLE, epub.getMeta().getFirstTitle());
        values.put(Epubs.SUBJECT, epub.getMeta().getSubjects().get(0));
        values.put(Epubs.FILENAME, epub.getId());
        values.put(Epubs.COVERDATA, epub.getMeta().getCoverImage().getData());
        values.put(Epubs.LANGUAGE, epub.getMeta().getLanguage());
        values.put(Epubs.DESCRIPTION, epub.getMeta().getDescriptions().get(0));
        values.put(Epubs.REF_AUTHOR_ID, authorId);
        values.put(Epubs.REF_PUBLISHER_ID, publisherId);
        return values;
    }

    /**
     * Delete the epub with epubId equal to id.
     * 
     * @param context
     * @param id
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    public static void deleteEpub(Context context, String id) {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        final String epubId = String.valueOf(id);

        batch.add(ContentProviderOperation.newDelete(Epubs.buildEpubUri(epubId)).build());
        try {
            context.getContentResolver().applyBatch(EpubsContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * check if the specific author exists
     * 
     * @param author
     * @param context
     * @return
     */
    public static boolean isAuthorExists(Author author, Context context) {
        Cursor search = context.getContentResolver().query(Authors.CONTENT_URI, new String[] {
            Authors._ID
        }, Authors.FIRSTNAME + " = ? AND " + Authors.LASTNAME + " = ?", new String[] {
                author.getFirstname(), author.getLastname()
        }, null);
        boolean isAuthorExists = search.getCount() != 0 ? true : false;
        search.close();
        return isAuthorExists;
    }

    /**
     * check if the specific publisher exists
     * 
     * @param author
     * @param context
     * @return
     */
    public static boolean isPublisherExists(String publisherName, Context context) {
        Cursor search = context.getContentResolver().query(Publishers.CONTENT_URI,
                new String[] {
                    Publishers._ID
                }, Publishers.FIRSTNAME + " = ? ", new String[] {
                    publisherName
                }, null);
        boolean isPublisherExists = search.getCount() != 0 ? true : false;
        search.close();
        return isPublisherExists;
    }

    /**
     * returns the id column value of the specific epub
     * 
     * @param epub
     * @param context
     * @return
     */
    public static String getEpubId(BookLink epub, Context context) {

        String result = null;
        String authorId = getAuthorId(epub.getMeta().getAuthors().get(0), context);

        if (StringUtil.isNotBlank(authorId)) {
            Cursor search = context.getContentResolver().query(
                    Epubs.buildEpubsOfAuthorUri(authorId), new String[] {
                        Epubs._ID
                    }, Epubs.TITLE + " = ? ", new String[] {
                        epub.getMeta().getTitles().get(0)
                    }, null);
            if (search.moveToFirst()) {
                result = String.valueOf(search.getInt(0));
            }
            search.close();
        }

        return result;
    }

    /**
     * returns the id column value of the specific author
     * 
     * @param author
     * @param context
     * @return
     */
    public static String getAuthorId(Author author, Context context) {
        Cursor search = context.getContentResolver().query(Authors.CONTENT_URI, new String[] {
            Authors._ID
        }, Authors.FIRSTNAME + " = ? AND " + Authors.LASTNAME + " = ?", new String[] {
                author.getFirstname(), author.getLastname()
        }, null);
        String id = null;
        if (search.moveToFirst()) {
            id = String.valueOf(search.getInt(0));
        }
        search.close();
        return id;
    }

    /**
     * get the id column value of the specific publisher
     * 
     * @param publisherName
     * @param context
     * @return
     */
    public static String getPublisherId(String publisherName, Context context) {
        Cursor search = context.getContentResolver().query(Publishers.CONTENT_URI,
                new String[] {
                    Publishers._ID
                }, Publishers.FIRSTNAME + " = ? ", new String[] {
                    publisherName
                }, null);
        String id = null;
        if (search.moveToFirst()) {
            id = String.valueOf(search.getInt(0));
        }
        search.close();
        return id;
    }

    /**
     * Adds a epub and its author and publisher (if they don't exist already in
     * DB).
     * 
     * @param epub
     */
    public static void addEpub(BookLink epub, Context context) {

        String publisher = epub.getMeta().getPublishers().get(0);
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        if (!isPublisherExists(publisher, context))
            batch.add(buildPublisherOp(publisher, context));

        if (!isAuthorExists(epub.getMeta().getAuthors().get(0), context))
            batch.add(buildAuthorOp(epub.getMeta().getAuthors().get(0), context));

        try {
            context.getContentResolver().applyBatch(EpubsContract.CONTENT_AUTHORITY, batch);

            final ArrayList<ContentProviderOperation> batch_b = Lists.newArrayList();
            batch_b.add(buildEpubOp(epub, context, getEpubId(epub, context)));
            context.getContentResolver().applyBatch(EpubsContract.CONTENT_AUTHORITY, batch_b);

        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * get the file location (filepath) of the specific epub
     * 
     * @param epubId
     * @param context
     * @return
     */
    public static String getEpubLocation(String epubId, Context context) {

        Cursor search = context.getContentResolver().query(Epubs.buildEpubUri(epubId),
                new String[] {
                    Epubs.FILENAME
                }, null, null, null);

        String location = null;
        if (search.moveToFirst()) {
            location = search.getString(search.getColumnIndexOrThrow(Epubs.FILENAME));
        }
        search.close();
        return location;
    }

    public static void onRenewFTSTable(Context context) {
        context.getContentResolver().query(EpubSearch.CONTENT_URI_RENEWFTSTABLE, null, null, null,
                null);
    }
}
