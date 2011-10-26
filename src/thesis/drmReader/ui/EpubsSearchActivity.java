package thesis.drmReader.ui;

import thesis.drmReader.EpubsDatabase;
import thesis.drmReader.ImageCache;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.EpubSearch;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.reader.ReaderView;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.concurrent.BetterApplication;
import android.app.Application;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class EpubsSearchActivity extends FragmentActivity implements
		AbsListView.OnScrollListener, LoaderCallbacks<Cursor> {

	private static final String TAG = EpubsSearchActivity.class
			.getCanonicalName();
	public ImageCache mImageCache;
	public boolean mBusy;
	private SlowAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		handleIntent(getIntent());

		mImageCache = ((thesis.drmReader.DrmReaderApplication) this
				.getApplication()).getImageCache();

		String[] from = new String[] { SearchQuery.TITLE, SearchQuery.LANGUAGE,
				SearchQuery.AUTH_FIRSTNAME, SearchQuery.AUTH_LASTNAME,
				SearchQuery.PUBLISHER };

		int[] to = new int[] { R.id.epubTitle, R.id.epubLanguage,
				R.id.epubAuthor, R.id.epubAuthor, R.id.epubPublisher };

		mAdapter = new SlowAdapter(this, R.layout.list_item, null, from, to, 0);

		AbsListView list = (AbsListView) findViewById(android.R.id.list);
		if (android.os.Build.VERSION.SDK_INT < 11) {
			((ListView) list).setAdapter(mAdapter);
		} else {
			// only possible since API level 11 (Honeycomb)
			list.setAdapter(mAdapter);
		}

		list.setFastScrollEnabled(true);
		list.setOnScrollListener(this);
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				readDoc(String.valueOf(id));
			}
		});
		
		View emptyView = findViewById(android.R.id.empty);
		if (emptyView != null) {
			list.setEmptyView(emptyView);
		}
		
		getSupportLoaderManager().initLoader(Constants.SEARCH_DOCUMENTS, null,
				this);
		
		Application application = getApplication();
		if (application instanceof BetterApplication) {
			((BetterApplication) application).setActiveContext(getClass()
					.getCanonicalName(), this);
		}

	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, ArchiveListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // to close the
																// rest
																// activities
																// and remain in
																// home.Like a
																// clear start
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
			readDoc(id);
			finish();
		}
	}

	private void doMySearch(String query) {

		this.setTitle("Search for '" + query + "'");
		Bundle args = new Bundle();
		args.putString("query", query);
		getSupportLoaderManager().restartLoader(Constants.SEARCH_DOCUMENTS, args,
				this);
	}

	
	private void readDoc(String id) {
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

		String[] PROJECTION = new String[] { _ID, TITLE, DESCRIPTION, SUBJECT,
				LANGUAGE, AUTH_FIRSTNAME, AUTH_LASTNAME, PUBLISHER };

		int _ID_index = 0;

		int TITLE_index = 1;

		int DESCRIPTION_index = 2;

		int SUBJECT_index = 3;

		int LANGUAGE_index = 4;

		int AUTH_FIRSTNAME_index = 5;

		int AUTH_LASTNAME_index = 6;

		int PUBLISHER_index = 7;
	}

	private class SlowAdapter extends SimpleCursorAdapter {

		private LayoutInflater mLayoutInflater;
		private int mLayout;

		public SlowAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);

			mLayoutInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (!mDataValid) {
				throw new IllegalStateException(
						"this should only be called when the cursor is valid");
			}
			if (!mCursor.moveToPosition(position)) {
				throw new IllegalStateException(
						"couldn't move cursor to position " + position);
			}

			ViewHolder viewHolder;

			if (convertView == null) {
				convertView = mLayoutInflater.inflate(mLayout, null);

				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) convertView
						.findViewById(R.id.epubCover);
				viewHolder.textViewTitle = (TextView) convertView
						.findViewById(R.id.epubTitle);
				viewHolder.textViewAuthor = (TextView) convertView
						.findViewById(R.id.epubAuthor);
				viewHolder.textViewLanguage = (TextView) convertView
						.findViewById(R.id.epubLanguage);
				viewHolder.textViewPublisher = (TextView) convertView
						.findViewById(R.id.epubPublisher);

				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			// we use explicit column index values because getColumnIndexOrThrow
			// was CPU hungry

			String title = mCursor.getString(SearchQuery.TITLE_index);
			viewHolder.textViewTitle.setText(title);
			viewHolder.textViewAuthor.setText(mCursor
					.getString(SearchQuery.AUTH_FIRSTNAME_index)
					+ " "
					+ mCursor.getString(SearchQuery.AUTH_LASTNAME_index));
			viewHolder.textViewLanguage.setText(mCursor
					.getString(SearchQuery.LANGUAGE_index));
			viewHolder.textViewPublisher.setText(mCursor
					.getString(SearchQuery.PUBLISHER_index));

			// set cover only when not busy scrolling
			if (!mBusy) {
				// load poster
				setPosterBitmap(viewHolder.imageView, title);

				// Null tag means the view has the correct data
				viewHolder.imageView.setTag(null);
			} else {
				// set placeholder
				viewHolder.imageView.setImageResource(R.drawable.nocover);

				// Non-null tag means the view still needs to load it's data
				viewHolder.imageView.setTag(title);
			}

			return convertView;
		}
	}

	public final class ViewHolder {

		public ImageView imageView;
		public TextView textViewTitle;
		public TextView textViewAuthor;
		public TextView textViewLanguage;
		public TextView textViewPublisher;
	}

	private void setPosterBitmap(ImageView cover, String path) {
		Bitmap bitmap = null;
		if (path.length() != 0) {
			bitmap = mImageCache.getThumb(path);
		}

		if (bitmap != null) {
			cover.setImageBitmap(bitmap);
		} else {
			// set placeholder
			cover.setImageResource(R.drawable.nocover);
			// maybe a thread to restore image from DB
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_IDLE:
			mBusy = false;

			int count = view.getChildCount();
			for (int i = 0; i < count; i++) {
				final ViewHolder holder = (ViewHolder) view.getChildAt(i)
						.getTag();
				final ImageView poster = holder.imageView;
				if (poster.getTag() != null) {
					setPosterBitmap(poster, (String) poster.getTag());
					poster.setTag(null);
				}
			}

			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mBusy = true;
			break;
		case OnScrollListener.SCROLL_STATE_FLING:
			mBusy = true;
			break;
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, EpubSearch.CONTENT_URI_SEARCH,
				SearchQuery.PROJECTION, null, new String[] { args.getString("query") }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
