package thesis.drmReader;

import java.io.File;
import java.util.ArrayList;

import thesis.pedlib.ped.Document;
import thesis.pedlib.ped.PedReader;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
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

	private ArrayList<Document> docList;
	private DocumentAdapter docAdapter;
	private ProgressDialog progressDialog;
	private PedReader reader;
	private Runnable viewDocuments;

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

		viewDocuments = new Runnable() {
			@Override
			public void run() {
				getDocuments();
			}
		};

		Thread thread = new Thread(null, viewDocuments, "ViewDocuments");
		thread.start();
		progressDialog = ProgressDialog.show(ArchiveList.this,
				"Please wait...", "Retrieving data..", true);
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

	private void getDocuments() {

		File sdDir = Environment.getExternalStorageDirectory();
		if (sdDir.exists() && sdDir.canRead()) {
			// File docDir = new File(sdDir.getAbsolutePath() + "/drmReader");
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
		runOnUiThread(returnResults);
	}

	private Runnable returnResults = new Runnable() {

		@Override
		public void run() {
			if (docList != null && docList.size() > 0) {
				docAdapter.notifyDataSetChanged();
				for (int i = 0; i < docList.size(); i++)
					docAdapter.add(docList.get(i));
			}
			progressDialog.dismiss();
			docAdapter.notifyDataSetChanged();
		}
	};
	
	private void readDoc(int position){
		Document doc = (Document) this.getListAdapter().getItem(position);
		Intent intent = new Intent(this, ReaderView.class);
		intent.putExtra("docSrc", doc.getPedPath());

		startActivity(intent);
		
	}
	
	private void deleteDoc(int position){
		
	}
}