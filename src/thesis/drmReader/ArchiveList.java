package thesis.drmReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.IOUtil;
import thesis.drmReader.data.EpubDbAdapter;
import thesis.drmReader.file.FileBrowser;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ArchiveList extends ListActivity {

	private class ArchiveListState {
		private ListDocumentsTask listTask;
		private ArrayList<BookLink> docList;
		private int dialogId = 999; // dummy value

		public void setDialogId(int id) {
			dialogId = id;
		}

		public int getDialogId() {
			return dialogId;
		}

		public ListDocumentsTask getListTask() {
			return listTask;
		}

		public void setListTask(ListDocumentsTask listTask) {
			this.listTask = listTask;
		}

		public ArrayList<BookLink> getDocList() {
			return docList;
		}

		public void setDocList(ArrayList<BookLink> docList) {
			this.docList = docList;
		}
	}

	private final static String TAG = "ArchiveList";

	private ArrayList<BookLink> docList;
	private BookLinkAdapter docAdapter;
	private ListDocumentsTask listTask;
	private boolean isDialogShowing = false;
	private int dialogId = 999;
	private final static int LIST_DOCUMENTS = 0;
	private final static int IMPORT_DOCUMENT = 1;
	private static final int IMPORT_REQUEST = 100;
	private MyListener listener = null;
	private Boolean myListenerIsRegistered = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		listener = new MyListener(this);

		docList = new ArrayList<BookLink>();
		docAdapter = new BookLinkAdapter(this, R.layout.list_item, docList);
		setListAdapter(docAdapter);
		registerForContextMenu(getListView());

		ArchiveListState state = (ArchiveListState) getLastNonConfigurationInstance();
		if (state != null) {
			if (state.getListTask() != null) {
				listTask = state.getListTask();
				listTask.attach(this);
			}

			if (state.getDocList() != null && docList.size() == 0) {
				docList.addAll(state.getDocList());
				docAdapter.notifyDataSetChanged();
			}
			showDialog(state.getDialogId());

		} else {
			listTask = new ListDocumentsTask(this);
			listTask.execute((Void) null);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!myListenerIsRegistered) {
			registerReceiver(listener, new IntentFilter(
					"thesis.drmReader.POPULATE_LIST"));
			myListenerIsRegistered = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// if (myListenerIsRegistered) {
		// unregisterReceiver(listener);
		// myListenerIsRegistered = false;
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.archive_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.importEpub:
			Intent i = new Intent(this, FileBrowser.class);
			this.startActivityForResult(i, IMPORT_REQUEST);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case IMPORT_REQUEST:
				String epubFile = data.getStringExtra("filepath");
				if (epubFile.endsWith(".epub")) {
					new ImportEpubTask(this).execute(epubFile);
				} else {
					Toast.makeText(
							this,
							"The file " + epubFile
									+ " has not a valid epub file extension",
							Toast.LENGTH_LONG).show();
				}
				break;
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		String itemTitle = ((TextView) info.targetView
				.findViewById(R.id.toptext)).getText().toString();

		MenuInflater inflater = getMenuInflater();
		menu.setHeaderTitle(itemTitle);
		inflater.inflate(R.menu.archive_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = null;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e("contextmenu", "bad menuInfo", e);
		}

		switch (item.getItemId()) {
		case R.id.read_item:
			readDoc(info.position);
			return true;
		case R.id.delete_item:
			deleteDoc(info.position);
			return true;
		default:
			return super.onContextItemSelected(item);

		}

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		readDoc(position);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		ArchiveListState state = new ArchiveListState();
		if (listTask != null) {
			listTask.detach();
			state.setListTask(listTask);
		}
		state.setDocList(docList);
		state.setDialogId(dialogId);

		return (state);
	}

	@Override
	public void onDestroy() {
		docAdapter.imageLoader.stopThread();
		this.setListAdapter(null);
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		ProgressDialog progressDialog = null;
		switch (id) {
		case LIST_DOCUMENTS:
			isDialogShowing = true;
			dialogId = LIST_DOCUMENTS;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Populating list..");
			// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case IMPORT_DOCUMENT:
			isDialogShowing = true;
			dialogId = IMPORT_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Importing epub in library..");
			// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		default:
			return null;
		}
	}

	private void readDoc(int position) {
		BookLink doc = (BookLink) this.getListAdapter().getItem(position);
		Intent intent = new Intent(this, ReaderView.class);
		intent.putExtra("docSrc", doc.getId());

		startActivity(intent);

	}

	private void deleteDoc(int position) {
		BookLink doc = (BookLink) this.getListAdapter().getItem(position);

		EpubDbAdapter dbAdapter = new EpubDbAdapter(this).open();
		String filePath = dbAdapter.getEpubLocation(doc.getId());
		dbAdapter.deleteEpub(doc);
		dbAdapter.close();
		docAdapter.remove(doc);

		if (filePath != null) {
			File fileToRm = new File(filePath);
			if (fileToRm.exists())
				fileToRm.delete();
		}

		docAdapter.notifyDataSetChanged();
	}

	private class ImportEpubTask extends AsyncTask<String, Void, BookLink> {

		ArchiveList activity = null;
		boolean completed = false;
		String invalidFile = null;

		ImportEpubTask(ArchiveList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(IMPORT_DOCUMENT);
		}

		@Override
		protected BookLink doInBackground(String... params) {
			String epubFilePath = params[0];
			try {
				FileInputStream epubStream = new FileInputStream(epubFilePath);
				BookLink epubLink = new BookLink();

				Decrypter decrypter = new Decrypter(epubFilePath, activity);
				Metadata meta = (new EpubReader(decrypter))
						.readEpubMetadata(epubStream);

				epubLink.setMeta(meta);
				//epubLink.setId(epubFilePath);
				if (meta.getCoverImage() != null)
					epubLink.setCoverUrl(meta.getCoverImage().getHref());

				File sdDir = Environment.getExternalStorageDirectory();
				if (sdDir.exists() && sdDir.canRead()) {
					File docDir = new File(sdDir.getAbsolutePath()
							+ "/drmReader");
					if (!docDir.exists())
						docDir.mkdirs();
					if (docDir.exists() && docDir.canRead()) {
						String fileName = docDir + "/"
								+ epubLink.getMeta().getFirstTitle() + ".epub";
						IOUtil.copy(new FileInputStream(epubFilePath),
								new FileOutputStream(fileName));
						EpubDbAdapter dbAdapter = new EpubDbAdapter(activity)
								.open();
						long epubId = dbAdapter.createEpub(epubLink, fileName, meta.getCoverImage().getData());
						dbAdapter.close();
						
						epubLink.setId(String.valueOf(epubId));
						return epubLink;
					}
				}
			} catch (InvalidKeyException e) {
				invalidFile = epubFilePath;
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(BookLink result) {
			completed = true;
			if (invalidFile != null) {
				Toast.makeText(
						activity,
						"The epub with filename: " + invalidFile
								+ " is drmed for another device.",
						Toast.LENGTH_LONG).show();
			}

			if (result != null) {
				docAdapter.add(result);
				docAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(activity,
						"Something went wrong during the import process",
						Toast.LENGTH_LONG).show();
			}

			if (activity.isDialogShowing) {
				activity.dismissDialog(IMPORT_DOCUMENT);
				activity.dialogId = 999;
			}
			super.onPostExecute(result);
		}

		void detach() {
			activity = null;
		}

		void attach(ArchiveList activity) {
			this.activity = activity;
			if (completed) {
				if (activity.isDialogShowing) {
					activity.dismissDialog(IMPORT_DOCUMENT);
					activity.dialogId = 999;
				}
			}
		}

	}

	private class ListDocumentsTask extends
			AsyncTask<Void, Void, List<BookLink>> {

		ArchiveList activity = null;
		boolean completed = false;

		ListDocumentsTask(ArchiveList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(LIST_DOCUMENTS);
		}

		@Override
		protected List<BookLink> doInBackground(Void... params) {
			EpubDbAdapter dbAdapter = new EpubDbAdapter(activity).open();
			List<BookLink> list = dbAdapter.getEpubs();
			dbAdapter.close();

			return list;
		}

		@Override
		protected void onPostExecute(List<BookLink> result) {
			completed = true;
			if (result != null && result.size() > 0) {
				docAdapter.notifyDataSetChanged();
				for (int i = 0; i < result.size(); i++)
					docAdapter.add(result.get(i));
			}
			if (activity.isDialogShowing) {
				activity.dismissDialog(LIST_DOCUMENTS);
				activity.dialogId = 999;
			}
			docAdapter.notifyDataSetChanged();

			super.onPostExecute(result);
		}

		void detach() {
			activity = null;
		}

		void attach(ArchiveList activity) {
			this.activity = activity;
			if (completed) {
				if (activity.isDialogShowing) {
					activity.dismissDialog(LIST_DOCUMENTS);
					activity.dialogId = 999;
				}
			}
		}

	}

	protected class MyListener extends BroadcastReceiver {
		private ArchiveList activity;

		MyListener(ArchiveList activity) {
			this.activity = activity;
		}

		@Override
		public void onReceive(Context context, Intent intent) {

			// No need to check for the action unless the listener will
			// will handle more than one - let's do it anyway
			if (intent.getAction().equals("thesis.drmReader.POPULATE_LIST")) {
				File root = android.os.Environment
						.getExternalStorageDirectory();
				String epubFilePath =  root.getAbsolutePath() + "/" + intent.getStringExtra("filepath");
				new ImportEpubTask(activity).execute(epubFilePath);
			}
		}
	}

}