package thesis.drmReader.util;

import java.util.List;

import thesis.drmReader.ui.BookLink;
import android.content.Context;
import android.util.Log;

public abstract class EpubLoader extends
		IncrementalAsyncTaskLoader<String, Integer, List<BookLink>> {
	static final String TAG = EpubLoader.class.getSimpleName();
	public String file;
	private List<BookLink> data;

	public EpubLoader(
			Context context,
			IncrementalAsyncTaskLoaderCallbacks<Integer, List<BookLink>> callbacks,
			String file) {
		super(context, callbacks);
		this.file = file;
	}

	/* Runs on the UI thread */
	@Override
	public void deliverResult(List<BookLink> data) {
		if (isReset()) {
			// An async query came in while the loader is stopped
			return;
		}

		this.data = data;

		// if (isStarted()) {
		super.deliverResult(data);
		// }
	}

	/**
	 * Must be called from the UI thread
	 */
	@Override
	protected void onStartLoading() {
		// forceLoad();
		if (data != null) {
			deliverResult(data);
		}

		if (takeContentChanged() || data == null) {
			forceLoad();
		}

	}

	/**
	 * Must be called from the UI thread
	 */
	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	public void onCanceled(List<BookLink> feed) {
		super.onCanceled(feed);
	}

	@Override
	protected void onReset() {
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();
		
		//data = null;
	}
}
