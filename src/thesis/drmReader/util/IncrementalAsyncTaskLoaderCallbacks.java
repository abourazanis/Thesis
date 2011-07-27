package thesis.drmReader.util;

import android.support.v4.app.LoaderManager;

public interface IncrementalAsyncTaskLoaderCallbacks<T, V> extends LoaderManager.LoaderCallbacks<V> {
public void onPreExecute();
public void onProgressUpdate(T progress);
public void onPostExecute(V result);
}
