package thesis.drmReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import thesis.sec.Decrypter;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
		docAdapter.remove(doc);

		File fileToRm = new File(doc.getId());
		if (fileToRm.exists())
			fileToRm.delete();

		docAdapter.notifyDataSetChanged();
	}

	private class ListDocumentsTask extends AsyncTask<Void, Void, Void> {

		ArchiveList activity = null;
		boolean completed = false;
		
		ArrayList<String> invalidFiles = new ArrayList<String>();

		ListDocumentsTask(ArchiveList activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			activity.showDialog(LIST_DOCUMENTS);
		}

		@Override
		protected Void doInBackground(Void... params) {
			File sdDir = Environment.getExternalStorageDirectory();
			if (sdDir.exists() && sdDir.canRead()) {
				// File docDir = new File(sdDir.getAbsolutePath() +
				// "/drmReader");
				File docDir = new File(sdDir.getAbsolutePath());
				if (docDir.exists() && docDir.canRead()) {
					docList = new ArrayList<BookLink>();
					String[] fileList = docDir.list();
					for (String filename : fileList) {
						if (filename.endsWith(".epub")) {
							String epubFilePath = sdDir.getAbsolutePath() + "/"
									+ filename;
							try {
								FileInputStream epubStream = new FileInputStream(
										epubFilePath);
								BookLink epubLink = new BookLink();
								
								Decrypter decrypter = new Decrypter(epubFilePath, activity);
								Metadata meta = (new EpubReader(decrypter))
										.readEpubMetadata(epubStream);
								
								epubLink.setMeta(meta);
								epubLink.setId(epubFilePath);
								if (meta.getCoverImage() != null)
									epubLink.setCoverUrl(meta.getCoverImage()
											.getHref());
								docList.add(epubLink);
							} catch (InvalidKeyException e) {
								invalidFiles.add(filename);
							} catch (FileNotFoundException e) {
								Log.e(TAG, e.getMessage());
							} catch (IOException e) {
								Log.e(TAG, e.getMessage());
							}
						}
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(!invalidFiles.isEmpty()){
				Toast.makeText(activity, "Found epub(s) with name(s) " + invalidFiles.toString() + " who belongs to other device.", Toast.LENGTH_LONG).show();
			}
			completed = true;
			if (docList != null && docList.size() > 0) {
				docAdapter.notifyDataSetChanged();
				for (int i = 0; i < docList.size(); i++)
					docAdapter.add(docList.get(i));
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
				listTask = new ListDocumentsTask(activity);
				listTask.execute((Void) null);
			}
		}
	}

}