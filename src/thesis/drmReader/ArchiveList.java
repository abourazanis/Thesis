package thesis.drmReader;

import java.io.File;
import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class ArchiveList extends ListActivity {
	
	private ArrayList<Document> docList;
	private DocumentAdapter docAdapter;
	private ProgressDialog progressDialog;
	private Runnable viewDocuments;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclist);
		
		docList = new ArrayList<Document>();
		docAdapter = new DocumentAdapter(this,R.layout.list_item, docList);
		setListAdapter(docAdapter);
		
		viewDocuments = new Runnable(){
			@Override
			public void run(){
				getDocuments();
			}
		};
		
		Thread thread = new Thread(null, viewDocuments, "ViewDocuments");
		thread.start();
		progressDialog = ProgressDialog.show(ArchiveList.this, "Please wait...", "Retrieving data..", true);
	}
    
    private void getDocuments(){
    	
    		File sdDir = Environment.getExternalStorageDirectory();
    		if(sdDir.exists() && sdDir.canRead()){
    			File docDir = new File(sdDir.getAbsolutePath() + "/drmReader");
    			if(docDir.exists() && docDir.canRead()){
    				//String[] docList = docDir.list();
    				
    				//TODO:based on the list of documents,
    				// generate the relevant Document classes - maybe implement it in the library
    				// and add them in the docList
    				
    			}
    		}
    	
    	
    	try{
    		docList = new ArrayList<Document>();
    		Document doc = new Document("First Document","Sample Pic","Doc Details");
    		Document doc2 = new Document("Second  Document","Sample Pic2222","Doc Details222");
    		Document doc3 = new Document("Third  Document","Sample Pic3333","Doc Details333");
    		
    		docList.add(doc);
    		docList.add(doc2);
    		docList.add(doc3);
    	}catch (Exception e) { 
            Log.e("BACKGROUND_PROC", e.getMessage());
         }
    	runOnUiThread(returnResults);
    }
    
    private Runnable returnResults = new Runnable() {

        @Override
        public void run() {
            if(docList != null && docList.size() > 0){
                docAdapter.notifyDataSetChanged();
                for(int i=0;i<docList.size();i++)
                docAdapter.add(docList.get(i));
            }
            progressDialog.dismiss();
            docAdapter.notifyDataSetChanged();
        }
      };
}