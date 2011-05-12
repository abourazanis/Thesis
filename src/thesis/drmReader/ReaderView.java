package thesis.drmReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipInputStream;

import thesis.drmReader.SimpleGestureFilter.SimpleGestureListener;
import thesis.pedlib.ped.Document;
import thesis.pedlib.ped.PedReader;
import thesis.pedlib.ped.TOCEntry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ViewSwitcher;

public class ReaderView extends Activity implements SimpleGestureListener {

	private static final int TOC_MENU = 0;

	//private ArrayList<WebView> pageViews;
	private WebView viewA;
	private WebView viewB;
	private ViewSwitcher switcher;
	private SimpleGestureFilter detector;
	private Animation inFromRight;
	private Animation inFromLeft;
	private Animation outToRight;
	private Animation outToLeft;

	private Document currentDoc;
	private int currentPageIndex = 0;
	private int currentViewIndex = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.reader_view);

		//pageViews.add(new WebView(this));
		//pageViews.add(new WebView(this));
		viewA = new WebView(this);
		viewB = new WebView(this);

		// switcher = (ViewSwitcher) findViewById(R.id.pages_switcher);
		switcher = new ViewSwitcher(this);
		//switcher.addView(pageViews.get(0));
		//switcher.addView(pageViews.get(1));
		switcher.addView(viewA);
		switcher.addView(viewB);
		setContentView(switcher);
		inFromRight = AnimationUtils.loadAnimation(this, R.anim.in_from_right);
		inFromLeft = AnimationUtils.loadAnimation(this, R.anim.in_from_left);
		outToRight = AnimationUtils.loadAnimation(this, R.anim.out_to_right);
		outToLeft = AnimationUtils.loadAnimation(this, R.anim.out_to_left);

		detector = new SimpleGestureFilter(this, this);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		String docSrc = extras.getString("docSrc");
		if (docSrc != null && docSrc != "") {
			PedReader reader = new PedReader();
			try {
				ZipInputStream ped = new ZipInputStream(new FileInputStream(
						docSrc));
				currentDoc = reader.readPed(ped, "UTF-8");
				goToPage(0);

			} catch (Exception e) {
				Log.e("FUCK", e.getMessage());
			}

		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.read_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.toc:
			showDialog(TOC_MENU);
			return true;
		case R.id.font_size:
			showFontSizeMenu();
			return true;
		case R.id.home:
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case TOC_MENU:
			List<String> tocTitles = currentDoc.getTOC().getItemTitles();
			final CharSequence[] items = tocTitles
					.toArray(new CharSequence[tocTitles.size()]);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(R.string.tableOfContents);
			builder.setSingleChoiceItems(items, -1,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							goToPage(item);
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}

	private void showFontSizeMenu() {

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	@Override
	public void onSwipe(int direction) {

		switch (direction) {

		case SimpleGestureFilter.SWIPE_RIGHT:
			goToPage(currentPageIndex - 1);
			switcher.setInAnimation(inFromRight);
			switcher.setOutAnimation(outToLeft);
			switcher.showNext();
			break;
		case SimpleGestureFilter.SWIPE_LEFT:
			goToPage(currentPageIndex + 1);
			switcher.setInAnimation(inFromLeft);
			switcher.setOutAnimation(outToRight);
			switcher.showNext();
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
			break;
		case SimpleGestureFilter.SWIPE_UP:
			break;

		}
	}

	@Override
	public void onDoubleTap() {

	}

	private void readDocumentTOCEntry(int entryIndex, WebView view)
			throws IOException {

		currentPageIndex = entryIndex;
		List<TOCEntry> toc = currentDoc.getTOC().getItems();
		InputStream in = currentDoc.getResources()
				.getByHref(toc.get(entryIndex).getSrc()).getInputStream();

		final BufferedReader breader = new BufferedReader(
				new InputStreamReader(in));
		StringBuilder b = new StringBuilder();
		String line;

		while ((line = breader.readLine()) != null) {
			b.append(line);
		}
		breader.close();
		view.loadData(b.toString(), "text/html", "utf-8");

	}

	private void goToPage(int pageIndex) {
		currentPageIndex = pageIndex;
		try {
		switch (currentViewIndex) {
		case 0:
			currentViewIndex = 1;
			readDocumentTOCEntry(pageIndex, viewB);
			break;
		case 1:
			currentViewIndex = 0;
			readDocumentTOCEntry(pageIndex, viewA);
			break;
		default:
			currentViewIndex = 0;
		}
		
			//readDocumentTOCEntry(pageIndex, pageViews.get(currentViewIndex));
		} catch (IOException e) {
			Log.e("gotopage", e.getMessage());
		}
	}
}
