package thesis.drmReader;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class DrmReader extends TabActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Reusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, ArchiveList.class);
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("archive").setIndicator("Archive",
	                      res.getDrawable(R.drawable.ic_tab_archive))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    //2nd Tab
	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, StoreList.class);
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("web").setIndicator("Web",
	                      res.getDrawable(R.drawable.ic_tab_web))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(0);
	}

}
