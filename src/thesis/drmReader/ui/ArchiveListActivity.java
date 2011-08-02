package thesis.drmReader.ui;

import java.io.File;

import thesis.drmReader.EpubsDatabase;
import thesis.drmReader.ImageCache;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubSorting;
import thesis.drmReader.db.EpubsContract.Authors;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.db.EpubsContract.Publishers;
import thesis.drmReader.filebrowser.FileBrowser;
import thesis.drmReader.reader.ReaderView;
import thesis.drmReader.ui.QuickAction.ActionItem;
import thesis.drmReader.ui.QuickAction.QuickAction;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.ImportTask;
import thesis.drmReader.util.concurrent.BetterApplication;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ArchiveListActivity extends FragmentActivity implements
		AbsListView.OnScrollListener, LoaderCallbacks<Cursor> {

	private final static String TAG = ArchiveListActivity.class
			.getCanonicalName();

	private static final String STATE_IMPORT_IN_PROGRESS = "thesis.drmReader.ui.import.inprogress";

	private QuickAction quickAction;
	private long longClickedItemPos = -1;

	public ProgressBar mUpdateProgress;
	public View mProgressOverlay;
	private SlowAdapter mAdapter;
	public ImageCache mImageCache;
	public boolean mBusy;
	private ImportTask mImportTask;
	private EpubSorting sorting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		updatePreferences();

		mImageCache = ((thesis.drmReader.DrmReaderApplication) this
				.getApplication()).getImageCache();
		

		String[] from = new String[] { Epubs.TITLE, Epubs.LANGUAGE,
				Authors.FIRSTNAME, Authors.LASTNAME, Publishers.FIRSTNAME };
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
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				readDoc(id);
			}
		});
		list.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
					long id) {
				longClickedItemPos = id;
				quickAction.show(v);
				return true;
			}
		});
		list.setOnScrollListener(this);
		View emptyView = findViewById(android.R.id.empty);
		if (emptyView != null) {
			list.setEmptyView(emptyView);
		}

		getSupportLoaderManager().initLoader(Constants.LIST_DOCUMENTS, null,
				this);

		initQuickAction();
		registerForContextMenu(list);

		Application application = getApplication();
		if (application instanceof BetterApplication) {
			((BetterApplication) application).setActiveContext(getClass()
					.getCanonicalName(), this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkPreferences();
	}
	
	public void onStart() {
   		// start tracing to “/sdcard/archiveList.trace”
   		//Debug.startMethodTracing("archiveList");
		super.onStart();
 			// other start up code here…
       }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Debug.stopMethodTracing();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.getBoolean(STATE_IMPORT_IN_PROGRESS)) {
			if (mProgressOverlay == null) {
				Log.d(TAG, "restore instance progress is null");
				mProgressOverlay = ((ViewStub) findViewById(R.id.stub_update))
						.inflate();
				mUpdateProgress = (ProgressBar) findViewById(R.id.ProgressBarShowListDet);

				final View cancelButton = mProgressOverlay
						.findViewById(R.id.overlayCancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelTasks();
					}
				});
			}

			mUpdateProgress.setIndeterminate(false);
			mUpdateProgress.setProgress(0);
			showOverlay(mProgressOverlay);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		final ImportTask task = mImportTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			outState.putBoolean(STATE_IMPORT_IN_PROGRESS, true);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

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
	protected Dialog onCreateDialog(int id) {
		String message = "";
		switch (id) {
		case Constants.IMPORT_DOCUMENT_ERROR:
			message = getString(R.string.import_error_msg);
			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.import_error_title))
					.setMessage(message)
					.setPositiveButton(android.R.string.ok, null).create();
		case Constants.IMPORT_DOCUMENT_FILENOTFOUND:
			message = getString(R.string.import_file_error_msg);
			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.import_file_error_title))
					.setMessage(message)
					.setPositiveButton(android.R.string.ok, null).create();
		case Constants.IMPORT_DOCUMENT_INVALIDKEY:
			message = getString(R.string.import_invalid_key_msg);
			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.import_invalid_key_title))
					.setMessage(message)
					.setPositiveButton(android.R.string.ok, null).create();
		case Constants.SORT_DIALOG:
			final CharSequence[] items = getResources().getStringArray(
					R.array.epub_sorting);

			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.sorting_title))
					.setSingleChoiceItems(items, sorting.index(),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									sorting = (EpubSorting.values())[item];

									SharedPreferences.Editor prefEditor = PreferenceManager
											.getDefaultSharedPreferences(
													getApplicationContext())
											.edit();
									prefEditor
											.putString(
													Constants.KEY_EPUBSORTORDERARCHIVE,
													(getResources()
															.getStringArray(R.array.epub_sortingData))[item]);
									prefEditor.commit();

									requery();
									removeDialog(Constants.SORT_DIALOG);
								}
							}).create();
		}

		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.archive_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			final CharSequence[] items = getResources().getStringArray(
					R.array.epub_sorting);
			menu.findItem(R.id.menu_showsortby).setTitle(
					getString(R.string.sorting_title) + ": "
							+ items[sorting.index()]);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.importEpub:
			if (!isImportTaskRunning()) {
				Intent i = new Intent(this, FileBrowser.class);
				this.startActivityForResult(i, Constants.IMPORT_REQUEST);
			}
			return true;
		case R.id.web:
			Intent intent = new Intent(this, WebStoreActivity.class);
			this.startActivity(intent);
			return true;
		case R.id.menu_showsortby:
			showDialog(Constants.SORT_DIALOG);
			return true;
		case R.id.menu_search:
			Log.d(TAG,"search requested");
			onSearchRequested();
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case Constants.IMPORT_REQUEST:
				String[] epubFiles = data.getStringArrayExtra("filepaths");
				importEpub(epubFiles );
				break;
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Epubs.CONTENT_URI, EpubsQuery.PROJECTION,
				null, null, sorting.query());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);

	}
	
	private void requery() {
        getSupportLoaderManager().restartLoader(Constants.LIST_DOCUMENTS, null, this);
    }

	private void checkPreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		updateSorting(prefs);
	}

	private void updatePreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		updateSorting(prefs);
	}

	/**
	 * Fetch the sorting preference and store it in this class.
	 * 
	 * @param prefs
	 * @return Returns true if the value changed, false otherwise.
	 */
	private boolean updateSorting(SharedPreferences prefs) {
		EpubSorting oldSorting = sorting;
		String value = prefs.getString(Constants.KEY_EPUBSORTORDERARCHIVE,
				"alphabetic asc");
		if (value.equalsIgnoreCase("alphabetic asc")) {
			sorting = EpubSorting.ALPHABETIC_ASC;
		} else if (value.equalsIgnoreCase("alphabetic desc")) {
			sorting = EpubSorting.ALPHABETIC_DESC;
		} else if (value.equalsIgnoreCase("author desc")) {
			sorting = EpubSorting.AUTHOR_ASC;
		} else {
			sorting = EpubSorting.AUTHOR_DESC;
		}

		return oldSorting != sorting;
	}

	private interface EpubsQuery {
		String[] PROJECTION = { BaseColumns._ID, Epubs.TITLE, Epubs.SUBJECT,
				Epubs.DESCRIPTION, Epubs.FILENAME,
				Epubs.LANGUAGE, Authors.FIRSTNAME, Authors.LASTNAME,
				Publishers.FIRSTNAME };
		
		int TITLE = 1;
		int SUBJECT = 2;
		int DESCRIPTION = 3;
		int FILENAME = 4;
		int LANGUAGE = 5;
		int FIRSTNAME = 6;
		int LASTNAME = 7;
		int PUBLISHER = 8;
		
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
			
			//we use exlipcit column index values because getColumnIndexOrThrow was CPU hungry

			String title = mCursor.getString(EpubsQuery.TITLE);
			viewHolder.textViewTitle.setText(title);
			viewHolder.textViewAuthor.setText(mCursor.getString(EpubsQuery.FIRSTNAME)
					+ " "
					+ mCursor.getString(EpubsQuery.LASTNAME));
			viewHolder.textViewLanguage.setText(mCursor.getString(EpubsQuery.LANGUAGE));
			viewHolder.textViewPublisher.setText(mCursor.getString(EpubsQuery.PUBLISHER));
			

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

	private void initQuickAction() {
		quickAction = new QuickAction(this);
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);

		ActionItem read = new ActionItem();
		read.setTitle(this.getString(R.string.read_item));
		read.setIcon(getResources().getDrawable(R.drawable.ic_menu_read));

		ActionItem delete = new ActionItem();
		delete.setTitle(this.getString(R.string.delete_item));
		delete.setIcon(getResources().getDrawable(R.drawable.ic_menu_delete));

		quickAction.addActionItem(read);
		quickAction.addActionItem(delete);

		quickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(int pos) {
						if (pos == 0) {
							readDoc(longClickedItemPos);
						} else {
							deleteDoc(longClickedItemPos);
						}
						longClickedItemPos = -1;
					}
				});
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
			//maybe a thread to restore image from DB
		}
	}

	private void deleteDoc(long position) {
		final String filename = EpubsDatabase.getEpubLocation(
				String.valueOf(position), this);
		EpubsDatabase.deleteEpub(this, String.valueOf(position));

		if (filename != null) {
			File fileToRm = new File(filename);
			if (fileToRm.exists())
				fileToRm.delete();
		}
	}

	private void readDoc(long position) {
		Intent intent = new Intent(this, ReaderView.class);
		final String filename = EpubsDatabase.getEpubLocation(
				String.valueOf(position), this);
		intent.putExtra(Epubs.FILENAME, filename);
		startActivity(intent);
	}

	/**
	 * If the importThread is already running, shows a toast telling the user to
	 * wait.
	 * 
	 * @return true if an import is in progress and toast was shown, false
	 *         otherwise
	 */
	private boolean isImportTaskRunning() {
		if (mImportTask != null
				&& mImportTask.getStatus() != AsyncTask.Status.FINISHED) {
			Toast.makeText(this, getString(R.string.import_inprogress),
					Toast.LENGTH_LONG).show();
			return true;
		} else {
			return false;
		}
	}

	public void importEpub(String[] epubFiles) {
		Toast.makeText(this, getString(R.string.import_inbackground),
				Toast.LENGTH_SHORT).show();
		mImportTask = (ImportTask) new ImportTask(epubFiles, this).execute();

	}

	public void onCancelTasks() {
		if (mImportTask != null
				&& mImportTask.getStatus() == AsyncTask.Status.RUNNING) {
			mImportTask.cancel(true);
			mImportTask = null;
		}
	}

	public void showOverlay(View overlay) {
		if (overlay != null) {
			overlay.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fadein));
			overlay.setVisibility(View.VISIBLE);
		}
	}

	public void hideOverlay(View overlay) {
		if (overlay != null) {
			overlay.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fadeout));
			overlay.setVisibility(View.GONE);
		}
	}
}
