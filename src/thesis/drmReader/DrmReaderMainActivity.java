package thesis.drmReader;

import thesis.drmReader.ui.ArchiveListFragment;
import thesis.drmReader.ui.DialogHandler;
import thesis.drmReader.ui.WebListFragment;
import thesis.drmReader.ui.WebListFragment.ImportListener;
import thesis.drmReader.util.Constants;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.ActionBar.TabListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class DrmReaderMainActivity extends FragmentActivity implements
		DialogHandler, ImportListener {

	private boolean useLogo = false;
	private boolean showHomeUp = false;

	private static boolean isDialogShowing = false;
	public static int dialogId = 999;

	private ProgressDialog progressDialog = null;
	ArchiveListFragment archiveFragment;
	WebListFragment webFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.drmreader_main);		

		// setup Action Bar for tabs
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// remove the activity title to make space for tabs
		actionBar.setDisplayShowTitleEnabled(false);

		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(showHomeUp);
		actionBar.setDisplayUseLogoEnabled(useLogo);
		actionBar.setDisplayShowTitleEnabled(true);

			
			
			// add a new tab and set its title text and tab listener
			actionBar.addTab(actionBar.newTab()
					.setIcon(R.drawable.ic_tab_archive).setText("Archive")
					.setTabListener(new MyTabListener<ArchiveListFragment>(
	                        this, "archive", ArchiveListFragment.class)));

			// add a new tab and set its title text and tab listener
			actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_tab_web).setText("Web")
					.setTabListener(new MyTabListener<WebListFragment>(
	                        this, "web", WebListFragment.class)));
			
			if (savedInstanceState != null) {
				Log.d("tag","savedInstanceState is not null");
	        	getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("index"));
	        }

	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case Constants.LIST_DOCUMENTS: {
			((ProgressDialog) dialog).setMessage("Populating list..");
		}
			break;
		case Constants.IMPORT_DOCUMENT: {
			((ProgressDialog) dialog).setMessage("Importing epub in library..");
			((ProgressDialog) dialog)
					.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			((ProgressDialog) dialog).setMax(100);
		}
			break;
		case Constants.DOWNLOAD_DOCUMENT: {
			((ProgressDialog) dialog).setMessage("Downloading document..");
		}
			break;
		case Constants.DOWNLOAD_DOCLIST: {
			((ProgressDialog) dialog).setMessage("Downloading document list..");
		}
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case Constants.LIST_DOCUMENTS:
			isDialogShowing = true;
			dialogId = Constants.LIST_DOCUMENTS;
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.IMPORT_DOCUMENT:
			isDialogShowing = true;
			dialogId = Constants.IMPORT_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.DOWNLOAD_DOCUMENT:
			isDialogShowing = true;
			dialogId = Constants.DOWNLOAD_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.DOWNLOAD_DOCLIST:
			isDialogShowing = true;
			dialogId = Constants.DOWNLOAD_DOCLIST;
			progressDialog = new ProgressDialog(this);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.DOWNLOAD_DOCLIST_ALERT:
			dialogId = Constants.DOWNLOAD_DOCLIST_ALERT;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"An network error occurred during the document list download process.Please try again or go back.")
					.setCancelable(false)
					// .setTitle("Attention")
					// .setIcon(R.drawable.ic_stat_alert)
					.setPositiveButton("Retry",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									webFragment.refreshList();
									removeDialog(Constants.DOWNLOAD_DOCLIST_ALERT);
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									removeDialog(Constants.DOWNLOAD_DOCLIST_ALERT);
								}
							});
			AlertDialog dialog = builder.create();
			dialog.setOnCancelListener(new OnCancelListener() {
				// Called when the Back button is pressed
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(Constants.DOWNLOAD_DOCLIST_ALERT);
				}
			});
			return dialog;
		case Constants.DOWNLOAD_DOCUMENT_ALERT:
			dialogId = Constants.DOWNLOAD_DOCUMENT_ALERT;
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setMessage(
					"An network error occurred during the document download process.Please try again or go back.")
					.setCancelable(false)
					// .setTitle("Attention")
					.setIcon(R.drawable.ic_stat_alert)
					.setPositiveButton("Retry",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									webFragment.downloadDocument();
									removeDialog(Constants.DOWNLOAD_DOCUMENT_ALERT);
								}
							})
					.setNegativeButton("Back",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									removeDialog(Constants.DOWNLOAD_DOCUMENT_ALERT);
								}
							});
			AlertDialog dialog2 = builder2.create();
			dialog2.setOnCancelListener(new OnCancelListener() {
				// Called when the Back button is pressed
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(Constants.DOWNLOAD_DOCUMENT_ALERT);
				}
			});
			return dialog2;
		default:
			return null;
		}
	}
	
	public static class MyTabListener<T extends Fragment> implements TabListener {
        private final FragmentActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public MyTabListener(FragmentActivity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public MyTabListener(FragmentActivity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
            	Log.d("tag","fragment is null :  " + tab.getText());
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                mActivity.getSupportFragmentManager().beginTransaction().add(R.id.reader_main, mFragment, mTag).commit();
            } else {
            	if(mFragment.isDetached()){
            		Log.d("tag","fragment is detached :  " + tab.getText());
            		mActivity.getSupportFragmentManager().beginTransaction().attach(mFragment).commit();
            	}
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
            	mActivity.getSupportFragmentManager().beginTransaction().detach(mFragment).commit();
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

	
	
	

	/************** DialogHandler methods *******************/

	@Override
	public void setDialogProgress(int progress) {
		if (progressDialog != null && isDialogShowing)
			progressDialog.setProgress(progress);
	}

	@Override
	public void displayDialog(int dialogID) {
		dialogId = dialogID;
		showDialog(dialogID);
	}

	@Override
	public void hideDialog(int dialogID) {
		if (dialogId == dialogID && isDialogShowing) {
			dialogId = -1;// dummy value
			removeDialog(dialogID);
		}

	}

	@Override
	public void importEpub(String file) {
		if (archiveFragment != null)
			archiveFragment.importEpub(file);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onPause()
	 */
	@Override
	protected void onPause() {
		Log.d("DrmMainReaderActivity","onPause");
		// TODO Auto-generated method stub
		super.onPause();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.d("DrmMainReaderActivity","onResume");
		super.onResume();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	@Override
	protected void onStart() {
		Log.d("DrmMainReaderActivity","onStart");
		// TODO Auto-generated method stub
		super.onStart();
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	@Override
	protected void onStop() {
		Log.d("DrmMainReaderActivity","onStop");
		// TODO Auto-generated method stub
		super.onStop();
	}

}
