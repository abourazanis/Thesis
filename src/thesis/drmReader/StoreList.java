package thesis.drmReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import thesis.drmReader.RestClient.RequestMethod;
import thesis.sec.Decrypter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class StoreList extends ListActivity {

	private class StoreListState {
		private GetDocListTask docListTask = null;
		private GetDocumentTask docTask = null;
		private ArrayList<BookLink> docList;
		private int dialogId = 999; //dummy value
		

		public void setDialogId(int id){
			dialogId = id;
		}
		
		public int getDialogId(){
			return dialogId;
		}
		
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

		public ArrayList<BookLink> getDocList() {
			return docList;
		}

		public void setDocList(ArrayList<BookLink> docList) {
			this.docList = docList;
		}
	}

	private ArrayList<BookLink> docList;
	private BookLinkAdapter docLinkAdapter;
	private boolean isDialogShowing;
	private int dialogId = 999;
	private String docDown;
	private GetDocumentTask docTask;
	private GetDocListTask docListTask;

	private final static String URL = "http://10.0.2.2:8080/thesis.server/rest/epubs";
	private final static String GETDOCURL = "http://10.0.2.2:8080/thesis.server/rest/epubs/";
	private final static String TITLE = "titles";
	private final static String SUBJECT = "subjects";
	private final static String AUTHOR = "authors";
	private final static String COVER = "coverPath";
	private final static String DOCLINK = "epubInfo";
	private final static String DOCID = "id";

	private final static int HTTP_RESPONSE_OK = 200;
	private final static int DOWNLOAD_DOCUMENT = 0;
	private final static int DOWNLOAD_DOCLIST = 1;
	private final static int DOWNLOAD_DOCLIST_ALERT = 2;
	private final static int DOWNLOAD_DOCUMENT_ALERT = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		docList = new ArrayList<BookLink>();
		docLinkAdapter = new BookLinkAdapter(this, R.layout.list_item,
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
			showDialog(state.getDialogId());

		} else {
			docListTask = new GetDocListTask(this);
			docListTask.execute(URL);
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.store_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			if(docListTask.getStatus() == AsyncTask.Status.FINISHED){
				docListTask = new GetDocListTask(this);
				docListTask.execute(URL);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
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
		state.setDialogId(dialogId);

		return (state);
	}
	
	@Override
    public void onDestroy()
    {
		docLinkAdapter.imageLoader.stopThread();
        this.setListAdapter(null);
        super.onDestroy();
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		BookLink docLink = (BookLink) this.getListAdapter().getItem(
				position);
		docDown = docLink.getId();
		if (checkExternalMedia()) {
			docTask = new GetDocumentTask(this);
			docTask.execute(docDown,docLink.getMeta().getFirstTitle());
		} else {
			// message show
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog progressDialog = null;
		switch (id) {
		case DOWNLOAD_DOCUMENT:
			isDialogShowing = true;
			dialogId = DOWNLOAD_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document..");
			// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case DOWNLOAD_DOCLIST:
			isDialogShowing = true;
			dialogId = DOWNLOAD_DOCLIST;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document list..");
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case DOWNLOAD_DOCLIST_ALERT:
			dialogId = DOWNLOAD_DOCLIST_ALERT;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("An network error occurred during the document list download process.Please try again or go back.")
			       .setCancelable(false)
					// .setTitle("Attention")
					// .setIcon(R.drawable.ic_stat_alert)
			       .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   docListTask = new GetDocListTask(StoreList.this);
			   			   docListTask.execute(URL);
			           }
			       })
			       .setNegativeButton("Back", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			return  builder.create();
		case DOWNLOAD_DOCUMENT_ALERT:
			dialogId = DOWNLOAD_DOCUMENT_ALERT;
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setMessage("An network error occurred during the document download process.Please try again or go back.")
			       .setCancelable(false)
//			       .setTitle("Attention")
//			       .setIcon(R.drawable.ic_stat_alert)
			       .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   docTask = new GetDocumentTask(StoreList.this);
			   			   docTask.execute(docDown);
			           }
			       })
			       .setNegativeButton("Back", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			return  builder2.create();

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
	private List<BookLink> parseXMLResult(String xmlString) {

		ArrayList<BookLink> docs = null;
		InputStream xmlInputStream = parseStringToIS(xmlString);
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

	class GetDocListTask extends AsyncTask<String, String, String> {

		StoreList activity = null;
		String responseString = null;
		int responseCode;
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
			responseCode = client.getResponseCode();

			return responseString;

		}

		@Override
		protected void onPostExecute(String result) {
			completed = true;
			if (responseCode == HTTP_RESPONSE_OK) {
				activity.docList.addAll(activity.parseXMLResult(result));
				activity.docLinkAdapter.notifyDataSetChanged();
			}
			if (activity.isDialogShowing){
				activity.dismissDialog(DOWNLOAD_DOCLIST);
				activity.dialogId = 999;
			}
			if (responseCode != HTTP_RESPONSE_OK) {
				activity.showDialog(DOWNLOAD_DOCLIST_ALERT);
			}

		}

		void detach() {
			activity = null;
		}

		void attach(StoreList activity) {
			this.activity = activity;
			if (activity.isDialogShowing){
				activity.dismissDialog(DOWNLOAD_DOCLIST);
				activity.dialogId = 999;
			}
		}

	}

	class GetDocumentTask extends AsyncTask<String, String, String> {

		StoreList activity = null;
		boolean completed = false;
		int responseCode;

		GetDocumentTask(StoreList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(DOWNLOAD_DOCUMENT);
		}

		@Override
		protected String doInBackground(String... params) {

			RestClient client = new RestClient(GETDOCURL + params[0], true,
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

		protected void onProgressUpdate(String... progress) {
			// progressDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(String result) {
			completed = true;
			super.onPostExecute(result);
			if (activity.isDialogShowing){
				activity.dismissDialog(DOWNLOAD_DOCUMENT);
				activity.dialogId = 999;
			}
			if (responseCode != HTTP_RESPONSE_OK) {
				activity.showDialog(DOWNLOAD_DOCUMENT_ALERT);
			}
			
			Intent i = new Intent();
			i.putExtra("filepath", result);
		    i.setAction("thesis.drmReader.POPULATE_LIST");
		    sendBroadcast(i);

		}

		void detach() {
			activity = null;
		}

		void attach(StoreList activity) {
			this.activity = activity;
			if (completed) {
				if (activity.isDialogShowing){
					activity.dismissDialog(DOWNLOAD_DOCUMENT);
					activity.dialogId = 999;
				}
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
