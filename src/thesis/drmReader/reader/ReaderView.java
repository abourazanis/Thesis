
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

@SuppressLint("NewApi")
public class ReaderView extends SherlockFragmentActivity implements SimpleGestureListener,
        OnChangedListener, NavigationEventListener {

    static class DisplayInfo {
        static float density;

        static int densityDpi;

        static int widthPixels;

        static int heightPixels;

        static float scaledDensity;

        static float xdpi;

        static float ydpi;
    }

    private static final int FONT_SIZE_MENU = 1;

    private static final String TAG = "ReaderView";

    private static final int SCREEN_TAP_SIZE = 30;

    private static final int MAX_FONT_SIZE = 20;

    private static final int HANDLER_JS_CURPAGE = 1;

    private static final int HANDLER_JS_TOTPAGE = 2;

    private static final int HANDLER_SEEKBAR_CHANGING = 3;

    private static final int HANDLER_SEEKBAR_CHANGED = 4;

    private static final int HANDLER_SHOW_OSD = 5;

    private static final int HANDLER_HIDE_OSD = 6;

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

    private SimpleGestureFilter detector;

    private Decrypter decrypter;

    private int displayWidth;

    private int displayHeight;

    private String cache;

    // navigation variables
    private int mCurPage;// = 1;

    private float mCurPercentage;// = 0.0f;

    private int mMaxPage;// = 1;

    private Book currentDoc;

    private Navigator navigator;

    private Bundle mState;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("curResourceHref", navigator.getCurrentResource().getHref());
        savedInstanceState.putInt("curPage", mCurPage);
        savedInstanceState.putFloat("curPercentage", mCurPercentage);
        mState = null;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mState = savedInstanceState;
        navigator.gotoResource(savedInstanceState.getString("curResourceHref"), this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpUI();
        cache = this.getCacheDir().getAbsolutePath() + "/readertmp/";
        new File(cache).mkdirs();

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        Application application = getApplication();
        if (application instanceof BetterApplication) {
            ((BetterApplication) application).setActiveContext(getClass().getCanonicalName(), this);
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
                    author = currentDoc.getMetadata().getAuthors().get(0).toString();
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

    @SuppressWarnings("deprecation")
    private void setUpUI() {
        setContentView(R.layout.reader);

        webView = (WebView) findViewById(R.id.webView);
        // tail infos (chapter, pages)
        tvChapter = (TextView) findViewById(R.id.tvChapter);
        tvPages = (TextView) findViewById(R.id.tvPages);

        // frame layout for OSD
        flOSD = (FrameLayout) findViewById(R.id.flOSD);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        tvPageTitle = (TextView) findViewById(R.id.tvPageTitle);
        tvPageNumber = (TextView) findViewById(R.id.tvPageNumber);
        sbPages = (SeekBar) findViewById(R.id.sbPages);

        detector = new SimpleGestureFilter(this, this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        DisplayInfo.density = metrics.density;
        DisplayInfo.densityDpi = metrics.densityDpi;
        DisplayInfo.widthPixels = metrics.widthPixels;
        DisplayInfo.heightPixels = metrics.heightPixels;
        DisplayInfo.scaledDensity = metrics.scaledDensity;
        DisplayInfo.xdpi = metrics.xdpi;
        DisplayInfo.ydpi = metrics.ydpi;
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
                Log.i(TAG, "[CALLBACK_WV] void onPageFinished(view:" + view + ", url:" + url + ")");

                if (!ReaderUtils.isCoverResource(navigator.getCurrentResource(), currentDoc)) {
                    // calc total page number
                    // also, move to certain location
                    webView.loadUrl("javascript:getTotalPageNum()");
                    Log.d(TAG, "javascript:getTotalPageNum()");

                    // move to certain location
                    webView.loadUrl("javascript:openPageByPercentage(" + mCurPercentage + ")");
                    Log.d(TAG, "javascript:openPageByPercentage(" + mCurPercentage + ")");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("ReaderView",
                        cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

        });

        webView.setSelected(true);
        webView.setClickable(false);
        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setDefaultZoom(this.getZoomDensity());
        webSettings.setDefaultFontSize(12);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        // SeekBar change listener
        sbPages.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                webView.loadUrl("javascript:openPageByNum(" + (progress + 1) + ")");
                handler.sendMessage(Message.obtain(handler, HANDLER_SEEKBAR_CHANGED,
                        (Integer) (progress + 1)));
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                handler.sendMessage(Message.obtain(handler, HANDLER_SEEKBAR_CHANGING,
                        (Integer) (progress + 1)));
            }
        });
    }

    /**
     * Calc zoom density
     * 
     * @return
     */
    private ZoomDensity getZoomDensity() {
        ZoomDensity zd;
        if (DisplayInfo.densityDpi == 240) {
            zd = WebSettings.ZoomDensity.FAR;
        } else if (DisplayInfo.densityDpi == 160) {
            zd = WebSettings.ZoomDensity.MEDIUM;
        } else if (DisplayInfo.densityDpi == 120) {
            zd = WebSettings.ZoomDensity.CLOSE;
        } else {
            zd = WebSettings.ZoomDensity.MEDIUM;
        }
        return zd;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    navigate(mCurPage + 1);
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    navigate(mCurPage - 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    navigate(mCurPage + 1);
                    break;

                case KeyEvent.KEYCODE_BACK:
                    if (isOsdOn) {
                        flOSD.setVisibility(View.GONE);
                        isOsdOn = false;
                        return false;
                    }
                    break;
            }

        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getSupportMenuInflater().inflate(R.menu.read_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toc:
                Log.d(TAG, "TOC Selected");
                Iterator<TOCReference> it = currentDoc.getTableOfContents().getTocReferences()
                        .iterator();
                ArrayList<String> titles = new ArrayList<String>();
                while (it.hasNext()) {
                    TOCReference ref = (TOCReference) it.next();
                    titles.add(ref.getTitle());
                }
                Intent i = new Intent(this, TOCList.class);
                i.putStringArrayListExtra("titles", titles);
                i.putExtra("currentTitle",
                        ReaderUtils.getChapterName(currentDoc, navigator.getCurrentResource()));
                this.startActivityForResult(i, TOC_LIST);
                return true;
            case R.id.font_size:
                Log.d(TAG, "Font Selected");
                showDialog(FONT_SIZE_MENU);
                return true;
            case R.id.home:
                super.onBackPressed();
                return true;
            default:
                Log.d(TAG, "Default Selected");
                return super.onOptionsItemSelected(item);
        }
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
                NumberPicker picker = (NumberPicker) fontDialog.findViewById(R.id.num_picker);

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
                navigate(mCurPage - 1);
                break;
            case SimpleGestureFilter.SWIPE_LEFT:
                navigate(mCurPage + 1);
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
            navigate(mCurPage + 1);
        } else if (sub > (displayWidth - SCREEN_TAP_SIZE)) {
            navigate(mCurPage - 1);
        }// toggle OSD
        else {
            if (flOSD.getVisibility() == View.GONE) {
                if (!ReaderUtils.isCoverResource(navigator.getCurrentResource(), currentDoc))
                    handler.sendMessage(Message.obtain(handler, HANDLER_SHOW_OSD));
            } else {
                handler.sendMessage(Message.obtain(handler, HANDLER_HIDE_OSD));
            }
        }

    }

    private void navigate(int page) {

        if (page <= 0) {
            mCurPage = 1;
            navigator.gotoPreviousSpineSection(this);
            webView.loadUrl("javascript:openPageByPercentage(1.0)");
        } else if (page <= mMaxPage) {
            goToPage(page);
        } else {
            navigator.gotoNextSpineSection(this);
        }

    }

    private void readDocumentSpineEntry() {
        new LoadDocTask(this).execute();
    }

    private void goToPage(int pageIndex) {

        if (mCurPage < pageIndex)
            webView.loadUrl("javascript:nextPage()");
        else
            webView.loadUrl("javascript:prevPage()");

        mCurPage = pageIndex;
    }

    // for NumberPicker (font size change)
    @Override
    public void onChanged(NumberPicker picker, int oldVal, int newVal) {
        webSettings.setDefaultFontSize(newVal);
        readDocumentSpineEntry();
    }

    private class LoadDocTask extends BetterAsyncTask<Void, Void, String> implements
            BetterAsyncTaskCallable<Void, Void, String> {

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
            ((ReaderView) context).webView.loadDataWithBaseURL(null, result, null, "utf-8", null);
            // problem: http://code.google.com/p/android/issues/detail?id=1733
            // webView.loadData(result, "text/html", "utf-8");

        }

        @Override
        protected void onCancel(Context context) {

        }

        @Override
        public String call(BetterAsyncTask<Void, Void, String> task) throws Exception {
            String htmlContent = "";

            Resource resource = ((ReaderView) this.getCallingContext()).decrypter
                    .decrypt(((ReaderView) this.getCallingContext()).navigator.getCurrentResource());
            htmlContent = ReaderUtils.getModifiedDocument(
                    ((ReaderView) this.getCallingContext()).navigator.getBook(), resource,
                    displayWidth, displayHeight, cache,
                    ((ReaderView) this.getCallingContext()).decrypter);
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
            handler.sendMessage(Message.obtain(handler, HANDLER_JS_CURPAGE, (Integer) mCurPage));
        }

        public void setTotalPageNum(final int page) {
            mMaxPage = page;
            handler.sendMessage(Message.obtain(handler, HANDLER_JS_TOTPAGE));
        }
    }

    @Override
    public void navigationPerformed(NavigationEvent navigationEvent) {
        if (!navigationEvent.isBookChanged()) {
            if (navigationEvent.isResourceChanged()) {
                readDocumentSpineEntry();
                if (mState != null) {
                    mCurPage = mState.getInt("curPage");
                    mCurPercentage = mState.getFloat("curPercentage");
                    mState = null;
                } else {
                    mCurPage = 1;
                    mCurPercentage = 0.0f;
                }
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

                    animation = AnimationUtils
                            .loadAnimation(getApplicationContext(), R.anim.fadein);
                    flOSD.startAnimation(animation);
                    break;

                case HANDLER_HIDE_OSD:
                    animation = AnimationUtils.loadAnimation(getApplicationContext(),
                            R.anim.fadeout);
                    flOSD.startAnimation(animation);

                    flOSD.setVisibility(View.GONE);
                    isOsdOn = false;
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
        tvPageTitle.setText(ReaderUtils.getChapterName(currentDoc, navigator.getCurrentResource()));
        tvPageNumber.setText(String.valueOf(curPage) + "/" + String.valueOf(mMaxPage));
    }

    /**
     * Refreshes Page info.
     * 
     * @param curPage
     */
    private void refreshPage() {
        tvChapter.setText(ReaderUtils.getChapterName(currentDoc, navigator.getCurrentResource()));
        tvPages.setText(String.valueOf(mCurPage) + "/" + String.valueOf(mMaxPage));
    }
}
