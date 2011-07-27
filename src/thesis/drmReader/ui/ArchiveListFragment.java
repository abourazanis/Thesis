package thesis.drmReader.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.IOUtil;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubDbAdapter;
import thesis.drmReader.filebrowser.FileBrowser;
import thesis.drmReader.reader.ReaderView;
import thesis.drmReader.ui.QuickAction.ActionItem;
import thesis.drmReader.ui.QuickAction.QuickAction;
import thesis.drmReader.util.Constants;
import thesis.drmReader.util.EpubLoader;
import thesis.drmReader.util.IncrementalAsyncTaskLoaderCallbacks;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class ArchiveListFragment extends ListFragment implements
		IncrementalAsyncTaskLoaderCallbacks<Integer, List<BookLink>> {

	private final static String TAG = ArchiveListFragment.class
			.getCanonicalName();
	private ArrayList<BookLink> docList;
	private BookLinkAdapter docAdapter;
	private QuickAction quickAction;
	private int longClickedItemPos = -1;

	private boolean firstRun = true;
	private boolean firstImport = true;
	EpubDbAdapter dbAdapter;
	Bundle args;
	private final Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		setRetainInstance(true);// save the state across screen configuration
								// changes

		if (firstRun) {
			docList = new ArrayList<BookLink>();
			docAdapter = new BookLinkAdapter(this.getActivity(),
					R.layout.list_item, docList);
			setListAdapter(docAdapter);
			dbAdapter = new EpubDbAdapter(this.getActivity()
					.getApplicationContext());
		}

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

		// call this to re-connect with an existing
		// loader (after screen configuration changes for e.g!)
		LoaderManager lm = getLoaderManager();
		if (lm.getLoader(Constants.LIST_DOCUMENTS) != null) {
			Log.d(TAG,"list document loader founded");
			lm.initLoader(Constants.LIST_DOCUMENTS, null, this);
		}

		if (lm.getLoader(Constants.IMPORT_DOCUMENT) != null) {
			Log.d(TAG,"import document loader founded");
			lm.initLoader(Constants.IMPORT_DOCUMENT, args, this);
		}

		if (firstRun) {
			firstRun = false;
			getLoaderManager().initLoader(Constants.LIST_DOCUMENTS, null, this);
			showDialog(Constants.LIST_DOCUMENTS);
		}
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
				Log.d(TAG,"came back from filebrowser - execute importEpub");
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

	public void importEpub(String epubFile) {
		if (epubFile.endsWith(".epub")) {
			args = new Bundle();
			args.putString("file", epubFile);
				this.getLoaderManager().restartLoader(
						Constants.IMPORT_DOCUMENT, args, this);
			showDialog(Constants.IMPORT_DOCUMENT);
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

	protected void restartLoading() {
		showDialog(Constants.LIST_DOCUMENTS);
		docAdapter.clear();
		docAdapter.notifyDataSetChanged();
		getListView().invalidateViews();

		// --------- the other magic lines ----------
		// call restart because we want the background work to be executed
		// again
		Log.d(TAG, "restartLoading(): re-starting loader");
		getLoaderManager().restartLoader(Constants.LIST_DOCUMENTS, null, this);
		// --------- end the other magic lines --------
	}

	// methods for loader callbacks
	@Override
	public Loader<List<BookLink>> onCreateLoader(int id, Bundle args) {
		EpubLoader loader = null;
		switch (id) {
		case Constants.IMPORT_DOCUMENT: {
			Log.d(TAG,"onCreateLoader import document");
			loader = new EpubLoader(getActivity(), this, args.getString("file")) {
				@Override
				public List<BookLink> loadInBackground() {
					Log.d(TAG,"loadInBackground import document loader");
					String epubFilePath = file;
					try {
						FileInputStream epubStream = new FileInputStream(
								epubFilePath);
						BookLink epubLink = new BookLink();

						Decrypter decrypter = new Decrypter(epubFilePath,
								this.getContext());
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
										+ epubLink.getMeta().getFirstTitle()
										+ ".epub";
								publishProgress(70);
								IOUtil.copy(new FileInputStream(epubFilePath),
										new FileOutputStream(fileName));
								publishProgress(90);
								EpubDbAdapter dbAdapter = new EpubDbAdapter(
										this.getContext()).open();
								long epubId = dbAdapter.createEpub(epubLink,
										fileName, meta.getCoverImage()
												.getData());
								dbAdapter.close();
								publishProgress(100);
								epubLink.setId(String.valueOf(epubId));
								ArrayList<BookLink> res = new ArrayList<BookLink>();
								res.add(epubLink);
								return res;
							}
						}
					} catch (InvalidKeyException e) {
						// invalid file
					} catch (FileNotFoundException e) {
						Log.e(TAG, e.getMessage());
					} catch (IOException e) {
						Log.e(TAG, e.getMessage());
					}
					
					return null;
				}
			};

		}
			break;
		case Constants.LIST_DOCUMENTS: {
			loader = new EpubLoader(getActivity(), this, args == null ? null
					: args.getString("file")) {
				@Override
				public List<BookLink> loadInBackground() {
					dbAdapter.open();
					List<BookLink> list = dbAdapter.getEpubs();
					dbAdapter.close();
					
					return list;
				}
			};
		}
			break;
		}

		// somehow the AsyncTaskLoader doesn't want to start its job without
		// calling this method
		loader.forceLoad();

		return loader;
	}

	@Override
	public void onLoaderReset(Loader<List<BookLink>> loader) {
	}

	@Override
	public void onLoadFinished(Loader<List<BookLink>> loader,
			List<BookLink> docs) {
		Log.d(TAG,"onLoadFinished");
		switch (loader.getId()) {
		case Constants.LIST_DOCUMENTS: {
			if (docs != null && docs.size() > 0) {
				docList.clear();
				Iterator<BookLink> it = docs.iterator();
				while (it.hasNext()) {
					docList.add(it.next());
				}
				docAdapter.notifyDataSetChanged();
			}
			Log.d(TAG,"hide dialog " + loader.getId());
			hideDialog();
		}
			break;
		case Constants.IMPORT_DOCUMENT: {
			Log.d(TAG,"importEpub loader onLoadFinished");
			Log.d(TAG,"hide dialog " + loader.getId());
			hideDialog();
			// XXX: make it to add all list items
			if (docs != null && docs.size() > 0) {
				addListItem(docs.get(0));
				Log.d(TAG,"importEpub loader onLoadFinished item Added");
			} else {
				Toast.makeText(loader.getContext(),
						"Something went wrong during the import process",
						Toast.LENGTH_LONG).show();
			}
		}
			break;
		}

	}

	@Override
	public void onPreExecute() {

	}

	@Override
	public void onProgressUpdate(Integer progress) {
		Log.d(TAG,"importEpub onProgressUpdate");
		setProgress(progress);
	}

	@Override
	public void onPostExecute(List<BookLink> result) {

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
	            ProgressDialog dialog = null;
	            switch(id){
	            case Constants.LIST_DOCUMENTS:
	            	dialog = new ProgressDialog(this.getActivity());
					dialog.setMessage("Populating list..");
					dialog.setCancelable(false);
					break;
	            case Constants.IMPORT_DOCUMENT:
	            	dialog = new ProgressDialog(this.getActivity());
					dialog.setMessage("Importing epub in library..");
					dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					dialog.setCancelable(false);
					dialog.setMax(100);
					break;
	            }
	            return dialog;
		 }
		 
		 public void setProgress(int progress) {
			 ((ProgressDialog)this.getDialog()).setProgress(progress);
			}
	}
	
	
	
	
}
