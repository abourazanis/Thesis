package thesis.drmReader.reader;

import java.util.List;

import thesis.drmReader.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

public class TOCListAdapter extends ArrayAdapter<String> {
	private final Activity context;
	private final List<String> titles;
	private int selectedPos = -1;

	public TOCListAdapter(Activity context, List<String> titles) {
		super(context, R.layout.toclist_item, titles);
		this.context = context;
		this.titles = titles;
	}
	
	public void setSelectedPosition(int pos){
		selectedPos = pos;
		// inform the view of this change
		notifyDataSetChanged();
	}
	
	public int getSelectedPosition(){
		return selectedPos;
	}

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	static class ViewHolder {
		public CheckedTextView textView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// ViewHolder will buffer the assess to the individual fields of the row
		// layout

		ViewHolder holder;
		// Recycle existing view if passed as parameter
		// This will save memory and time on Android
		// This only works if the base layout for all classes are the same
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.toclist_item, null, true);
			holder = new ViewHolder();
			holder.textView = (CheckedTextView) rowView.findViewById(R.id.tocTitle);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		holder.textView.setChecked(false);
		
		if(selectedPos == position){
			holder.textView.setChecked(true);
		}

		holder.textView.setText(titles.get(position));
		return rowView;
	}

}
