package thesis.drmReader.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Iterator;

import nl.siegmann.epublib.browsersupport.NavigationEvent;
import nl.siegmann.epublib.browsersupport.NavigationEventListener;
import nl.siegmann.epublib.browsersupport.Navigator;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.StringUtil;
import thesis.drmReader.R;
import thesis.drmReader.db.EpubsContract.Epubs;
import thesis.drmReader.reader.SimpleGestureFilter.SimpleGestureListener;
import thesis.drmReader.ui.NumberPicker;
import thesis.drmReader.ui.NumberPicker.OnChangedListener;
import thesis.drmReader.util.ReaderUtils;
import thesis.drmReader.util.concurrent.BetterApplication;
import thesis.drmReader.util.concurrent.BetterAsyncTask;
import thesis.drmReader.util.concurrent.BetterAsyncTaskCallable;
import thesis.sec.Decrypter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

@SuppressLint("NewApi")
public class ReaderView extends SherlockFragmentActivity implements
		SimpleGestureListener, NavigationEventListener, OnChangedListener {

	private static final int FONT_SIZE_MENU = 1;

	private static final String TAG = "ReaderView";

	private static final int HANDLER_JS_CURPAGE = 1;

	private static final int HANDLER_JS_TOTPAGE = 2;

	private static final int HANDLER_SEEKBAR_CHANGING = 3;

	private static final int HANDLER_SEEKBAR_CHANGED = 4;

	private static final int HANDLER_SHOW_OSD = 5;

	private static final int HANDLER_HIDE_OSD = 6;

	private static final int HANDLER_NAVIGATE = 7;

	private static final int HANDLER_DAYNIGHT = 8;
	
	private static final int HANDLER_UPDATE_MONOCLE = 9;

	private static final int TOC_LIST = 100;

	private WebView webView;

	private WebSettings webSettings;

	private TextView tvChapter;

	private TextView tvPages;

	// OSD layer
	private FrameLayout flOSD;

	private TextView tvInfo;

	private TextView tvPageTitle;

	private TextView tvPageNumber;

	private SeekBar sbPages;

	private boolean isOsdOn = false;
	private boolean isCoverResource = false;

	private SimpleGestureFilter detector;

	private Decrypter decrypter;

	private int displayWidth;

	private int displayHeight;

	private String cache;

	// navigation variables
	private int mCurPage = 1;

	private float mCurPercentage = 0.0f;
	private float mPercentage = 0.0f;

	private int mMaxPage;// = 1;
	private int mCurFontScale = 5;
	private boolean mCurNightMode = false;

	private String[] fontScales;

	private Book currentDoc;

	private Navigator navigator;


	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("curResourceHref", navigator
				.getCurrentResource().getHref());
		savedInstanceState.putInt("curPage", mCurPage);
		savedInstanceState.putInt("curFontScale", mCurFontScale);
		savedInstanceState.putBoolean("curNightMode", mCurNightMode);
		savedInstanceState.putFloat("curPercentage", mCurPercentage);
		savedInstanceState.putBoolean("isCoverResource", isCoverResource);

		super.onSaveInstanceState(savedInstanceState);

	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		mCurPage = savedInstanceState.getInt("curPage", 1);
		mCurPercentage = mPercentage = savedInstanceState.getFloat("curPercentage", 0);
		mCurFontScale = savedInstanceState.getInt("curFontScale", 5);
		mCurNightMode = savedInstanceState.getBoolean("curNightMode", false);
		isCoverResource = savedInstanceState.getBoolean("isCoverResource", false);
		
		navigator.gotoResource(savedInstanceState.getString("curResourceHref"),
				this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fontScales = new String[16];
		int num = 5;
		for (int i = 0; i < fontScales.length; i++) {
			fontScales[i] = Double.toString(num / 10.0);
			num++;
		}

		setUpUI();
		cache = this.getCacheDir().getAbsolutePath() + "/readertmp/";
		new File(cache).mkdirs();

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		Application application = getApplication();
		if (application instanceof BetterApplication) {
			((BetterApplication) application).setActiveContext(getClass()
					.getCanonicalName(), this);
		}

		String docSrc = extras.getString(Epubs.FILENAME);
		if (docSrc != null && docSrc != "") {
			try {
				FileInputStream epubStream = new FileInputStream(docSrc);

				decrypter = new Decrypter(docSrc, this);

				currentDoc = (new EpubReader(decrypter)).readEpub(epubStream);

				navigator = new Navigator(currentDoc); // here we have as
														// current resource the
														// cover
				navigator.addNavigationEventListener(this);

				if (savedInstanceState == null) {
					readDocumentSpineEntry();
				}
				String author = null;
				if (currentDoc.getMetadata().getAuthors().size() > 0)
					author = currentDoc.getMetadata().getAuthors().get(0)
							.toString();
				tvInfo.setText(currentDoc.getTitle() + " "
						+ (author == null ? " " : "(" + author + ")"));
			} catch (InvalidKeyException e) {

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void setUpUI() {
		setContentView(R.layout.reader);

		webView = (WebView) findViewById(R.id.webView);
		// tail infos (chapter, pages)
		tvChapter = (TextView) findViewById(R.id.tvChapter);
		tvPages = (TextView) findViewById(R.id.tvPages);
		tvPages.setTextColor(Color.BLACK);
		tvChapter.setTextColor(Color.BLACK);

		// frame layout for OSD
		flOSD = (FrameLayout) findViewById(R.id.flOSD);
		tvInfo = (TextView) findViewById(R.id.tvInfo);
		tvPageTitle = (TextView) findViewById(R.id.tvPageTitle);
		tvPageNumber = (TextView) findViewById(R.id.tvPageNumber);
		sbPages = (SeekBar) findViewById(R.id.sbPages);
		
		//set up menu buttons
		
		Button btnTOC = (Button)findViewById(R.id.btnTOC);
		Button btnDayNight = (Button)findViewById(R.id.btnDayNight);
		Button btnFont = (Button)findViewById(R.id.btnFont);
		Button btnHome = (Button)findViewById(R.id.btnHome);
		
		
		btnTOC.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	Iterator<TOCReference> it = currentDoc.getTableOfContents()
						.getTocReferences().iterator();
				ArrayList<String> titles = new ArrayList<String>();
				while (it.hasNext()) {
					TOCReference ref = (TOCReference) it.next();
					titles.add(ref.getTitle());
				}
				Intent i = new Intent(ReaderView.this, TOCList.class);
				i.putStringArrayListExtra("titles", titles);
				i.putExtra(
						"currentTitle",
						ReaderUtils.getChapterName(currentDoc,
								navigator.getCurrentResource()));
				ReaderView.this.startActivityForResult(i, TOC_LIST);
		    }
		});
		
		btnDayNight.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	mCurNightMode = !mCurNightMode;
				handler.sendMessage(Message.obtain(handler, HANDLER_DAYNIGHT));
		    }
		});
		
		btnFont.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	showDialog(FONT_SIZE_MENU);
		    }
		});
		
		btnHome.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        ReaderView.this.onBackPressed();
		    }
		});
		

		detector = new SimpleGestureFilter(this, this);
		detector.setMode(SimpleGestureFilter.MODE_DOUBLETAP);// catch only
																// doubletap -
																// let other
																// pass

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;

		/**
		 * XXX: android emulator bug in Gingerbread and IceCream Sandwich
		 * emulators: http://code.google.com/p/android/issues/detail?id=12987
		 * the android bridge with javascript interface does not work.
		 */
		webView.addJavascriptInterface(new AndroidBridge(), "android");
		webView.setWebViewClient(new WebViewClient() {

			public void onPageFinished(WebView view, String url) {
				if (isCoverResource)
					Toast.makeText(getApplicationContext(),
							"Double tap to open book.", Toast.LENGTH_LONG)
							.show();
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
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY); // remove
																	// right
																	// space
																	// from
																	// webview

		// SeekBar change listener
		sbPages.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress();
				webView.loadUrl("javascript:openPageByNum(" + (progress + 1)
						+ ")");
				handler.sendMessage(Message.obtain(handler,
						HANDLER_SEEKBAR_CHANGED, (Integer) (progress + 1)));
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				handler.sendMessage(Message.obtain(handler,
						HANDLER_SEEKBAR_CHANGING, (Integer) (progress + 1)));
			}
		});
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_UP) {
			int keyCode = event.getKeyCode();

			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				navigate(1);
				if (!isOsdOn) {
					handler.sendMessage(Message.obtain(handler,
							HANDLER_SHOW_OSD));
				} else {
					handler.sendMessage(Message.obtain(handler,
							HANDLER_HIDE_OSD));
				}
				break;

			case KeyEvent.KEYCODE_DPAD_LEFT:
				navigate(-1);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				navigate(1);
				break;

			case KeyEvent.KEYCODE_BACK:
				if (isOsdOn) {
					handler.sendMessage(Message.obtain(handler,
							HANDLER_HIDE_OSD));
					return false;
				}
				break;
			}

		}
		return super.dispatchKeyEvent(event);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case TOC_LIST:
				String title = data.getStringExtra("chapter");
				if (title != null) {
					String chapterId = null;
					Iterator<TOCReference> it = currentDoc.getTableOfContents()
							.getTocReferences().iterator();
					while (it.hasNext()) {
						TOCReference ref = (TOCReference) it.next();
						if (ref.getTitle().equalsIgnoreCase(title))
							chapterId = ref.getResourceId();
					}
					navigator.gotoResourceId(chapterId, this);
				}
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case FONT_SIZE_MENU:

			Dialog fontDialog = new Dialog(this);

			fontDialog.setContentView(R.layout.dialog_fontsize);
			fontDialog.setTitle(R.string.font_size_msg);
			fontDialog.setCanceledOnTouchOutside(true);
			fontDialog.setCancelable(true);
			NumberPicker picker = (NumberPicker) fontDialog
					.findViewById(R.id.num_picker);

			picker.setOnChangeListener(this);
			picker.setRange(0, fontScales.length - 1, fontScales);
			picker.setCurrent(mCurFontScale); // 1.0
			return fontDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	@Override
	public void onDoubleTap() {
		if (isCoverResource) {
			navigate(1);// go to first chapter
		} else {
			if (flOSD.getVisibility() == View.GONE) {
				handler.sendMessage(Message.obtain(handler, HANDLER_SHOW_OSD));
			} else {
				handler.sendMessage(Message.obtain(handler, HANDLER_HIDE_OSD));
			}
		}
	}

	@Override
	public void onSwipe(int direction) {
		// DO nothing - swipe events are tracked by the webview
	}

	private void navigate(int direction/* page */) {

		if (direction > 0)// next chapter
			navigator.gotoNextSpineSection(this);
		else {
			mPercentage = 1.0f; //end of chapter
			navigator.gotoPreviousSpineSection(this);
			webView.loadUrl("javascript:openPageByPercentage(1.0)");
		}

	}

	private void readDocumentSpineEntry() {
		new LoadDocTask(this).execute();
	}

	@Override
	public void onChanged(NumberPicker picker, int oldVal, int newVal) {
		mCurFontScale = newVal;
		webView.loadUrl("javascript:setFontScale(" + fontScales[newVal] + ")");

	}

	private class LoadDocTask extends BetterAsyncTask<Void, Void, String>
			implements BetterAsyncTaskCallable<Void, Void, String> {

		public LoadDocTask(Context context) {
			super(context);
			this.setCallable(this);
		}

		@Override
		protected void before(Context context) {

		}

		@Override
		protected void handleError(Context context, Exception error) {
			if (StringUtil.isNotBlank(error.getMessage()))
				Log.e(TAG, error.getLocalizedMessage());

		}

		@Override
		protected void after(Context context, String result) {
			((ReaderView) context).webView.clearHistory();
			((ReaderView) context).webView.clearFormData();
			((ReaderView) context).webView.clearCache(true);
			((ReaderView) context).isCoverResource = ReaderUtils
					.isCoverResource(((ReaderView) context).navigator
							.getCurrentResource(),
							((ReaderView) context).currentDoc);
			((ReaderView) context).webView.loadDataWithBaseURL(null, result,
					"text/html", "utf-8", null);
			// problem: http://code.google.com/p/android/issues/detail?id=1733
			// webView.loadData(result, "text/html", "utf-8");

		}

		@Override
		protected void onCancel(Context context) {

		}

		@Override
		public String call(BetterAsyncTask<Void, Void, String> task)
				throws Exception {
			String htmlContent = "";

			Resource resource = ((ReaderView) this.getCallingContext()).decrypter
					.decrypt(((ReaderView) this.getCallingContext()).navigator
							.getCurrentResource());
			htmlContent = ReaderUtils
					.getModifiedDocument(
							((ReaderView) this.getCallingContext()).navigator
									.getBook(), resource, displayWidth,
							displayHeight, cache, ((ReaderView) this
									.getCallingContext()).decrypter);
			return htmlContent;

		}

	}

	/**
	 * Bridge for Javascript functions
	 */
	class AndroidBridge {
		public void setCurPageLocation(final int page, final float percentage)
				throws InterruptedException {
			mCurPage = page;
			mCurPercentage = percentage;
			handler.sendMessage(Message.obtain(handler, HANDLER_JS_CURPAGE,
					(Integer) mCurPage));
		}

		public void setTotalPageNum(final int page) {
			mMaxPage = page;
			handler.sendMessage(Message.obtain(handler, HANDLER_JS_TOTPAGE));
		}

		public void navigate(final int direction) {
			handler.sendMessage(Message.obtain(handler, HANDLER_NAVIGATE,
					(Integer) direction));
		}

		public void updateViewValues() {
			
			// Future versions of WebView may not support method use on other thread..
			// so we use handler to call methods on UI thread..
			handler.sendMessage(Message.obtain(handler, HANDLER_UPDATE_MONOCLE));
			
			handler.sendMessage(Message.obtain(handler, HANDLER_DAYNIGHT));
		}
	}

	@Override
	public void navigationPerformed(NavigationEvent navigationEvent) {
		if (!navigationEvent.isBookChanged()) {
			if (navigationEvent.isResourceChanged() || isCoverResource) {
				readDocumentSpineEntry();
			}
		}
	}

	/**
	 * Handler for epub viewer
	 */
	private Handler handler = new Handler() {
		Animation animation;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_JS_CURPAGE:
				refreshOSD((Integer) msg.obj);
				refreshPage();
				sbPages.setProgress((Integer) msg.obj - 1);
				break;

			case HANDLER_JS_TOTPAGE:
				sbPages.setMax(mMaxPage - 1);
				break;

			case HANDLER_SEEKBAR_CHANGING:
				refreshOSD((Integer) msg.obj);
				break;

			case HANDLER_SEEKBAR_CHANGED:
				refreshPage();
				break;

			case HANDLER_SHOW_OSD:
				flOSD.setVisibility(View.VISIBLE);
				isOsdOn = true;

				animation = AnimationUtils.loadAnimation(
						getApplicationContext(), R.anim.fadein);
				flOSD.startAnimation(animation);
				break;

			case HANDLER_HIDE_OSD:
				animation = AnimationUtils.loadAnimation(
						getApplicationContext(), R.anim.fadeout);
				flOSD.startAnimation(animation);

				flOSD.setVisibility(View.GONE);
				isOsdOn = false;
				break;
			case HANDLER_NAVIGATE:
				navigate((Integer) msg.obj);
				break;
			case HANDLER_DAYNIGHT:
				tvPages.setTextColor(mCurNightMode ? Color.WHITE : Color.BLACK);
				tvChapter.setTextColor(mCurNightMode ? Color.WHITE
						: Color.BLACK);
				webView.loadUrl("javascript:toggleDayNight(" + mCurNightMode
						+ ")");
				break;
			case HANDLER_UPDATE_MONOCLE:
				webView.loadUrl("javascript:updateMonocle(" + mPercentage + "," +  fontScales[mCurFontScale] + ")");
				break;
				
			}
		}
	};

	/**
	 * Refreshes OSD
	 * 
	 * @param curPage
	 */
	private void refreshOSD(int curPage) {
		tvPageTitle.setText(ReaderUtils.getChapterName(currentDoc,
				navigator.getCurrentResource()));
		tvPageNumber.setText(String.valueOf(curPage) + "/"
				+ String.valueOf(mMaxPage));
		if(mMaxPage == 1){
			tvPageNumber.setVisibility(TextView.GONE);
			sbPages.setVisibility(SeekBar.GONE);
		}else{
			tvPageNumber.setVisibility(TextView.VISIBLE);
			sbPages.setVisibility(SeekBar.VISIBLE);
		}
	}

	/**
	 * Refreshes Page info.
	 * 
	 * @param curPage
	 */
	private void refreshPage() {
		tvChapter.setText(ReaderUtils.getChapterName(currentDoc,
				navigator.getCurrentResource()));
		tvPages.setText(String.valueOf(mCurPage) + "/"
				+ String.valueOf(mMaxPage));
	}

}
