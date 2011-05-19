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
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ReaderView extends Activity implements SimpleGestureListener {

	private static final int TOC_MENU = 0;
	private static final String TAG = "ReaderView";
	private static final int SCREEN_TAP_SIZE = 30;

	private WebView webView;
	private SimpleGestureFilter detector;

	private int displayWidth;
	private int displayHeight;
	private int columnCount;

	private Document currentDoc;
	private int currentPageIndex = 0;
	private int currentTOCIndex = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		webView = new WebView(this) {
			@Override
			public boolean onTouchEvent(MotionEvent ev) {
				return false;
			}

			@Override
			public boolean dispatchTouchEvent(MotionEvent ev) {
				return false;
			}

		};
		setContentView(webView);
		detector = new SimpleGestureFilter(this, this);

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		webView.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {

				// Column Count is just the number of 'screens' of text. Add one
				// for partial 'screens'
				columnCount = (view.getContentHeight() / view.getHeight()) + 1;
				Log.e(TAG, "WwebView columns:" + columnCount
						+ " ContentHeight:" + view.getContentHeight()
						+ " Height:" + view.getHeight());

				// css3 column module
				String js = "var d = document.getElementsByTagName('body')[0];"
						+ "d.style.WebkitColumnCount="
						+ columnCount + ";"
						+ "d.style.height='" + displayHeight + "px';"

						+ "d.style['-webkit-transition-property'] = '-webkit-transform';"
						+ "d.style['-webkit-transform-origin'] = \"0 0\";"
						+ "d.style['-webkit-transition-duration'] = '550ms';"
						+ "d.style['-webkit-transition-timing-function'] = 'ease-in-out';"

						+ "console.log(\"" + columnCount + "\");"
						+ "d.style.WebkitColumnWidth='" + displayWidth + "px';";
				webView.loadUrl("javascript:(function(){" + js + "})()");
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}
		});

		webView.getSettings().setJavaScriptEnabled(true);
		webView.setVerticalScrollBarEnabled(false);
		webView.setHorizontalScrollBarEnabled(false);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		// TODO: new thread
		String docSrc = extras.getString("docSrc");
		if (docSrc != null && docSrc != "") {
			PedReader reader = new PedReader();
			try {
				ZipInputStream ped = new ZipInputStream(new FileInputStream(
						docSrc));
				currentDoc = reader.readPed(ped, "UTF-8");
				currentPageIndex = 1;
				readDocumentTOCEntry(0);

			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
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
							readDocumentTOCEntry(item);
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
			navigate(currentPageIndex - 1);
			break;
		case SimpleGestureFilter.SWIPE_LEFT:
			navigate(currentPageIndex + 1);
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
			break;
		case SimpleGestureFilter.SWIPE_UP:
			break;

		}

	}

	@Override
	public void onSingleTapConfirmed(MotionEvent e) {
		float x = e.getX();
		float sub = displayWidth - x;
		
		if(sub < SCREEN_TAP_SIZE){
			navigate(currentPageIndex + 1);
		}
		else if(sub > (displayWidth - SCREEN_TAP_SIZE)){
			navigate(currentPageIndex - 1);
		}
			
		

	}

	private void navigate(int page) {
		int tocSize = currentDoc.getTOC().size();
		if (page <= 0) {
			currentPageIndex = 1;
			currentTOCIndex--;
			if (tocSize >= currentTOCIndex && currentTOCIndex >= 0) {
				readDocumentTOCEntry(currentTOCIndex);
				Log.e(TAG,"CoumnCount in navigate:" + columnCount);
				//not working because columnCount has old value at this point (onPageFinished has not been raised)
				//TODO: fix it
				//currentPageIndex = columnCount;
				//goToPage(columnCount);
			} else
				currentTOCIndex = 0;
		} else if (page <= columnCount) {
			goToPage(page);
		} else {
			currentTOCIndex++;
			if (tocSize >= currentTOCIndex && currentTOCIndex >= 0) {
				currentPageIndex = 1;
				readDocumentTOCEntry(currentTOCIndex);
			} else
				currentTOCIndex = tocSize;

		}
	}

	private void readDocumentTOCEntry(int entryIndex) {

		try {
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
			webView.clearHistory();
			webView.clearFormData();
			webView.clearCache(true);
			webView.loadData(b.toString(), "text/html", "utf-8");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

	}

	private void goToPage(int pageIndex) {

		int moveWidth = (pageIndex - 1) * (displayWidth + 75);
		Log.e(TAG, "Width:" + moveWidth + " PageIndex:" + pageIndex);
		String js = "var d = document.getElementsByTagName('body')[0];"
				+ "d.style['-webkit-transform'] = 'translate3d(-" 
				+ moveWidth
				+ "px,0px,0px)';";
		
		webView.loadUrl("javascript:(function(){" + js + "})()");
		currentPageIndex = pageIndex;

		// webView.scrollBy(moveWidth, 0);
		// webView.loadUrl("javascript:(function(){window.scrollTo(" +
		// (pageIndex*displayWidth) +"+window.scrollX,window.scrollY);})()");

	}
}
