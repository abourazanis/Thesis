package thesis.drmReader.ui;

import java.io.FileInputStream;
import java.util.ArrayList;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import thesis.drmReader.EpubsDatabase;
import thesis.drmReader.R;
import thesis.drmReader.RestClient;
import thesis.drmReader.RestClient.RequestMethod;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.Utils;
import thesis.sec.Decrypter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class WebStoreActivity extends SherlockFragmentActivity implements
		ParentActivity, LoaderCallbacks<String> {

	private static final String STATE_IMPORT_IN_PROGRESS = "thesis.drmReader.ui.download.inprogress";
	private ProgressBar mUpdateProgress;
	private View mProgressOverlay;
	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;
	int mDialogShowing = -100;
	int mActiveFragmentIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.webstore);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();
		mTabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
		addTab("new", "New", Constants.URL_NEW, 0);
		addTab("toppicks", "Top Picks", Constants.URL_TOP, 1);
		addTab("all", "All", Constants.URL, 2);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

	}

	private void addTab(String tabname, String indicator, String url, int index) {
		TabHost.TabSpec spec = mTabHost.newTabSpec(tabname);

		View tabIndicator = LayoutInflater.from(this).inflate(R.layout.tabs_bg,
				mTabHost.getTabWidget(), false);
		TextView title = (TextView) tabIndicator.findViewById(R.id.title);
		title.setText(indicator);
		spec.setIndicator(tabIndicator);

		Bundle args = new Bundle();
		args.putString("URL", url);
		args.putInt("index", index);
		mTabsAdapter.addTab(spec, WebListFragment.class, args);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());

		if (mDialogShowing >= 0)
			hideDialog(mDialogShowing);
		Loader<Object> load = this.getSupportLoaderManager().getLoader(0);
		if (load != null && load.isStarted()) {
			outState.putBoolean(STATE_IMPORT_IN_PROGRESS, true);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.getBoolean(STATE_IMPORT_IN_PROGRESS)) {
			if (mProgressOverlay == null) {
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
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.store_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
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
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case Constants.OFFLINE:
			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.offline_title))
					.setMessage(
							getString(R.string.weblist_error) + ". "
									+ getString(R.string.offline))
					.setPositiveButton(android.R.string.ok, null).create();
		case Constants.DOWNLOAD_DOCLIST_ALERT:
			return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.weblist_error_title))
					.setMessage(getString(R.string.weblist_error_msg))
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mDialogShowing = -1;

								}

							}).create();
		}

		return null;
	}

	@Override
	public void displayDialog(int dialogID) {
		mDialogShowing = dialogID;
		showDialog(dialogID);

	}

	@Override
	public void hideDialog(int dialogID) {
		mDialogShowing = -1;
		dismissDialog(dialogID);
	}

	@Override
	public void downloadDocument(BookLink document) {
		Loader<Object> load = this.getSupportLoaderManager().getLoader(0);
		if (load != null && load.isStarted()) {
			Toast.makeText(
					this,
					"There is a file download already in progress.Please wait to finish and try again",
					Toast.LENGTH_LONG).show();
		} else {
			if (mProgressOverlay == null) {
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

			mUpdateProgress.setIndeterminate(true);
			showOverlay(mProgressOverlay);

			Bundle args = new Bundle();
			args.putString("docid", document.getId());
			args.putString("docname", document.getMeta().getFirstTitle());
			this.getSupportLoaderManager().restartLoader(0, args, this);
		}
	}

	@Override
	public int getActiveFragmentIndex() {
		return mActiveFragmentIndex;
	}

	public void onCancelTasks() {
		Loader<Object> load = this.getSupportLoaderManager().getLoader(0);
		if (load != null && load.isStarted()) {
			hideOverlay(mProgressOverlay);
			this.getSupportLoaderManager().getLoader(0).reset();
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

	@Override
	public Loader<String> onCreateLoader(int id, Bundle args) {
		String docID = args.getString("docid");
		String docName = args.getString("docname");
		return new EpubDownloader(this, docID, docName);
	}

	@Override
	public void onLoadFinished(Loader<String> loader, String data) {
		hideOverlay(mProgressOverlay);
		loader.stopLoading();

	}

	@Override
	public void onLoaderReset(Loader<String> loader) {

	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */
	public static class TabsAdapter extends FragmentPagerAdapter implements
			TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final TabHost mTabHost;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final String tag;
			private final Class<?> clss;
			private final Bundle args;

			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				clss = _class;
				args = _args;
			}
		}

		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			public DummyTabFactory(Context context) {
				mContext = context;
			}

			@Override
			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}

		public TabsAdapter(FragmentActivity activity, TabHost tabHost,
				ViewPager pager) {
			super(activity.getSupportFragmentManager());
			mContext = activity;
			mTabHost = tabHost;
			mViewPager = pager;
			mTabHost.setOnTabChangedListener(this);
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
			tabSpec.setContent(new DummyTabFactory(mContext));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, clss, args);
			mTabs.add(info);
			mTabHost.addTab(tabSpec);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			return Fragment.instantiate(mContext, info.clss.getName(),
					info.args);
		}

		@Override
		public void onTabChanged(String tabId) {
			int position = mTabHost.getCurrentTab();
			mViewPager.setCurrentItem(position);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mTabHost.setCurrentTab(position);
			((WebStoreActivity) this.mContext).mActiveFragmentIndex = position;
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}

	
	public static class EpubDownloader extends AsyncTaskLoader<String> {

		private String mResult;
		private String fileID;
		private String filename;

		public EpubDownloader(Context context, String docID, String docName) {
			super(context);
			fileID = docID;
			filename = docName;
		}

		/**
		 * This is where the bulk of our work is done. This function is called
		 * in a background thread and should generate a new set of data to be
		 * published by the loader.
		 */
		@Override
		public String loadInBackground() {
			String result = null;
			RestClient client = new RestClient(Constants.GETDOCURL + fileID,
					true, filename + ".epub");
			try {
				String key = new Decrypter(this.getContext())
						.getUniqueIdentifier();
				client.AddParam("key", key);
				client.Execute(RequestMethod.POST);
				if (client.getResponseCode() == Constants.HTTP_RESPONSE_OK) {
					result = client.getSavedFilePath();

					// import to lib
					FileInputStream epubStream = new FileInputStream(result);
					BookLink epubLink = new BookLink();
					Decrypter decrypter = new Decrypter(result,
							this.getContext());
					Metadata meta = (new EpubReader(decrypter))
							.readEpubMetadata(epubStream);

					epubLink.setMeta(meta);
					if (meta.getCoverImage() != null)
						epubLink.setCoverUrl(meta.getCoverImage().getHref());
					epubLink.setId(String.valueOf(result));

					Resource coverResource = decrypter.decrypt(meta
							.getCoverImage());
					epubLink.getMeta().setCoverImage(coverResource);

					Utils.putCoverToCache(this.getContext()
							.getApplicationContext(), coverResource.getData(),
							meta.getFirstTitle());
					EpubsDatabase.addEpub(epubLink, this.getContext());

					// renew FTS3 table
					EpubsDatabase.onRenewFTSTable(this.getContext());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}

		/**
		 * Called when there is new data to deliver to the client. The super
		 * class will take care of delivering it; the implementation here just
		 * adds a little more logic.
		 */
		@Override
		public void deliverResult(String result) {
			if (isReset()) {
				// An async query came in while the loader is stopped. We
				// don't need the result.
				if (result != null) {
					onReleaseResources(result);
				}
			}
			String oldResult = result;
			mResult = result;

			if (isStarted()) {
				// If the Loader is currently started, we can immediately
				// deliver its results.
				super.deliverResult(result);
			}

			// At this point we can release the resources associated with
			// 'oldApps' if needed; now that the new result is delivered we
			// know that it is no longer in use.
			if (oldResult != null) {
				onReleaseResources(oldResult);
			}
		}

		/**
		 * Handles a request to start the Loader.
		 */
		@Override
		protected void onStartLoading() {
			if (mResult != null) {
				deliverResult(mResult);
			}

			if (takeContentChanged() || mResult == null) {
				forceLoad();
			}
		}

		/**
		 * Handles a request to stop the Loader.
		 */
		@Override
		protected void onStopLoading() {
			// Attempt to cancel the current load task if possible.
			cancelLoad();
		}

		/**
		 * Handles a request to cancel a load.
		 */
		@Override
		public void onCanceled(String result) {
			super.onCanceled(result);

			// At this point we can release the resources associated with 'apps'
			// if needed.
			onReleaseResources(result);
		}

		/**
		 * Handles a request to completely reset the Loader.
		 */
		@Override
		protected void onReset() {
			super.onReset();

			// Ensure the loader is stopped
			onStopLoading();

			// At this point we can release the resources associated with 'apps'
			// if needed.
			if (mResult != null) {
				onReleaseResources(mResult);
				mResult = null;
			}
		}

		/**
		 * Helper function to take care of releasing resources associated with
		 * an actively loaded data set.
		 */
		protected void onReleaseResources(String result) {
			// For a simple List<> there is nothing to do. For something
			// like a Cursor, we would close it here.
		}
	}

}