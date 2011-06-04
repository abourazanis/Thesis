package thesis.drmReader;

import java.io.File;
import java.util.ArrayList;

import thesis.pedlib.ped.Document;
import thesis.pedlib.ped.PedReader;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
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

public class ArchiveList extends ListActivity {

	private class ArchiveListState {
		private ListDocumentsTask listTask;
		private ArrayList<Document> docList;

		public ListDocumentsTask getListTask() {
			return listTask;
		}

		public void setListTask(ListDocumentsTask listTask) {
			this.listTask = listTask;
		}

		public ArrayList<Document> getDocList() {
			return docList;
		}

		public void setDocList(ArrayList<Document> docList) {
			this.docList = docList;
		}
	}

	private ArrayList<Document> docList;
	private DocumentAdapter docAdapter;
	private ListDocumentsTask listTask;
	private PedReader reader;
	private boolean isDialogShowing = false;
	private final static int LIST_DOCUMENTS = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);

		reader = new PedReader();
		docList = new ArrayList<Document>();
		docAdapter = new DocumentAdapter(this, R.layout.list_item, docList);
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

		} else {
			listTask = new ListDocumentsTask(this);
			listTask.execute((Void) null);
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

		return (state);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		isDialogShowing = true;
		ProgressDialog progressDialog = null;
		switch (id) {
		case LIST_DOCUMENTS:
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
		Document doc = (Document) this.getListAdapter().getItem(position);
		Intent intent = new Intent(this, ReaderView.class);
		intent.putExtra("docSrc", doc.getPedPath());

		startActivity(intent);

	}

	private void deleteDoc(int position) {
		Document doc = (Document) this.getListAdapter().getItem(position);
		docAdapter.remove(doc);

		File fileToRm = new File(doc.getPedPath());
		if (fileToRm.exists())
			fileToRm.delete();

		docAdapter.notifyDataSetChanged();
	}

	private class ListDocumentsTask extends AsyncTask<Void, Void, Void> {

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
		protected Void doInBackground(Void... params) {
			File sdDir = Environment.getExternalStorageDirectory();
			if (sdDir.exists() && sdDir.canRead()) {
				// File docDir = new File(sdDir.getAbsolutePath() +
				// "/drmReader");
				File docDir = new File(sdDir.getAbsolutePath());
				if (docDir.exists() && docDir.canRead()) {
					docList = new ArrayList<Document>();
					String[] fileList = docDir.list();
					for (String filename : fileList) {
						if (filename.endsWith(".ped")) {
							String pedFilePath = sdDir.getAbsolutePath() + "/"
									+ filename;
							Document doc = reader.getPedPreview(pedFilePath,
									"UTF-8");
							// TODO:remove
							doc.setPedPath(pedFilePath);
							docList.add(doc);
						}
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			completed = true;
			if (docList != null && docList.size() > 0) {
				docAdapter.notifyDataSetChanged();
				for (int i = 0; i < docList.size(); i++)
					docAdapter.add(docList.get(i));
			}
			if (activity.isDialogShowing)
				activity.dismissDialog(LIST_DOCUMENTS);
			docAdapter.notifyDataSetChanged();

			super.onPostExecute(result);
		}

		void detach() {
			activity = null;
		}

		void attach(ArchiveList activity) {
			this.activity = activity;
			if (completed) {
				if (activity.isDialogShowing)
					activity.dismissDialog(LIST_DOCUMENTS);
			}
		}

	}

}