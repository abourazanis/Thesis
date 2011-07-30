package thesis.drmReader.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

public class Utils {
	private final static String TAG = "thesis.drmReader.Utils";
	
	public static String getPackagePath(Context context){
		return getPackagePath(context,null);
	}
	
	public static String getPackagePath(Context context, String defaultValue){
		PackageManager pm = context.getPackageManager();
		String dataPath = defaultValue;
		try {
			dataPath = pm.getApplicationInfo(context.getPackageName(), 0).dataDir;
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
		
		return dataPath;
	}
	
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(l
					+ " cannot be cast to int without changing its value.");
		}
		return (int) l;
	}

	public static InputStream parseStringToIS(String xml) {
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		} catch (Exception e) {

		}
		return is;
	}
	
	/** Method to check whether external media available and writable. */

	public static boolean checkExternalMedia() {
		boolean stat;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// Can read and write the media
			stat = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// Can only read the media
			stat = false;
		} else {
			// Can't read or write
			stat = false;
		}

		return stat;
	}
	
	public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
    }
	
}
