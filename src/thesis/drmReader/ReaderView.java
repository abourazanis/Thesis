package thesis.drmReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipInputStream;

import thesis.drmReader.NumberPicker.OnChangedListener;
import thesis.drmReader.SimpleGestureFilter.SimpleGestureListener;
import thesis.pedlib.ped.Document;
import thesis.pedlib.ped.PedReader;
import thesis.pedlib.ped.Resource;
import thesis.pedlib.ped.TOCEntry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
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
		OnChangedListener {

	private static final int TOC_MENU = 0;
	private static final int FONT_SIZE_MENU = 1;
	private static final String TAG = "ReaderView";
	private static final int SCREEN_TAP_SIZE = 30;
	private static final int SCREEN_PADDING = 0;
	private static final int MAX_FONT_SIZE = 20;

	private WebView webView;
	private WebSettings webSettings;
	private SimpleGestureFilter detector;
	private ProgressDialog progressDialog;

	private int displayWidth;
	private int displayHeight;
	private int columnWidth;
	private int columnCount;

	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private boolean isPageLoaded = false;

	private Document currentDoc;
	private int currentPageIndex = 0;
	private int currentTOCIndex = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setUpUI();

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		String docSrc = extras.getString("docSrc");
		if (docSrc != null && docSrc != "") {
			PedReader reader = new PedReader(this);
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

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		webView.addJavascriptInterface(new JSInterface(), "interface");
		webView.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {

				// Column Count is just the number of 'screens' of text. Add one
				// for partial 'screens'
				columnCount = (view.getContentHeight() / view.getHeight()) + 1;

				// css3 column module & webkit transition setup
				String js = "var d = document.getElementsByTagName('body')[0];"

				+ "var rWidth = d.style.width;" + "d.style.width='"
						+ (displayWidth - SCREEN_PADDING * 2)
						+ "px';"
						+ "var pageCount = Math.ceil(d.offsetHeight/"
						+ displayHeight
						+ ");"
						+ "d.style.width=rWidth;"
						+ "d.style.height='"
						+ displayHeight
						+ "px';"
						+ "d.style['padding-left']='"
						+ SCREEN_PADDING
						+ "px';"
						+ "d.style['padding-right']='"
						+ SCREEN_PADDING
						+ "px';"

						+ "d.style['-webkit-transition-property'] = '-webkit-transform';"
						+ "d.style['-webkit-transform-origin'] = \"0 0\";"
						+ "d.style['-webkit-transition-duration'] = '700ms';"
						+ "d.style['-webkit-transition-timing-function'] = 'linear';"

						+ "d.style.WebkitColumnCount= pageCount;"
						+ "d.style.WebkitColumnWidth='"
						+ (displayWidth - SCREEN_PADDING * 2) + "px';"

						+ "window.interface.setColumnCount(pageCount);"
						+ "window.interface.setColumnWidth(screen.width);";
				webView.loadUrl("javascript:(function(){" + js + "})()");
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
		case TOC_MENU:
			List<String> tocTitles = currentDoc.getTOC().getItemTitles();
			final CharSequence[] items = tocTitles
					.toArray(new CharSequence[tocTitles.size()]);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.tableOfContents);
			builder.setSingleChoiceItems(items, -1,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							currentPageIndex = 1;
							readDocumentTOCEntry(item);
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			return alert;
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
		int tocSize = currentDoc.getTOC().size();
		if (page <= 0) {
			currentPageIndex = 1;
			currentTOCIndex--;
			if (tocSize >= currentTOCIndex && currentTOCIndex >= 0) {
				readDocumentTOCEntry(currentTOCIndex);
				currentPageIndex = 1;
				new GoToPageTask().execute();
			} else
				currentTOCIndex = 0;
		} else if (page <= columnCount) {
			goToPage(page, true);
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

		new LoadDocTask(this).execute(entryIndex);
		progressDialog = ProgressDialog.show(ReaderView.this, "Please wait...",
				"Parsing..", true);

	}

	private void goToPage(int pageIndex, boolean withCSS3) {

		int moveWidth = (pageIndex - 1) * columnWidth;

		String js = "";
		if (withCSS3) {
			js = "var d = document.getElementsByTagName('body')[0];"
					+ "d.style['-webkit-transform'] = 'translate3d(-"
					+ moveWidth + "px,0px,0px)';";
		} else {
			js = "var d = document.getElementsByTagName('body')[0];"
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

	private class LoadDocTask extends AsyncTask<Integer, Void, String> {

		ReaderView activity = null;
		
		LoadDocTask(ReaderView activity){
			this.activity = activity;
		}
		
		@Override
		protected String doInBackground(Integer... params) {

			String htmlContent = "";
			try {
				List<TOCEntry> toc = currentDoc.getTOC().getItems();
				Resource resource = currentDoc.getResources().getByHref(toc.get(params[0].intValue()).getSrc());
				InputStream in = resource.getInputStream();

				final BufferedReader breader = new BufferedReader(
						new InputStreamReader(in));
				StringBuilder b = new StringBuilder();
				String line;

				while ((line = breader.readLine()) != null) {
					b.append(line);
				}
				breader.close();
				htmlContent = b.toString();

			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}

			return htmlContent;
		}

		@Override
		protected void onPostExecute(String result) {

			isPageLoaded = false;

			webView.clearHistory();
			webView.clearFormData();
			webView.clearCache(true);
			webView.loadData(result, "text/html", "utf-8");

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
			lock.lock();

			try {
				columnCount = count;
				isPageLoaded = true;
				condition.signalAll();
			} finally {
				lock.unlock();
			}

		}

		public void setColumnWidth(int width) {
			columnWidth = width;
		}
	}
}
