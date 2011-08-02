package thesis.drmReader.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.R;
import thesis.drmReader.RestClient;
import thesis.drmReader.RestClient.RequestMethod;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.Utils;
import thesis.imageLazyLoader.ImageLoader;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class WebListFragment extends ListFragment implements
		LoaderCallbacks<List<BookLink>>{

	private ParentActivity mParent;
	// This is the Adapter being used to display the list's data.
	WebListAdapter mAdapter;

	// If non-null, this is the current filter the user has provided.
	String mCurFilter;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mParent = (ParentActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ParentActivity");
        }
    }
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Give some text to display if there is no data. In a real
		// application this would come from a resource.
		setEmptyText("No epubs");
		setRetainInstance(true);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new WebListAdapter(getActivity(), R.layout.list_item);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		BookLink doc = this.mAdapter.getItem(position);
		mParent.downloadDocument(doc);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			refreshList();
			return true;
		default:
		return super.onOptionsItemSelected(item);
		}
	}


	public void setParent(ParentActivity activity){
		mParent = activity;
	}

	@Override
	public Loader<List<BookLink>> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader with no arguments, so it is simple.
		return new WebListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<BookLink>> loader,
			List<BookLink> data) {
		final WebListLoader wl = ((WebListLoader) loader);
		switch (wl.getResultCode()) {
		case Constants.OFFLINE: mParent.displayDialog(Constants.OFFLINE);
			break;
		case Constants.HTTP_RESPONSE_OK: {
			// Set the new data in the adapter.
			mAdapter.setData(data);
		}
			break;
		default:// error
			mParent.displayDialog(Constants.DOWNLOAD_DOCLIST_ALERT);

		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<BookLink>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}
	
	public void refreshList() {
		this.setListShown(false);
		getLoaderManager().restartLoader(0,null, this);
	}


	public static class WebListAdapter extends ArrayAdapter<BookLink> {
		private final LayoutInflater mInflater;
		private final int mLayout;
		public ImageLoader imageLoader;

		public WebListAdapter(Context context, int layout) {
			super(context, layout);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
			imageLoader = new ImageLoader(context);
		}

		public void setData(List<BookLink> data) {
			clear();
			if (data != null) {
				for (BookLink item : data) {
					add(item);
				}
			}
		}

		public final class ViewHolder {

			public ImageView imageView;
			public TextView textViewTitle;
			public TextView textViewAuthor;
			public TextView textViewLanguage;
			public TextView textViewPublisher;
		}

		/**
		 * Populate new items in the list.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;

			if (convertView == null) {
				convertView = mInflater.inflate(mLayout, null);

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

			BookLink doc = this.getItem(position);
			if (doc != null) {

				Metadata meta = doc.getMeta();
				Author author = meta.getAuthors().size() > 0 ? meta
						.getAuthors().get(0) : null;
				String publisher = meta.getPublishers().size() > 0 ? meta
						.getPublishers().get(0) : null;

				imageLoader.DisplayImage(doc.getCoverUrl(),
						viewHolder.imageView);
				viewHolder.textViewTitle.setText(meta.getFirstTitle());
				viewHolder.textViewLanguage.setText(meta.getLanguage());

				if (publisher != null)
					viewHolder.textViewPublisher.setText(publisher);
				if (author != null)
					viewHolder.textViewAuthor.setText(author.getFirstname()
							+ " " + author.getLastname());
			}

			return convertView;
		}
	}

	/**
	 * A custom Loader that loads all of the installed applications.
	 */
	public static class WebListLoader extends AsyncTaskLoader<List<BookLink>> {

		private final static String TITLE = "titles";
		private final static String SUBJECT = "subjects";
		private final static String AUTHOR = "authors";
		private final static String COVER = "coverPath";
		private final static String DOCLINK = "epubInfo";
		private final static String DOCID = "id";

		private List<BookLink> mLinks;
		private int mResultCode;

		public WebListLoader(Context context) {
			super(context);
		}

		/**
		 * This is where the bulk of our work is done. This function is called
		 * in a background thread and should generate a new set of data to be
		 * published by the loader.
		 */
		@Override
		public List<BookLink> loadInBackground() {
			List<BookLink> result = null;
			RestClient client = new RestClient(Constants.URL);
			if (!Utils.isNetworkAvailable(this.getContext()))
				mResultCode = Constants.OFFLINE;
			else {
				try {
					client.Execute(RequestMethod.GET);
					String responseString = client.getResponse();
					int responseCode = client.getResponseCode();
					if (responseCode == Constants.HTTP_RESPONSE_OK) {
						result = parseXMLResult(responseString);
					}
					mResultCode = responseCode;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return result;
		}

		/**
		 * Called when there is new data to deliver to the client. The super
		 * class will take care of delivering it; the implementation here just
		 * adds a little more logic.
		 */
		@Override
		public void deliverResult(List<BookLink> links) {
			if (isReset()) {
				// An async query came in while the loader is stopped. We
				// don't need the result.
				if (links != null) {
					onReleaseResources(links);
				}
			}
			List<BookLink> oldApps = links;
			mLinks = links;

			if (isStarted()) {
				// If the Loader is currently started, we can immediately
				// deliver its results.
				super.deliverResult(links);
			}

			// At this point we can release the resources associated with
			// 'oldApps' if needed; now that the new result is delivered we
			// know that it is no longer in use.
			if (oldApps != null) {
				onReleaseResources(oldApps);
			}
		}

		/**
		 * Handles a request to start the Loader.
		 */
		@Override
		protected void onStartLoading() {
			if (mLinks != null) {
				deliverResult(mLinks);
			}

			if (takeContentChanged() || mLinks == null) {
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
		public void onCanceled(List<BookLink> links) {
			super.onCanceled(links);

			// At this point we can release the resources associated with 'apps'
			// if needed.
			onReleaseResources(links);
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
			if (mLinks != null) {
				onReleaseResources(mLinks);
				mLinks = null;
			}
		}

		/**
		 * Helper function to take care of releasing resources associated with
		 * an actively loaded data set.
		 */
		protected void onReleaseResources(List<BookLink> links) {
			// For a simple List<> there is nothing to do. For something
			// like a Cursor, we would close it here.
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

				XmlPullParserFactory factory = XmlPullParserFactory
						.newInstance();
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
						if (name.equalsIgnoreCase(DOCLINK)
								&& currentItem != null) {
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

		public int getResultCode() {
			return mResultCode;
		}
	}
		

}
