package thesis.drmReader.reader;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class TOCList extends ListActivity {
	
	private TOCListAdapter adapter;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}
		
		ArrayList<String> titles = extras.getStringArrayList("titles");
		String selectedTitle = extras.getString("currentTitle");
		
		adapter = new TOCListAdapter(this,titles);
		adapter.setNotifyOnChange(true);
		this.setListAdapter(adapter);
		
		int selected = titles.indexOf(selectedTitle);
		adapter.setSelectedPosition(selected);
		
		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		adapter.setSelectedPosition(position);
		// Get the item that was clicked
		Object o = this.getListAdapter().getItem(position);
		String title = o.toString();
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra("chapter", title);
		this.setResult(RESULT_OK, resultIntent);
		this.finish();
	}

}
