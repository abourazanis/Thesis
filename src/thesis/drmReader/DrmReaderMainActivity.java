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
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.ActionBar.TabListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class DrmReaderMainActivity extends FragmentActivity implements
		DialogHandler,ImportListener  {//TODO : change the different interface to one generic message passing interface

	private boolean useLogo = false;
	private boolean showHomeUp = false;

	private boolean isDialogShowing = false;
	private int dialogId = 999;
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

		// instantiate fragment for the tab
		archiveFragment = new ArchiveListFragment();
		// add a new tab and set its title text and tab listener
		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_tab_archive)//.setText("Archive")
				.setTabListener(new MyTabsListener(archiveFragment)));

		// instantiate fragment for the tab
		webFragment = new WebListFragment();
		// add a new tab and set its title text and tab listener
		actionBar.addTab(actionBar.newTab().setIcon(R.drawable.ic_tab_web)//.setText("Web")
				.setTabListener(new MyTabsListener(webFragment)));

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case Constants.LIST_DOCUMENTS:
			isDialogShowing = true;
			dialogId = Constants.LIST_DOCUMENTS;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Populating list..");
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.IMPORT_DOCUMENT:
			isDialogShowing = true;
			dialogId = Constants.IMPORT_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Importing epub in library..");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.setMax(100);
			progressDialog.show();
			return progressDialog;
		case Constants.DOWNLOAD_DOCUMENT:
			isDialogShowing = true;
			dialogId = Constants.DOWNLOAD_DOCUMENT;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document..");
			// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		case Constants.DOWNLOAD_DOCLIST:
			isDialogShowing = true;
			dialogId = Constants.DOWNLOAD_DOCLIST;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("Downloading document list..");
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
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			return builder.create();
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
								}
							})
					.setNegativeButton("Back",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			return builder2.create();
		default:
			return null;
		}
	}


	protected class MyTabsListener implements TabListener {

		private Fragment mFragment;

		public MyTabsListener(Fragment fragment) {
			this.mFragment = fragment;
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub

		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// in official documentation the ft is not NULL
			ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.reader_main, mFragment, null);
			// official documentation says that we must not call commit :
			// http://developer.android.com/guide/topics/ui/actionbar.html
			ft.commit();

		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// in official documentation the ft is not NULL
			ft = getSupportFragmentManager().beginTransaction();
			ft.remove(mFragment);
			// official documentation says that we must not call commit :
			// http://developer.android.com/guide/topics/ui/actionbar.html
			ft.commit();
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
		if (dialogId == dialogID && isDialogShowing){
			dialogId = -1;//dummy value
			progressDialog.dismiss();
		}

	}

	@Override
	public void importEpub(String file) {
		if(archiveFragment != null)
			archiveFragment.importEpub(file);
	}

}
