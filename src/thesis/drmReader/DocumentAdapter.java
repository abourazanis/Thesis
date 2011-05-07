package thesis.drmReader;

import java.io.BufferedInputStream;
import java.util.ArrayList;

import thesis.pedlib.ped.Document;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DocumentAdapter extends ArrayAdapter<Document> {

	private final ArrayList<Document> documents;

	public DocumentAdapter(Context context, int textViewResourceId,
			ArrayList<Document> items) {
		super(context, textViewResourceId, items);
		documents = items;
	}

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	static class ViewHolder {
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
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.list_item, null);

			holder = new ViewHolder();
			holder.imageView = (ImageView) view.findViewById(R.id.icon);
			holder.textViewBottom = (TextView) view
					.findViewById(R.id.bottomtext);
			holder.textViewTop = (TextView) view.findViewById(R.id.toptext);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		Document doc = documents.get(position);
		if (doc != null) {
			try {
				//TODO: try to make it run on a separate thread 
				BufferedInputStream buf = new BufferedInputStream(doc
						.getCoverImage().getInputStream());
				Bitmap bm = BitmapFactory.decodeStream(buf);
				
				int width = bm.getWidth();
				int height = bm.getHeight();
				int newWidth = 30;
				int newHeight = 40;
				float scaleWidth = ((float) newWidth) / width;
				float scaleHeight = ((float) newHeight) / height;
				// createa matrix for the manipulation
				Matrix matrix = new Matrix();
				// resize the bit map
				matrix.postScale(scaleWidth, scaleHeight);
				Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0,
						width, height, matrix, true);

				// make a Drawable from Bitmap to allow to set the BitMap
				// to the ImageView, ImageButton or what ever
				BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);
				
				holder.imageView.setImageDrawable(bmd);
				buf.close();
			} catch (Exception e) {
				//
			}
			holder.textViewBottom.setText(doc.getDocumentDescription());
			holder.textViewTop.setText(doc.getDocumentTitle());
		}

		return view;
	}

}
