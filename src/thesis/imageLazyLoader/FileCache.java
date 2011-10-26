
package thesis.imageLazyLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class FileCache {

    private File cacheDir;

    public FileCache(Context context, String folder) {
        // Find the dir to save cached images
        // if
        // (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        // cacheDir=new
        // File(android.os.Environment.getExternalStorageDirectory(),folder);
        // else
        cacheDir = new File(context.getCacheDir(), folder);
        if (!cacheDir.exists())
            cacheDir.mkdirs();
    }

    public File getFile(String url) {
        String filename = String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
//        if (!f.exists()){
//            Log.d("contentprovider","file doesnt exists");
//            FileWriter writer;
//            try {
//                writer = new FileWriter(f);
//                writer.flush();
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        return f;

    }

    public void clear() {
        File[] files = cacheDir.listFiles();
        for (File f : files)
            f.delete();
    }

}
