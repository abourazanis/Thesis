package thesis.drmReader;
import java.util.ArrayList;

import thesis.drmReader.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class DocumentAdapter extends ArrayAdapter<Document>{
	
	private final ArrayList<Document> documents;
	
	public DocumentAdapter(Context context,int textViewResourceId, ArrayList<Document> items ){
		super(context, textViewResourceId, items);
		documents = items;
	}
	
	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	static class ViewHolder{
		public ImageView imageView;
		public TextView textViewTop;
		public TextView textViewBottom;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// ViewHolder will buffer the assess to the individual fields of the row
		// layout

		ViewHolder holder;
		// Recycle existing view if passed as parameter
		// This will save memory and time on Android
		// This only works if the base layout for all classes are the same
		
		View view = convertView;
		if(view == null){
			LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.list_item, null);
			
			holder = new ViewHolder();
			holder.imageView = (ImageView)view.findViewById(R.id.icon);
			holder.textViewBottom = (TextView)view.findViewById(R.id.bottomtext);
			holder.textViewTop = (TextView)view.findViewById(R.id.toptext);
			
			view.setTag(holder);
		}else{
			holder = (ViewHolder)view.getTag();
		}
		
		Document doc = documents.get(position);
		if(doc != null){
			//holder.imageView.setImageURI(uri);
			holder.textViewBottom.setText(doc.getDocumentDetails());
			holder.textViewTop.setText(doc.getDocumentTitle());
		}
		
		return view;
	}
	
	

}
