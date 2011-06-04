package thesis.drmReader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.RestClient.RequestMethod;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class StoreList extends ListActivity {

	private class StoreListState {
		public GetDocListTask docListTask = null;
		public GetDocumentTask docTask = null;
		public ArrayList<DocumentLink> docList;

		public GetDocListTask getDocListTask() {
			return docListTask;
		}

		public void setDocListTask(GetDocListTask docListTask) {
			this.docListTask = docListTask;
		}

		public GetDocumentTask getDocTask() {
			return docTask;
		}

		public void setDocTask(GetDocumentTask docTask) {
			this.docTask = docTask;
		}

		public ArrayList<DocumentLink> getDocList() {
			return docList;
		}

		public void setDocList(ArrayList<DocumentLink> docList) {
			this.docList = docList;
		}
	}

	private ArrayList<DocumentLink> docList;
	private DocumentLinkAdapter docLinkAdapter;
	private boolean isDialogShowing;
	private String docDown;
	private GetDocumentTask docTask;
	private GetDocListTask docListTask;

	private final static String URL = "http://10.0.2.2:8080/thesis.server/rest/peds";
	private final static String GETDOCURL = "http://10.0.2.2:8080/thesis.server/rest/peds/";
	private final static String TITLE = "name";
	private final static String SUBJECT = "subject";
	private final static String AUTHOR = "author";
	private final static String COVER = "coverPath";
	private final static String DOCLINK = "pedInfo";
	private final static String DOCID = "id";

	public final static int DOWNLOAD_DOCUMENT = 0;
	public final static int DOWNLOAD_DOCLIST = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		docList = new ArrayList<DocumentLink>();
		docLinkAdapter = new DocumentLinkAdapter(this, R.layout.list_item,
				docList);
		setListAdapter(docLinkAdapter);
		registerForContextMenu(getListView());

		StoreListState state = (StoreListState) getLastNonConfigurationInstance();
		if (state != null) {
			if (state.getDocTask() != null) {
				docTask = state.getDocTask();
				docTask.attach(this);
			}

			if (state.getDocListTask() != null) {
				docListTask = state.getDocListTask();
				docListTask.attach(this);
			}

			if (state.getDocList() != null && docList.size() == 0) {
				docList.addAll(state.getDocList());
				docLinkAdapter.notifyDataSetChanged();
			}

		} else {
			docListTask = new GetDocListTask(this);
			docListTask.execute(URL);
		}

	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		StoreListState state = new StoreListState();
		if (docListTask != null) {
			docListTask.detach();
			state.setDocListTask(docListTask);
		}
		if (docTask != null) {
			docTask.detach();
			state.setDocTask(docTask);	
		}
		state.setDocList(docList);

		return (state);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		DocumentLink docLink = (DocumentLink) this.getListAdapter().getItem(
				position);
		docDown = docLink.getId();
		if (checkExternalMedia()) {
			docTask = new GetDocumentTask(this);
			docTask.execute(docDown);
		} else {
			// message show
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		isDialogShowing = true;
		ProgressDialog progressDialog = null;
		switch (id) {
		case DOWNLOAD_DOCUMENT:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document..");
			// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case DOWNLOAD_DOCLIST:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document list..");
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;

		default:
			return null;
		}
	}

	/**
	 * Parse XML result into data objects.
	 * 
	 * @param xmlString
	 * @return
	 */
	private List<DocumentLink> parseXMLResult(String xmlString) {

		ArrayList<DocumentLink> docs = null;
		InputStream xmlInputStream = parseStringToIS(xmlString);
		try {

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();

			parser.setInput(xmlInputStream, null);
			int type = parser.getEventType();

			DocumentLink currentItem = null;
			String name = "";
			boolean done = false;
			while (type != XmlPullParser.END_DOCUMENT && !done) {
				switch (type) {
				case XmlPullParser.START_DOCUMENT:
					docs = new ArrayList<DocumentLink>();
					break;
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(DOCLINK)) {

						currentItem = new DocumentLink();
					} else if (currentItem != null) {
						if (name.equalsIgnoreCase(TITLE)) {
							currentItem.setTitle(parser.nextText());
						} else if (name.equalsIgnoreCase(AUTHOR)) {
							currentItem.setAuthor(parser.nextText());
						} else if (name.equalsIgnoreCase(SUBJECT)) {
							currentItem.setCoverUrl(parser.nextText());
						} else if (name.equalsIgnoreCase(COVER)) {
							currentItem.setCoverUrl(parser.nextText());
						} else if (name.equalsIgnoreCase(DOCID)) {
							currentItem.setId(parser.nextText());
						}
					}

					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase(DOCLINK) && currentItem != null) {
						docs.add(currentItem);
					} else if (name.equalsIgnoreCase("pedInfoes")) {
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

	class GetDocListTask extends AsyncTask<String, String, String> {

		StoreList activity = null;
		String responseString = null;
		boolean completed = false;

		GetDocListTask(StoreList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(DOWNLOAD_DOCLIST);
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

			return responseString;

		}

		@Override
		protected void onPostExecute(String result) {
			completed = true;
			activity.docList.addAll(activity.parseXMLResult(result));
			activity.docLinkAdapter.notifyDataSetChanged();
			if (activity.isDialogShowing)
				activity.dismissDialog(DOWNLOAD_DOCLIST);
		}

		void detach() {
			activity = null;
		}

		void attach(StoreList activity) {
			this.activity = activity;
				if (activity.isDialogShowing)
					activity.dismissDialog(DOWNLOAD_DOCLIST);
			}

	}

	class GetDocumentTask extends AsyncTask<String, String, Void> {

		StoreList activity = null;
		boolean completed = false;

		GetDocumentTask(StoreList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(DOWNLOAD_DOCUMENT);
		}

		@Override
		protected Void doInBackground(String... params) {

			RestClient client = new RestClient(GETDOCURL + params[0], true, params[0] + ".ped");
			try {
				client.Execute(RequestMethod.GET);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(String... progress) {
			// progressDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(Void result) {
			completed = true;
			super.onPostExecute(result);
			if (activity.isDialogShowing)
				activity.dismissDialog(DOWNLOAD_DOCUMENT);
		}

		void detach() {
			activity = null;
		}

		void attach(StoreList activity) {
			this.activity = activity;
			if (completed) {
				if (activity.isDialogShowing)
					activity.dismissDialog(DOWNLOAD_DOCUMENT);
			}
		}
	
	}

	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(l
					+ " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}

	public InputStream parseStringToIS(String xml) {
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		} catch (Exception e) {

		}
		return is;
	}

	/** Method to check whether external media available and writable. */

	private boolean checkExternalMedia() {
		boolean stat;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// Can read and write the media
			stat = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// Can only read the media
			stat = false;
		} else {
			// Can't read or write
			stat = false;
		}

		return stat;
	}

}
