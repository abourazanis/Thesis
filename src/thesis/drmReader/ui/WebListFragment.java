package thesis.drmReader.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.BookLink;
import thesis.drmReader.BookLinkAdapter;
import thesis.drmReader.R;
import thesis.drmReader.RestClient;
import thesis.drmReader.RestClient.RequestMethod;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.Utils;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

public class WebListFragment extends ListFragment{
	
	private ArrayList<BookLink> docList;
	private BookLinkAdapter docLinkAdapter;
	private GetDocumentTask docTask;
	private GetDocListTask docListTask;
	private DialogHandler dialogHandler;
	private ImportListener importHandler;
	private String docDown;

	private final static String TITLE = "titles";
	private final static String SUBJECT = "subjects";
	private final static String AUTHOR = "authors";
	private final static String COVER = "coverPath";
	private final static String DOCLINK = "epubInfo";
	private final static String DOCID = "id";

	private final static int HTTP_RESPONSE_OK = 200;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onAttach(FragmentActivity activity) {
		super.onAttach(activity);
		try {
			dialogHandler = (DialogHandler) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement DialogHandler");
		}
		
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
		
		docList = new ArrayList<BookLink>();
		docLinkAdapter = new BookLinkAdapter(this.getActivity(), R.layout.list_item,
				docList);
		setListAdapter(docLinkAdapter);
		registerForContextMenu(getListView());
		
		docListTask = new GetDocListTask(this.getActivity());
		docListTask.execute(Constants.URL);
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
			if(docListTask.getStatus() == AsyncTask.Status.FINISHED){
				docListTask = new GetDocListTask(this.getActivity());
				docListTask.execute(Constants.URL);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		BookLink docLink = (BookLink) this.getListAdapter().getItem(
				position);
		docDown = docLink.getId();
		if (Utils.checkExternalMedia()) {
			docTask = new GetDocumentTask(this.getActivity());
			docTask.execute(docDown,docLink.getMeta().getFirstTitle());
		} else {
			Toast.makeText(
					this.getActivity(),
					"SDCard is not mounted.Please mount your sdcard and try again",
					Toast.LENGTH_LONG).show();
		}

	}
	
	public void refreshList(){
		new GetDocListTask(this.getActivity()).execute(Constants.URL);
	}
	
	public void downloadDocument(){
		new GetDocumentTask(this.getActivity()).execute(docDown);
	}
	
	
	class GetDocListTask extends AsyncTask<String, String, String> {

		Activity activity = null;
		String responseString = null;
		int responseCode;
		boolean completed = false;

		GetDocListTask(Activity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialogHandler.displayDialog(Constants.DOWNLOAD_DOCLIST);
		}

		@Override
		protected String doInBackground(String... params) {

			RestClient client = new RestClient(params[0]);
			try {
				client.Execute(RequestMethod.GET);
			} catch (Exception e) {
				e.printStackTrace();
			}
			responseString = client.getResponse();
			responseCode = client.getResponseCode();

			return responseString;

		}

		@Override
		protected void onPostExecute(String result) {
			completed = true;
			if (responseCode == HTTP_RESPONSE_OK) {
				docList.addAll(parseXMLResult(result));
				docLinkAdapter.notifyDataSetChanged();
			}
			
			dialogHandler.hideDialog(Constants.DOWNLOAD_DOCLIST);
			if (responseCode != HTTP_RESPONSE_OK) {
				dialogHandler.displayDialog(Constants.DOWNLOAD_DOCLIST_ALERT);
			}

		}

	}

	class GetDocumentTask extends AsyncTask<String, Integer, String> {

		Activity activity = null;
		boolean completed = false;
		int responseCode;

		GetDocumentTask(Activity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialogHandler.displayDialog(Constants.DOWNLOAD_DOCUMENT);
		}

		@Override
		protected String doInBackground(String... params) {

			RestClient client = new RestClient(Constants.GETDOCURL + params[0], true,
					params[1] + ".epub");
			try {
				String key = new Decrypter(activity).getUniqueIdentifier();
				client.AddParam("key", key);
				client.Execute(RequestMethod.POST);
				responseCode = client.getResponseCode();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return params[1] + ".epub";
		}

		protected void onProgressUpdate(Integer... progress) {
			dialogHandler.setDialogProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			completed = true;
			super.onPostExecute(result);
				dialogHandler.hideDialog(Constants.DOWNLOAD_DOCUMENT);
			if (responseCode != HTTP_RESPONSE_OK) {
				dialogHandler.displayDialog(Constants.DOWNLOAD_DOCUMENT_ALERT);
			}
			importHandler.importEpub(result);
		}

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
							//currentItem.getMeta().addAuthor(new Author(parser.nextText()));
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
						currentItem.setCoverUrl("http://naturescrusaders.files.wordpress.com/2009/02/gex_green-sea-turtle.jpg"); //TODO:remove.
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

	
	public interface ImportListener{
		void importEpub(String file);
	}
	
}
