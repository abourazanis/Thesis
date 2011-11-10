package thesis.drmReader.filebrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import nl.siegmann.epublib.util.StringUtil;

import thesis.drmReader.R;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FileBrowser extends ListActivity {

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		savedInstanceState.putString("currentDir",
				this.currentDirectory.getAbsolutePath());
		
		ArrayList<String> filesToImport = new ArrayList<String>();
		Iterator<IconifiedText> it = itla.getSelectedItems().iterator();
		while (it.hasNext()) {
			filesToImport.add(it.next().getText());
		}
		
		savedInstanceState.putStringArray("selectedFiles", filesToImport.toArray(new String[filesToImport.size()]));
		super.onSaveInstanceState(savedInstanceState);
	}

	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
	private ArrayList<IconifiedText> directoryEntries = new ArrayList<IconifiedText>();
	private File currentDirectory = new File("/");

	private TextView browserTitle;
	private IconifiedTextListAdapter itla;
	private RelativeLayout browserActionButtons;
	private Button btnImport;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.filebrowser);
		browserTitle = (TextView) findViewById(R.id.browser_title_text);
		View up = (View) findViewById(R.id.browser_up);
		browserActionButtons = (RelativeLayout) findViewById(R.id.browser_actionbuttons);
		final View cancelButton = findViewById(R.id.overlay_clear);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onClearSelections();
				hideOverlay(browserActionButtons);
			}
		});

		btnImport = (Button) findViewById(R.id.overlay_ok);
		btnImport.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openFiles();
				hideOverlay(browserActionButtons);
			}
		});

		up.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				upOneLevel();
			}
		});

		if (savedInstanceState != null) {
			String currentDirPath = savedInstanceState.getString("currentDir");
			if (StringUtil.isNotBlank(currentDirPath)) {
				this.browseTo(new File(currentDirPath));
			} else
				browseToSDCard();
			itla.setSelectedItems(savedInstanceState.getStringArray("selectedFiles"));
			showButtonActions();
		} else {
			browseToSDCard();
		}
		browserTitle.setText(currentDirectory.isDirectory() ? currentDirectory
				.getName() : currentDirectory.getParentFile().getName());
		
	}

	private void browseToSDCard() {
		File sdDir = Environment.getExternalStorageDirectory();
		if (sdDir.exists() && sdDir.canRead())
			browseTo(new File(sdDir.getAbsolutePath()));
		else
			browseTo(new File("/"));
	}

	/**
	 * This function browses up one level according to the field:
	 * currentDirectory
	 */
	private void upOneLevel() {
		if (this.currentDirectory.getParent() != null)
			this.browseTo(this.currentDirectory.getParentFile());
	}

	private void browseTo(final File entry) {
		if (entry.isDirectory()) {
			this.currentDirectory = entry;
			browserTitle.setText(entry.getName());
			fill(entry.listFiles());
		} 
	}

	private void openFiles() {
		ArrayList<String> filesToImport = new ArrayList<String>();
		Iterator<IconifiedText> it = itla.getSelectedItems().iterator();
		while (it.hasNext()) {
			String filePath = this.currentDirectory.getAbsolutePath() + "/"
					+ it.next().getText();
			filesToImport.add(filePath);
		}
		Intent resultIntent = new Intent();
		resultIntent.putExtra("filepaths",
				filesToImport.toArray(new String[filesToImport.size()]));
		this.setResult(RESULT_OK, resultIntent);
		this.finish();
	}

	private void fill(File[] files) {
		if (files == null)
			files = new File[] {};
		this.directoryEntries.clear();

		Drawable currentIcon = null;
		boolean isSelectable = true;
		for (File currentFile : files) {
			if (currentFile.isDirectory()) {
				currentIcon = getResources().getDrawable(R.drawable.folder);
				isSelectable = false;
			} else {
				String fileName = currentFile.getName();
				/*
				 * Determine the Icon to be used, depending on the FileEndings
				 * defined in: res/values/fileendings.xml.
				 */
				if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingImage))) {
					currentIcon = getResources().getDrawable(R.drawable.image);
				} else if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingWebText))) {
					currentIcon = getResources()
							.getDrawable(R.drawable.webtext);
				} else if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingPackage))) {
					currentIcon = getResources().getDrawable(R.drawable.packed);
				} else if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingAudio))) {
					currentIcon = getResources().getDrawable(R.drawable.audio);
				} else if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingEpub))) {
					currentIcon = getResources().getDrawable(R.drawable.epub);
					isSelectable = true;
				} else {
					currentIcon = getResources().getDrawable(R.drawable.text);
				}
			}

			switch (this.displayMode) {
			case ABSOLUTE:
				/* On absolute Mode, we show the full path */
				this.directoryEntries.add(new IconifiedText(currentFile
						.getPath(), currentIcon, isSelectable));

				break;
			case RELATIVE:
				/*
				 * On relative Mode, we have to cut the current-path at the
				 * beginning
				 */
				int currentPathStringLenght = this.currentDirectory
						.getAbsolutePath().length();
				this.directoryEntries.add(new IconifiedText(currentFile
						.getAbsolutePath().substring(
								currentPathStringLenght + 1), currentIcon,
						isSelectable));

				break;
			}
		}
		Collections.sort(this.directoryEntries);

		itla = new IconifiedTextListAdapter(this, R.layout.browser_filerow,
				this.directoryEntries);
		this.setListAdapter(itla);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		File clickedFile = null;
		switch (this.displayMode) {
		case RELATIVE:
			clickedFile = new File(this.currentDirectory.getAbsolutePath()
					+ "/" + this.directoryEntries.get(position).getText());
			break;
		case ABSOLUTE:
			clickedFile = new File(this.directoryEntries.get(position)
					.getText());
			break;
		}

		if (clickedFile.isDirectory()) {
			hideOverlay(browserActionButtons);
			if (clickedFile != null)
				this.browseTo(clickedFile);
		}
	}

	/**
	 * Checks whether checkItsEnd ends with one of the Strings from fileEndings
	 */
	private boolean checkEndsWithInStringArray(String checkItsEnd,
			String[] fileEndings) {
		for (String aEnd : fileEndings) {
			if (checkItsEnd.endsWith(aEnd))
				return true;
		}
		return false;
	}

	private void showOverlay(View overlay) {
		if (overlay != null) {
			overlay.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fadein));
			overlay.setVisibility(View.VISIBLE);
		}
	}

	private void hideOverlay(View overlay) {
		if (overlay != null) {
			overlay.startAnimation(AnimationUtils.loadAnimation(this,
					R.anim.fadeout));
			overlay.setVisibility(View.GONE);
		}
	}

	private void onClearSelections() {
		itla.clearSelections();
	}

	public void showButtonActions() {
		int size = itla.getSelectedItems().size();
		if (size > 0) {
			btnImport.setText(getString(R.string.browser_action_import) + " ("
					+ size + ")");
			if (browserActionButtons.getVisibility() != View.VISIBLE)
				showOverlay(browserActionButtons);
		} else {
			hideOverlay(browserActionButtons);
		}

	}
}
