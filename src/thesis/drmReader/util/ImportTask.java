
package thesis.drmReader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.util.IOUtil;

import thesis.drmReader.EpubsDatabase;
import thesis.drmReader.R;
import thesis.drmReader.ui.ArchiveListActivity;
import thesis.drmReader.ui.BookLink;
import thesis.drmReader.util.concurrent.BetterAsyncTask;
import thesis.drmReader.util.concurrent.BetterAsyncTaskCallable;
import thesis.sec.Decrypter;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ImportTask extends BetterAsyncTask<Void, Integer, Integer> implements
        BetterAsyncTaskCallable<Void, Integer, Integer> {

    private final static int IMPORT_SUCCESS = 100;

    private final static int IMPORT_INCOMPLETE = 200;

    private final static int IMPORT_ERROR = 300;

    private final static int IMPORT_FILENOTFOUND = 400;

    private final static int IMPORT_INVALID_KEY = 500;

    public String[] mFiles;

    public String mInvalidFiles;

    public ImportTask(Context context) {
        super(context);
        this.setCallable(this);
    }

    public ImportTask(String[] files, Context context) {
        this(context);
        mFiles = files;
    }

    @Override
    protected void before(Context context) {
        if (((ArchiveListActivity) context).mProgressOverlay == null) {
            ((ArchiveListActivity) context).mProgressOverlay = ((ViewStub) ((ArchiveListActivity) context)
                    .findViewById(R.id.stub_update)).inflate();
            ((ArchiveListActivity) context).mUpdateProgress = (ProgressBar) ((ArchiveListActivity) context)
                    .findViewById(R.id.ProgressBarShowListDet);

            final View cancelButton = ((ArchiveListActivity) context).mProgressOverlay
                    .findViewById(R.id.overlayCancel);
            final Context c = context;
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ((ArchiveListActivity) c).onCancelTasks();
                }
            });
        }

        ((ArchiveListActivity) context).mUpdateProgress.setIndeterminate(false);
        ((ArchiveListActivity) context).mUpdateProgress.setProgress(0);
        ((ArchiveListActivity) context)
                .showOverlay(((ArchiveListActivity) context).mProgressOverlay);
    }

    @Override
    public Integer call(BetterAsyncTask<Void, Integer, Integer> task) throws Exception {
        final String[] files = mFiles;

        int resultCode = IMPORT_SUCCESS;

        for (int i = 0; i < files.length; i++) {
            if (isCancelled()) {
                resultCode = IMPORT_INCOMPLETE;
                break;
            }
            publishProgress((i + 1) * 1, 100 * (files.length + 1));
            String epubFilePath = files[i];
            try {
                FileInputStream epubStream = new FileInputStream(epubFilePath);
                BookLink epubLink = new BookLink();

                Decrypter decrypter = new Decrypter(epubFilePath, this.getCallingContext());
                publishProgress((i + 1) * 10, 100 * (files.length + 1));
                Metadata meta = (new EpubReader(decrypter)).readEpubMetadata(epubStream);
                publishProgress((i + 1) * 30, 100 * (files.length + 1));

                epubLink.setMeta(meta);
                if (meta.getCoverImage() != null)
                    epubLink.setCoverUrl(meta.getCoverImage().getHref());

                File sdDir = Environment.getExternalStorageDirectory();
                if (sdDir.exists() && sdDir.canRead()) {
                    File docDir = new File(sdDir.getAbsolutePath() + "/drmReader");
                    if (!docDir.exists())
                        docDir.mkdirs();
                    publishProgress((i + 1) * 50, 100 * (files.length + 1));
                    if (docDir.exists() && docDir.canRead()) {
                        String fileName = docDir + "/" + epubLink.getMeta().getFirstTitle()
                                + ".epub";
                        publishProgress((i + 1) * 70, 100 * (files.length + 1));
                        IOUtil.copy(new FileInputStream(epubFilePath), new FileOutputStream(
                                fileName));
                        publishProgress((i + 1) * 90, 100 * (files.length + 1));
                        epubLink.setId(String.valueOf(fileName));

                        Resource coverResource = decrypter.decrypt(meta.getCoverImage());
                        epubLink.getMeta().setCoverImage(coverResource);

                        Utils.putCoverToCache(this.getCallingContext().getApplicationContext(),
                                coverResource.getData(), meta.getFirstTitle());

                        EpubsDatabase.addEpub(epubLink, this.getCallingContext());
                        publishProgress((i + 1) * 100, 100 * (files.length + 1));
                    }
                }
            } catch (InvalidKeyException e) {
                resultCode = IMPORT_INVALID_KEY;
                mInvalidFiles += "," + epubFilePath.substring(0, epubFilePath.lastIndexOf("/"));
            } catch (FileNotFoundException e) {
                resultCode = IMPORT_FILENOTFOUND;
            } catch (IOException e) {
                resultCode = IMPORT_ERROR;
            }
        }
        // renew FTS3 table
        EpubsDatabase.onRenewFTSTable(this.getCallingContext());
        publishProgress(100 * (files.length + 1), 100 * (files.length + 1));

        return resultCode;
    }

    @Override
    protected void after(Context context, Integer result) {
        switch (result) {
            case IMPORT_SUCCESS:
                Toast.makeText(context, context.getString(R.string.import_success),
                        Toast.LENGTH_SHORT).show();
                break;
            case IMPORT_ERROR:
                ((ArchiveListActivity) context).showDialog(Constants.IMPORT_DOCUMENT_ERROR);
                break;
            case IMPORT_FILENOTFOUND:
                ((ArchiveListActivity) context).showDialog(Constants.IMPORT_DOCUMENT_FILENOTFOUND);
                break;
            case IMPORT_INVALID_KEY:
                ((ArchiveListActivity) context).showDialog(Constants.IMPORT_DOCUMENT_INVALIDKEY);
                break;

        }

        Log.d("import task", "hide overlay import task finished");
        ((ArchiveListActivity) context)
                .hideOverlay(((ArchiveListActivity) context).mProgressOverlay);

    }

    @Override
    protected void handleError(Context context, Exception error) {
        Log.d("import task - Error", error.getMessage());
        ((ArchiveListActivity) context)
                .hideOverlay(((ArchiveListActivity) context).mProgressOverlay);
        ((ArchiveListActivity) context).showDialog(Constants.IMPORT_DOCUMENT_ERROR);

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        final ProgressBar progress = ((ArchiveListActivity) this.getCallingContext()).mUpdateProgress;
        if (progress != null) {
            progress.setMax(values[1]);
            progress.setProgress(values[0]);
        }
    }

    @Override
    protected void onCancel(Context context) {
        ((ArchiveListActivity) context)
                .hideOverlay(((ArchiveListActivity) context).mProgressOverlay);
    }
}
