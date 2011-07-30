package thesis.drmReader;


import thesis.drmReader.util.concurrent.BetterApplication;

public class DrmReaderApplication extends BetterApplication {
	private ImageCache mImageCache;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public synchronized ImageCache getImageCache() {
        if (mImageCache == null) {
            mImageCache = ImageCache.getInstance(getApplicationContext());
        }
        return mImageCache;
    }

    @Override
    public void onLowMemory() {
        if (mImageCache != null) {
            mImageCache.clear();
        }
    }
	
}
