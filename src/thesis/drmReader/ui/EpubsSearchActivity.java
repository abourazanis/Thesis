package thesis.drmReader.ui;

import thesis.drmReader.EpubsDatabase;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.EpubSearch;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.db.EpubsContract.Publishers;
import thesis.drmReader.reader.ReaderView;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class EpubsSearchActivity extends ListActivity {

	private static final String TAG = EpubsSearchActivity.class
			.getCanonicalName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		handleIntent(getIntent());

		setTitle(getString(R.string.search_title));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (intent == null) {
			return;
		}
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMySearch(query);
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri data = intent.getData();
			String id = data.getLastPathSegment();
			onViewEpub(id);
			finish();
		}
	}
	
	//TODO: remove epubdescription declaration and use some variable

	private void doMySearch(String query) {
		Cursor searchResults = getContentResolver().query(
				EpubSearch.CONTENT_URI_SEARCH, SearchQuery.PROJECTION, null,
				new String[] { query }, null);
		startManagingCursor(searchResults);
		String[] from = new String[] { SearchQuery.TITLE, SearchQuery.LANGUAGE,
				SearchQuery.DESCRIPTION, SearchQuery.AUTH_FIRSTNAME, SearchQuery.AUTH_LASTNAME,
				SearchQuery.PUBLISHER };
		int[] to = new int[] { R.id.search_epubTitle, R.id.search_language,
				R.id.search_snippet, R.id.search_epubAuthor,
				R.id.search_epubAuthor, R.id.search_publishers };
		SimpleCursorAdapter resultsAdapter = new SimpleCursorAdapter(
				getApplicationContext(), R.layout.search_row, searchResults,
				from, to);
		resultsAdapter.setViewBinder(new ViewBinder() {

			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (columnIndex == SearchQuery.TITLE_index) {
					TextView title = (TextView) view;
					title.setText(cursor.getString(columnIndex));
					return true;
				}
				if (columnIndex == SearchQuery.LANGUAGE_index) {
					TextView language = (TextView) view;
					language.setText(cursor.getString(columnIndex));
					return true;
				}
				if (columnIndex == SearchQuery.AUTH_FIRSTNAME_index) {
					TextView author = (TextView) view;
					author.setText(cursor.getString(columnIndex) + " "
							+ cursor.getString(SearchQuery.AUTH_LASTNAME_index));
					return true;
				}
				if (columnIndex == SearchQuery.PUBLISHER_index) {
					TextView publisher = (TextView) view;
					publisher.setText(cursor.getString(columnIndex));
					return true;
				}

				return false;
			}
		});
		setListAdapter(resultsAdapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		onViewEpub(String.valueOf(id));
	}

	private void onViewEpub(String id) {
		Intent intent = new Intent(this, ReaderView.class);
		final String filename = EpubsDatabase.getEpubLocation(id, this);
		intent.putExtra(Epubs.FILENAME, filename);
		startActivity(intent);
	}

	public interface SearchQuery {
		
		String _ID = Epubs._ID;
		String TITLE = Epubs.TITLE;
		String DESCRIPTION = "epubdescription";
		String AUTH_FIRSTNAME = Authors.FIRSTNAME;
		String AUTH_LASTNAME = Authors.LASTNAME;
		String SUBJECT = Epubs.SUBJECT;
		String LANGUAGE = Epubs.LANGUAGE;
		String PUBLISHER = "publisherName";
		
		String[] PROJECTION = new String[] { _ID, TITLE,
				DESCRIPTION, SUBJECT, LANGUAGE,
				AUTH_FIRSTNAME, AUTH_LASTNAME, PUBLISHER };

		int _ID_index = 0;

		int TITLE_index = 1;

		int DESCRIPTION_index = 2;

		int SUBJECT_index = 3;

		int LANGUAGE_index = 4;
		

		int AUTH_FIRSTNAME_index = 5;

		int AUTH_LASTNAME_index = 6;

		int PUBLISHER_index = 7;
	}

}
