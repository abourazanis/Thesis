package thesis.drmReader;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipInputStream;

import thesis.pedlib.ped.Document;
import thesis.pedlib.ped.PedReader;
import thesis.pedlib.ped.TOCEntry;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class ReaderView extends Activity {

	private WebView webView;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		webView = new WebView(this);
        setContentView(webView);
        Bundle extras = getIntent().getExtras();
        if(extras == null){
        	return;
        }
        
        String docSrc = extras.getString("docSrc");
        if(docSrc != null && docSrc != ""){
        	PedReader reader = new PedReader();
        	try{
        	ZipInputStream ped = new ZipInputStream(
					new FileInputStream(docSrc));
        	Document currentDoc = reader.readPed(ped, "UTF-8");
        	
        	List<TOCEntry> toc = currentDoc.getTOC().getItems();
        	InputStream in = currentDoc.getResources().getByHref(toc.get(1).getSrc()).getInputStream();
        	
        	final BufferedReader breader = new BufferedReader(new InputStreamReader(in));
        	StringBuilder b = new StringBuilder(); 
        	String line;
        	 
        	while ((line = breader.readLine()) != null) {
        	b.append(line);
        	}
        	breader.close();
        	
        	webView.loadData(b.toString(), "text/html", "utf-8");
        	}
        	catch(Exception e){
        		Log.e("FUCK",e.getMessage());
        	}
        	
        }
        
        
	}

}
