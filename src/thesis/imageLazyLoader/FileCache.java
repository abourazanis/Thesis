package thesis.imageLazyLoader;

import java.io.File;
import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    
    public FileCache(Context context,String folder){
        //Find the dir to save cached images
//        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
//            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),folder);
//        else
            cacheDir=new File(context.getCacheDir(),folder);
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        String filename=String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;
        
    }
    
    public void clear(){
        File[] files=cacheDir.listFiles();
        for(File f:files)
            f.delete();
    }

}