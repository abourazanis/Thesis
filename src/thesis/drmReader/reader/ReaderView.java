package thesis.drmReader.reader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;

import nl.siegmann.epublib.browsersupport.NavigationEvent;
import nl.siegmann.epublib.browsersupport.NavigationEventListener;
import nl.siegmann.epublib.browsersupport.Navigator;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

import org.xml.sax.SAXException;

import thesis.drmReader.R;
import thesis.drmReader.reader.SimpleGestureFilter.SimpleGestureListener;
import thesis.drmReader.ui.NumberPicker;
import thesis.drmReader.ui.NumberPicker.OnChangedListener;
import thesis.drmReader.util.ReaderUtils;
import thesis.sec.Decrypter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ReaderView extends Activity implements SimpleGestureListener,
		OnChangedListener, NavigationEventListener {

	private static final int TOC_MENU = 0;
	private static final int FONT_SIZE_MENU = 1;
	private static final String TAG = "ReaderView";
	private static final int SCREEN_TAP_SIZE = 30;
	private static final int MAX_FONT_SIZE = 20;

	private static final int SCREEN_WIDTH_CORRECTION = 5;
	private static final int SCREEN_HEIGHT_CORRECTION = 2;

	private WebView webView;
	private WebSettings webSettings;
	private SimpleGestureFilter detector;
	private ProgressDialog progressDialog;
	private Decrypter decrypter;

	private int displayWidth;
	private int displayHeight;
	private int columnCount;
	private int columnWidth;
	private String cache;

	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private boolean isPageLoaded = false;

	private Book currentDoc;
	private Navigator navigator;
	private int currentPageIndex = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setUpUI();
		cache = this.getCacheDir().getAbsolutePath() + "/readertmp";

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		String docSrc = extras.getString("docSrc");
		if (docSrc != null && docSrc != "") {
			try {
				FileInputStream epubStream = new FileInputStream(docSrc);

				decrypter = new Decrypter(docSrc, this);

				currentDoc = (new EpubReader(decrypter)).readEpub(epubStream);

				navigator = new Navigator(currentDoc); // here we have as
														// current resource the
														// cover
				navigator.addNavigationEventListener(this);
				readDocumentSpineEntry();
			} catch (InvalidKeyException e) {

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void setUpUI() {
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

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		displayWidth = metrics.widthPixels + SCREEN_WIDTH_CORRECTION;
		displayHeight = metrics.heightPixels + SCREEN_HEIGHT_CORRECTION;

		webView.addJavascriptInterface(new JSInterface(), "interface");
		webView.setWebViewClient(new WebViewClient() {

			public void onPageFinished(WebView view, String url) {

				// Column Count is just the number of 'screens' of text. Add one
				// for partial 'screens'
				// columnCount = (view.getScrollX() / displayWidth) + 1;

				// css3 column module & webkit transition setup
				String js = "var d = document.getElementsByTagName('body')[0];"
						+ "var width = d.scrollWidth;"
						+ "var pageCount = Math.ceil(d.scrollWidth/"
						+ displayWidth + ");"
						+ "window.interface.setColumnCount(pageCount);"
						+ "window.interface.setColumnWidth(screen.width);";
				webView.loadUrl("javascript:(function(){" + js + "})()");
				webView.scrollTo(0, 0);
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("ReaderView",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}

		});

		webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webView.setVerticalScrollBarEnabled(false);
		webView.setHorizontalScrollBarEnabled(false);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_UP) {
			int keyCode = event.getKeyCode();

			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				navigate(currentPageIndex + 1);
				break;

			case KeyEvent.KEYCODE_DPAD_LEFT:
				navigate(currentPageIndex - 1);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				navigate(currentPageIndex + 1);
				break;
			}

		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// setUpUI();
		webView.reload();
		currentPageIndex = 1;
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
			showDialog(FONT_SIZE_MENU);
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
		// TODO: implement new listview.See TableOfContentsPane for how to
		// process TOC
		// case TOC_MENU:
		// List<String> tocTitles = currentDoc.getTOC().getItemTitles();
		// final CharSequence[] items = tocTitles
		// .toArray(new CharSequence[tocTitles.size()]);
		//
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setTitle(R.string.tableOfContents);
		// builder.setSingleChoiceItems(items, -1,
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int item) {
		// currentPageIndex = 1;
		// readDocumentTOCEntry(item);
		// dialog.dismiss();
		// }
		// });
		// AlertDialog alert = builder.create();
		// return alert;
		case FONT_SIZE_MENU:
			Dialog fontDialog = new Dialog(this);

			fontDialog.setContentView(R.layout.dialog_fontsize);
			fontDialog.setTitle(R.string.font_size_msg);
			fontDialog.setCanceledOnTouchOutside(true);
			fontDialog.setCancelable(true);
			NumberPicker picker = (NumberPicker) fontDialog
					.findViewById(R.id.num_picker);

			int fontSize = webSettings.getDefaultFontSize();
			int minFontSize = webSettings.getMinimumFontSize();
			picker.setRange(minFontSize, MAX_FONT_SIZE);
			picker.setCurrent(fontSize);
			picker.setOnChangeListener(this);

			return fontDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
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

		if (sub < SCREEN_TAP_SIZE) {
			navigate(currentPageIndex + 1);
		} else if (sub > (displayWidth - SCREEN_TAP_SIZE)) {
			navigate(currentPageIndex - 1);
		}

	}

	private void navigate(int page) {

		if (page <= 0) {
			currentPageIndex = 1;
			navigator.gotoPreviousSpineSection(this);
			// new GoToPageTask().execute(); TODO: must be called so we can go
			// to the last page
		} else if (page <= columnCount) {
			goToPage(page, true);
		} else {
			navigator.gotoNextSpineSection(this);
		}

	}

	private void readDocumentSpineEntry() {

		new LoadDocTask(this).execute();
		progressDialog = ProgressDialog.show(ReaderView.this, "Please wait...",
				"Parsing..", true);

	}

	private void goToPage(int pageIndex, boolean withCSS3) {

		int moveWidth = (int) (pageIndex - 1) * columnWidth;
		
		Log.d(TAG, "width: " + moveWidth);
		String js = "";
		if (withCSS3) {
			js = "var d = document.getElementById('wrapper');"
					+ "d.style['-webkit-transform'] = 'translateX(-"
					+ moveWidth + "px)';";
		} else {
			js = "var d = document.getElementById('wrap');"
					+ "window.scrollTo( " + moveWidth + ", 0) ;";
		}

		webView.loadUrl("javascript:(function(){" + js + "})()");
		currentPageIndex = pageIndex;
	}

	// for NumberPicker
	@Override
	public void onChanged(NumberPicker picker, int oldVal, int newVal) {
		webSettings.setDefaultFontSize(newVal);
		webView.invalidate();
		webView.reload();
		navigate(currentPageIndex);
	}

	private class GoToPageTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			lock.lock();
			try {
				while (isPageLoaded == false) {
					condition.await();
				}

				isPageLoaded = true;
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			} finally {
				lock.unlock();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			goToPage(columnCount, false);
			super.onPostExecute(result);
		}
	}

	private class LoadDocTask extends AsyncTask<Void, Void, String> {

		ReaderView activity = null;

		LoadDocTask(ReaderView activity) {
			this.activity = activity;
		}

		@Override
		protected String doInBackground(Void... params) {

			String htmlContent = "";
			try {
				Resource resource = activity.decrypter
						.decrypt(activity.navigator.getCurrentResource());
				htmlContent = ReaderUtils.getModifiedDocument(
						activity.navigator.getBook(), resource, displayWidth,
						displayHeight, cache);

			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			} catch (InvalidKeyException e) {
				Log.e(TAG, e.getMessage());
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return htmlContent;
		}

		@Override
		protected void onPostExecute(String result) {

			isPageLoaded = false;

			webView.clearHistory();
			webView.clearFormData();
			webView.clearCache(true);
			webView.loadDataWithBaseURL(null, result, null, "utf-8", null);
			// problem: http://code.google.com/p/android/issues/detail?id=1733
			// webView.loadData(result, "text/html", "utf-8");

			progressDialog.dismiss();

			super.onPostExecute(result);
		}

		void detach() {
			activity = null;
		}

		void attach(ReaderView activity) {
			this.activity = activity;
		}

	}

	final class JSInterface {
		JSInterface() {
		}

		public void setColumnCount(int count) {
			columnCount = count;
			isPageLoaded = true;

			// lock.lock();
			//
			// try {
			// columnCount = count;
			// isPageLoaded = true;
			// condition.signalAll();
			// } finally {
			// lock.unlock();
			// }

		}

		public void setColumnWidth(int width) {
			columnWidth = width;
		}

	}

	@Override
	public void navigationPerformed(NavigationEvent navigationEvent) {
		if (navigationEvent.isBookChanged()) {
			// initBook(navigationEvent.getCurrentBook());
		} else {
			if (navigationEvent.isResourceChanged()) {
				readDocumentSpineEntry();
				currentPageIndex = 1;
			} else if (navigationEvent.isSectionPosChanged()) {
				// editorPane.setCaretPosition(navigationEvent.getCurrentSectionPos());
			}
			// if
			// (StringUtils.isNotBlank(navigationEvent.getCurrentFragmentId()))
			// {
			// scrollToNamedAnchor(navigationEvent.getCurrentFragmentId());
			// }
		}

	}
}
