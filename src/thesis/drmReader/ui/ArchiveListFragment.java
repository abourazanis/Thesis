package thesis.drmReader.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.IOUtil;

import thesis.drmReader.BookLink;
import thesis.drmReader.BookLinkAdapter;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubDbAdapter;
import thesis.drmReader.filebrowser.FileBrowser;
import thesis.drmReader.reader.ReaderView;
import thesis.drmReader.ui.QuickAction.ActionItem;
import thesis.drmReader.ui.QuickAction.QuickAction;
import thesis.drmReader.util.Constants;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class ArchiveListFragment extends ListFragment {

	private final static String TAG = "ArchiveListFragment";
	private ArrayList<BookLink> docList;
	private BookLinkAdapter docAdapter;
	private ListDocumentsTask listTask;
	private QuickAction quickAction;
	private int longClickedItemPos = -1;
	private DialogHandler dialogHandler;

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

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setHasOptionsMenu(true);

		docList = new ArrayList<BookLink>();
		docAdapter = new BookLinkAdapter(this.getActivity(),
				R.layout.list_item, docList);
		setListAdapter(docAdapter);

		initQuickAction();

		ListView lv = this.getListView(); // to get long click of list items
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
					long id) {
				longClickedItemPos = pos;
				quickAction.show(v);
				return true;
			}

		});

		listTask = new ListDocumentsTask(this.getActivity());
		listTask.execute((Void) null);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.doclist, container, false);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		readDoc(position);
	}

	@Override
	public void onCreateOptionsMenu(android.support.v4.view.Menu menu,
			MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.archive_list_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.importEpub:
			Intent i = new Intent(this.getActivity(), FileBrowser.class);
			this.startActivityForResult(i, Constants.IMPORT_REQUEST);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case Constants.IMPORT_REQUEST:
				String epubFile = data.getStringExtra("filepath");
				importEpub(epubFile);
				break;
			}
		}
	}

	/****************************************************************************************
	 * private functionality methods
	 */

	private void initQuickAction() {
		quickAction = new QuickAction(this.getActivity());
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);

		ActionItem read = new ActionItem();
		read.setTitle(this.getString(R.string.read_item));
		read.setIcon(getResources().getDrawable(R.drawable.ic_menu_read));

		ActionItem delete = new ActionItem();
		delete.setTitle(this.getString(R.string.delete_item));
		delete.setIcon(getResources().getDrawable(R.drawable.ic_menu_delete));

		quickAction.addActionItem(read);
		quickAction.addActionItem(delete);

		quickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(int pos) {
						if (pos == 0) {
							readDoc(longClickedItemPos);
						} else {
							deleteDoc(longClickedItemPos);
						}
						longClickedItemPos = -1;
					}
				});
	}

	private void readDoc(int position) {
		BookLink doc = (BookLink) this.getListAdapter().getItem(position);
		Intent intent = new Intent(this.getActivity(), ReaderView.class);

		EpubDbAdapter dbAdapter = new EpubDbAdapter(this.getActivity()).open();
		String filepath = dbAdapter.getEpubLocation(doc.getId());
		dbAdapter.close();

		intent.putExtra("docSrc", filepath);

		startActivity(intent);

	}

	private void deleteDoc(int position) {
		BookLink doc = (BookLink) this.getListAdapter().getItem(position);

		EpubDbAdapter dbAdapter = new EpubDbAdapter(this.getActivity()).open();
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

	private class ListDocumentsTask extends
			AsyncTask<Void, Void, List<BookLink>> {

		Activity activity = null;

		ListDocumentsTask(Activity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialogHandler.displayDialog(Constants.LIST_DOCUMENTS);
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
			if (result != null && result.size() > 0) {
				docAdapter.notifyDataSetChanged();
				for (int i = 0; i < result.size(); i++)
					docAdapter.add(result.get(i));
			}
			dialogHandler.hideDialog(Constants.LIST_DOCUMENTS);

			docAdapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}

	}

	private class ImportEpubTask extends AsyncTask<String, Integer, BookLink> {

		Activity activity = null;
		boolean completed = false;
		String invalidFile = null;

		ImportEpubTask(Activity activity) {
			this.activity = activity;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialogHandler.displayDialog(Constants.IMPORT_DOCUMENT);
			publishProgress(0);

		}

		@Override
		protected BookLink doInBackground(String... params) {
			String epubFilePath = params[0];
			try {
				FileInputStream epubStream = new FileInputStream(epubFilePath);
				BookLink epubLink = new BookLink();

				Decrypter decrypter = new Decrypter(epubFilePath, activity);
				publishProgress(10);
				Metadata meta = (new EpubReader(decrypter))
						.readEpubMetadata(epubStream);
				publishProgress(30);

				epubLink.setMeta(meta);
				if (meta.getCoverImage() != null)
					epubLink.setCoverUrl(meta.getCoverImage().getHref());

				File sdDir = Environment.getExternalStorageDirectory();
				if (sdDir.exists() && sdDir.canRead()) {
					File docDir = new File(sdDir.getAbsolutePath()
							+ "/drmReader");
					if (!docDir.exists())
						docDir.mkdirs();
					publishProgress(50);
					if (docDir.exists() && docDir.canRead()) {
						String fileName = docDir + "/"
								+ epubLink.getMeta().getFirstTitle() + ".epub";
						publishProgress(70);
						IOUtil.copy(new FileInputStream(epubFilePath),
								new FileOutputStream(fileName));
						publishProgress(90);
						EpubDbAdapter dbAdapter = new EpubDbAdapter(activity)
								.open();
						long epubId = dbAdapter.createEpub(epubLink, fileName,
								meta.getCoverImage().getData());
						dbAdapter.close();
						publishProgress(100);
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
		protected void onProgressUpdate(Integer... progress) {
			dialogHandler.setDialogProgress(progress[0]);
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
				addListItem(result);
			} else {
				Toast.makeText(activity,
						"Something went wrong during the import process",
						Toast.LENGTH_LONG).show();
			}

			dialogHandler.hideDialog(Constants.IMPORT_DOCUMENT);
			super.onPostExecute(result);
		}

	}

	public void importEpub(String epubFile) {
		if (epubFile.endsWith(".epub")) {
			new ImportEpubTask(this.getActivity()).execute(epubFile);
		} else {
			Toast.makeText(
					this.getActivity(),
					"The file " + epubFile
							+ " has not a valid epub file extension",
					Toast.LENGTH_LONG).show();
		}

	}

	public void addListItem(BookLink item) {
		if (docAdapter != null) {
			docAdapter.add((BookLink) item);
			docAdapter.notifyDataSetChanged();
		}
	}

}
