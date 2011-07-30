package thesis.drmReader.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.R;
import thesis.drmReader.RestClient;
import thesis.drmReader.RestClient.RequestMethod;
import thesis.drmReader.db.EpubDbAdapter;
import thesis.drmReader.filebrowser.FileBrowser;
import thesis.drmReader.ui.WebListFragment.ImportListener;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class WebListActivity extends FragmentActivity implements
		AbsListView.OnScrollListener, LoaderCallbacks<List<BookLink>> {

	private final static String TAG = WebListActivity.class.getCanonicalName();

	private final static String TITLE = "titles";
	private final static String SUBJECT = "subjects";
	private final static String AUTHOR = "authors";
	private final static String COVER = "coverPath";
	private final static String DOCLINK = "epubInfo";
	private final static String DOCID = "id";
	private final static int HTTP_RESPONSE_OK = 200;

	private AbsListView list;
	private ArrayList<BookLink> docList;
	private BookLinkAdapter docAdapter;
	private String docDown;
	private Bundle args;

	public ProgressBar mUpdateProgress;
	public View mProgressOverlay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		final Context context = this;

		docList = new ArrayList<BookLink>();
		docAdapter = new BookLinkAdapter(this, R.layout.list_item, docList);
		list = (AbsListView) findViewById(android.R.id.list);
		if (android.os.Build.VERSION.SDK_INT < 11) {
			((ListView) list).setAdapter(docAdapter);
		} else {
			// only possible since API level 11 (Honeycomb)
			list.setAdapter(docAdapter);
		}

		list.setFastScrollEnabled(true);
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				BookLink docLink = (BookLink) list.getAdapter().getItem(
						position);
				docDown = docLink.getId();
				if (Utils.checkExternalMedia()) {
					args = new Bundle();
					args.putString("docID", docDown);
					args.putString("docName", docLink.getMeta().getFirstTitle());
					// getSupportLoaderManager().restartLoader(
					// Constants.DOWNLOAD_DOCUMENT, args, callBacks);

				} else {
					// Toast.makeText(
					// this,
					// "SDCard is not mounted.Please mount your sdcard and try again",
					// Toast.LENGTH_LONG).show();
				}
			}
		});
		list.setOnScrollListener(this);
		View emptyView = findViewById(android.R.id.empty);
		if (emptyView != null) {
			list.setEmptyView(emptyView);
		}

		getSupportLoaderManager().initLoader(Constants.DOWNLOAD_DOCLIST, null,
				this);
		registerForContextMenu(list);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.store_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			refreshList();
		case R.id.menu_search:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Loader<List<BookLink>> onCreateLoader(int id, Bundle args) {
		AsyncTaskLoader<List<BookLink>> loader = new AsyncTaskLoader<List<BookLink>>(
				this) {

			@Override
			public List<BookLink> loadInBackground() {
				List<BookLink> result = null;
				RestClient client = new RestClient(Constants.URL);
				try {
					Log.d("loader", "loader client execute");
					client.Execute(RequestMethod.GET);
					String responseString = client.getResponse();
					int responseCode = client.getResponseCode();
					if (responseCode == HTTP_RESPONSE_OK) {
						result = parseXMLResult(responseString);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return result;
			}
		};
		// somehow the AsyncTaskLoader doesn't want to start its job without
		// calling this method
		loader.forceLoad();
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<BookLink>> loader,
			List<BookLink> data) {
		if (data != null && data.size() > 0) {
			docList.addAll(data);
			docAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public void onLoaderReset(Loader<List<BookLink>> loader) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void refreshList() {
		getSupportLoaderManager().restartLoader(Constants.DOWNLOAD_DOCLIST,
				null, this);
		showDialog(Constants.DOWNLOAD_DOCLIST);
	}

	/**
	 * Parse XML result into data objects.
	 * 
	 * @param xmlString
	 * @return
	 */
	private List<BookLink> parseXMLResult(String xmlString) {

		ArrayList<BookLink> docs = null;
		InputStream xmlInputStream = Utils.parseStringToIS(xmlString);
		try {

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();

			parser.setInput(xmlInputStream, null);
			int type = parser.getEventType();

			BookLink currentItem = null;
			ArrayList<String> titles = null;
			ArrayList<String> subjects = null;
			String name = "";
			boolean done = false;
			while (type != XmlPullParser.END_DOCUMENT && !done) {
				switch (type) {
				case XmlPullParser.START_DOCUMENT:
					docs = new ArrayList<BookLink>();
					break;
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(DOCLINK)) {
						currentItem = new BookLink();
						titles = new ArrayList<String>();
						subjects = new ArrayList<String>();
					} else if (currentItem != null) {
						if (name.equalsIgnoreCase(TITLE)) {
							titles.add(parser.nextText());
						} else if (name.equalsIgnoreCase(AUTHOR)) {
							// currentItem.getMeta().addAuthor(new
							// Author(parser.nextText()));
						} else if (name.equalsIgnoreCase(SUBJECT)) {
							subjects.add(parser.nextText());
						} else if (name.equalsIgnoreCase(COVER)) {
							Resource cover = new Resource(parser.nextText());
							currentItem.getMeta().setCoverImage(cover);
						} else if (name.equalsIgnoreCase(DOCID)) {
							currentItem.setId(parser.nextText());
						}
					}

					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(DOCLINK) && currentItem != null) {
						Metadata meta = new Metadata();
						meta.setTitles(titles);
						meta.setSubjects(subjects);
						currentItem.setMeta(meta);
						currentItem
								.setCoverUrl("http://naturescrusaders.files.wordpress.com/2009/02/gex_green-sea-turtle.jpg"); // TODO:remove.
						docs.add(currentItem);
					} else if (name.equalsIgnoreCase("epubInfoes")) {
						done = true;
					}
					break;
				}
				type = parser.next();
			}
		} catch (Exception ex) {
			Log.e("Exception parsing xml", ex.getMessage());
		}

		return docs;

	}

}
