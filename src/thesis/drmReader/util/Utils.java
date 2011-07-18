package thesis.drmReader.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
	
}
