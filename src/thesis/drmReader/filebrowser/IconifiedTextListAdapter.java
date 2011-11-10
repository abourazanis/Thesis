package thesis.drmReader.filebrowser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import thesis.drmReader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;

import android.widget.ImageView;
import android.widget.TextView;

public class IconifiedTextListAdapter extends ArrayAdapter<IconifiedText> {

	/** Remember our context so we can use it when constructing views. */
	private Context mContext;

	private List<IconifiedText> mItems;

	public IconifiedTextListAdapter(Context context, int textViewResourceId,
			ArrayList<IconifiedText> items) {
		super(context, textViewResourceId, items);
		mContext = context;
		mItems = items;
	}

	/** @return The number of items in the */
	public int getCount() {
		return mItems.size();
	}

	public boolean isSelectable(int position) {
		try {
			return mItems.get(position).isSelectable();
		} catch (IndexOutOfBoundsException aioobe) {
			return false;
			// return super.isSelectable(position);
		}
	}

	/** Use the array index as a unique id. */
	public long getItemId(int position) {
		return position;
	}

	public List<IconifiedText> getSelectedItems() {
		List<IconifiedText> items = new ArrayList<IconifiedText>();
		Iterator<IconifiedText> it = mItems.iterator();
		while (it.hasNext()) {
			IconifiedText item = it.next();
			if (item.isChecked())
				items.add(item);
		}
		return items;
	}

	public void setSelectedItems(String[] itemNames) {
		for (String item : itemNames) {
			Iterator<IconifiedText> it = mItems.iterator();
			while (it.hasNext()) {
				IconifiedText icItem = it.next();
				if (icItem.getText().equalsIgnoreCase(item)) {
					icItem.toggle();
				}
			}
		}
		this.notifyDataSetChanged();

	}

	public void clearSelections() {
		Iterator<IconifiedText> it = mItems.iterator();
		while (it.hasNext()) {
			IconifiedText item = it.next();
			if (item.isChecked())
				item.toggle();
		}
		this.notifyDataSetChanged();
	}

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	static class ViewHolder {
		public ImageView icon;
		public TextView filename;
		public TextView filesize;
		public CheckBox checkbox;
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
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.browser_filerow, null);

			holder = new ViewHolder();
			holder.icon = (ImageView) view.findViewById(R.id.browser_file_icon);
			holder.filename = (TextView) view
					.findViewById(R.id.browser_filename);
			holder.filesize = (TextView) view
					.findViewById(R.id.browser_filesize);
			holder.checkbox = (CheckBox) view
					.findViewById(R.id.browser_checkbox);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		final int pos = position;
		holder.checkbox.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				((CheckBox) v).toggle();
				mItems.get(pos).toggle();
				((FileBrowser) mContext).showButtonActions();
			}
		});

		if (!mItems.get(position).isSelectable()) {
			holder.checkbox.setVisibility(View.INVISIBLE);
		} else {
			holder.checkbox.setVisibility(View.VISIBLE);
		}

		holder.checkbox.setChecked(mItems.get(position).isChecked());
		holder.filename.setText(mItems.get(position).getText());
		holder.icon.setImageDrawable(mItems.get(position).getIcon());

		return view;
	}
}