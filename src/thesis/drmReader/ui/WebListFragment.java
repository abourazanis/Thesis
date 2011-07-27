package thesis.drmReader.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.util.StringUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.R;
import thesis.drmReader.RestClient;
import thesis.drmReader.RestClient.RequestMethod;
import thesis.drmReader.ui.ArchiveListFragment.MyDialog;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.EpubLoader;
import thesis.drmReader.util.IncrementalAsyncTaskLoaderCallbacks;
import thesis.drmReader.util.Utils;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

public class WebListFragment extends ListFragment implements
		IncrementalAsyncTaskLoaderCallbacks<Integer, List<BookLink>> {

	private ArrayList<BookLink> docList;
	private BookLinkAdapter docLinkAdapter;
	private ImportListener importHandler;
	private String docDown;
	private EpubDownloaderCallBacks callBacks;

	private final static String TITLE = "titles";
	private final static String SUBJECT = "subjects";
	private final static String AUTHOR = "authors";
	private final static String COVER = "coverPath";
	private final static String DOCLINK = "epubInfo";
	private final static String DOCID = "id";

	private final static int HTTP_RESPONSE_OK = 200;

	private boolean firstRun = true;
	private boolean firstDownload = true;
	private Bundle args;
	private final Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			importHandler = (ImportListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement DialogHandler");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		registerForContextMenu(getListView());
		setRetainInstance(true);
		if (firstRun) {
			docList = new ArrayList<BookLink>();
			docLinkAdapter = new BookLinkAdapter(this.getActivity(),
					R.layout.list_item, docList);
			setListAdapter(docLinkAdapter);
			callBacks = new EpubDownloaderCallBacks(this.getActivity());
		}

		// call this to re-connect with an existing
		// loader (after screen configuration changes for e.g!)
		LoaderManager lm = getLoaderManager();
		if (lm.getLoader(Constants.DOWNLOAD_DOCLIST) != null) {
			lm.initLoader(Constants.DOWNLOAD_DOCLIST, null, this);
		}

		if (lm.getLoader(Constants.DOWNLOAD_DOCUMENT) != null) {
			lm.initLoader(Constants.DOWNLOAD_DOCUMENT, args, callBacks);
		}

		if (firstRun) {
			firstRun = false;
			getLoaderManager().initLoader(Constants.DOWNLOAD_DOCLIST, null,
					this);
			showDialog(Constants.DOWNLOAD_DOCLIST);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.doclist, container, false);
	}

	@Override
	public void onCreateOptionsMenu(android.support.v4.view.Menu menu,
			MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.store_list_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
				refreshList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		BookLink docLink = (BookLink) this.getListAdapter().getItem(position);
		docDown = docLink.getId();
		if (Utils.checkExternalMedia()) {
			args = new Bundle();
			args.putString("docID", docDown);
			args.putString("docName", docLink.getMeta().getFirstTitle());
			if (firstDownload) {
				firstDownload = false;
				getLoaderManager().initLoader(Constants.DOWNLOAD_DOCUMENT,
						args, callBacks);
				showDialog(Constants.DOWNLOAD_DOCUMENT);
			} else {
				getLoaderManager().restartLoader(Constants.DOWNLOAD_DOCUMENT,
						args, callBacks);
				showDialog(Constants.DOWNLOAD_DOCUMENT);
			}
		} else {
			Toast.makeText(
					this.getActivity(),
					"SDCard is not mounted.Please mount your sdcard and try again",
					Toast.LENGTH_LONG).show();
		}

	}

	public void refreshList() {
		if (firstRun) {
			getLoaderManager().initLoader(Constants.DOWNLOAD_DOCLIST, null,
					this);
		} else {
			getLoaderManager().restartLoader(Constants.DOWNLOAD_DOCLIST, null,
					this);
		}
		showDialog(Constants.DOWNLOAD_DOCLIST);
	}

	public void downloadDocument() {
		if (firstDownload) {
			firstDownload = false;
			getLoaderManager().initLoader(Constants.DOWNLOAD_DOCUMENT, args,
					callBacks);
		} else {
			getLoaderManager().restartLoader(Constants.DOWNLOAD_DOCUMENT, args,
					callBacks);
		}
		showDialog(Constants.DOWNLOAD_DOCUMENT);
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

	public interface ImportListener {
		void importEpub(String file);
	}

	@Override
	public Loader<List<BookLink>> onCreateLoader(int id, Bundle args) {
		EpubLoader loader = null;

		switch (id) {
		case Constants.DOWNLOAD_DOCLIST: {
			Log.d("loader", "loader created");
			loader = new EpubLoader(this.getActivity(),this,null) {

				@Override
				public List<BookLink> loadInBackground() {
					Log.d("loader", "loader inside background");
					List<BookLink> result = null;
					RestClient client = new RestClient(Constants.URL);
					try {
						Log.d("loader", "loader client execute");
						client.Execute(RequestMethod.GET);
						String responseString = client.getResponse();
						int responseCode = client.getResponseCode();
						this.file = Integer.toString(responseCode);
						if (responseCode == HTTP_RESPONSE_OK) {
							result = parseXMLResult(responseString);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					return result;
				}

			};
		}
			break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<BookLink>> loader,
			List<BookLink> data) {
		switch (loader.getId()) {
		case Constants.DOWNLOAD_DOCLIST: {
			Log.d("loader", "loader finished");
			if (data != null && data.size() > 0) {
				docList.addAll(data);
				docLinkAdapter.notifyDataSetChanged();
				hideDialog();
			} else {
				if(StringUtil.isNotBlank(((EpubLoader)loader).file)){
					showDialog(Constants.DOWNLOAD_DOCLIST_ALERT);
					((EpubLoader)loader).file = null;
				}
			}
		}
			break;
		}

	}

	@Override
	public void onLoaderReset(Loader<List<BookLink>> loader) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreExecute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProgressUpdate(Integer progress) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPostExecute(List<BookLink> result) {
		// TODO Auto-generated method stub

	}

	private class EpubDownloaderCallBacks implements
			IncrementalAsyncTaskLoaderCallbacks<Integer, String> {

		private Context context;

		public EpubDownloaderCallBacks(Context ctx) {
			this.context = ctx;
		}

		@Override
		public Loader<String> onCreateLoader(int id, Bundle args) {
			AsyncTaskLoader<String> loader = null;
			switch (id) {
			case Constants.DOWNLOAD_DOCUMENT: {

				final String fileID = args.getString("docID");
				final String fileName = args.getString("docName");
				loader = new AsyncTaskLoader<String>(this.context) {

					@Override
					public String loadInBackground() {
						String result = null;
						RestClient client = new RestClient(Constants.GETDOCURL
								+ fileID, true, fileName + ".epub");
						try {
							String key = new Decrypter(this.getContext())
									.getUniqueIdentifier();
							client.AddParam("key", key);
							client.Execute(RequestMethod.POST);
							if (client.getResponseCode() == HTTP_RESPONSE_OK)
								result = fileName + ".epub";
						} catch (Exception e) {
							e.printStackTrace();
						}
						return result;
					}
				};

			}
				break;
			}
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<String> loader, String data) {
			if (StringUtil.isNotBlank(data)) {
				importHandler.importEpub(data);
				hideDialog();
			} else {
				showDialog(Constants.DOWNLOAD_DOCUMENT_ALERT);
			}
		}

		@Override
		public void onLoaderReset(Loader<String> loader) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPreExecute() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProgressUpdate(Integer progress) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPostExecute(String result) {
			// TODO Auto-generated method stub

		}

	}
	
	private void setProgress(int progress) {
		MyDialog dialog = (MyDialog) this.getSupportFragmentManager()
				.findFragmentByTag("dialog");
		if (dialog != null)
			dialog.setProgress(progress);
	}
	
	private void showDialog(int id) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		
		DialogFragment newFragment = MyDialog.newInstance(id);
		//newFragment.show(ft, "dialog"); // Can not perform this action after onSaveInstanceState
		ft.add(newFragment, "dialog").commitAllowingStateLoss();
    }
	
	private void hideDialog() {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				Fragment prev = getSupportFragmentManager().findFragmentByTag(
						"dialog");
				if (prev != null) {
					ft.remove(prev).commit();
				}
			}
		});
	}
	
	
	public static class MyDialog extends DialogFragment {
		
		public static MyDialog newInstance(int id) {
			MyDialog frag = new MyDialog();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }
		
		 @Override
	        public Dialog onCreateDialog(Bundle savedInstanceState) {
	            int id = getArguments().getInt("id");
	            Dialog dialog = null;
	            switch(id){
	            case Constants.DOWNLOAD_DOCUMENT:
					dialog = new ProgressDialog(this.getActivity());
					((ProgressDialog)dialog).setMessage("Downloading document..");
					dialog.setCancelable(false);
					break;
				case Constants.DOWNLOAD_DOCLIST:
					dialog = new ProgressDialog(this.getActivity());
					((ProgressDialog)dialog).setMessage("Downloading document list..");
					dialog.setCancelable(false);
					break;
				case Constants.DOWNLOAD_DOCLIST_ALERT:
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.getActivity());
					builder.setMessage(
							"An network error occurred during the document list download process.Please try again or go back.")
							.setCancelable(false)
							// .setTitle("Attention")
							// .setIcon(R.drawable.ic_stat_alert)
							.setPositiveButton("Retry",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											//refreshList();
										}
									})
							.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											dialog.cancel();
										}
									});
					dialog = builder.create();
					break;
				case Constants.DOWNLOAD_DOCUMENT_ALERT:
					AlertDialog.Builder builder2 = new AlertDialog.Builder(
							this.getActivity());
					builder2.setMessage(
							"An network error occurred during the document download process.Please try again or go back.")
							.setCancelable(false)
							// .setTitle("Attention")
							.setIcon(R.drawable.ic_stat_alert)
							.setPositiveButton("Retry",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											//downloadDocument();
										}
									})
							.setNegativeButton("Back",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog,
												int id) {
											dialog.cancel();
										}
									});
					dialog = builder2.create();
					break;
	            }
	            return dialog;
		 }
		 
		 public void setProgress(int progress) {
			 ((ProgressDialog)this.getDialog()).setProgress(progress);
			}
	}
	

}
