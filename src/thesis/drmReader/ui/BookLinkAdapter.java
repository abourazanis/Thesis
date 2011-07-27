package thesis.drmReader.ui;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;

import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import thesis.drmReader.R;
import thesis.drmReader.R.id;
import thesis.drmReader.R.layout;
import thesis.drmReader.db.EpubDbAdapter;
import thesis.imageLazyLoader.ImageLoader;
import thesis.sec.Decrypter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BookLinkAdapter extends ArrayAdapter<BookLink> {

	private final ArrayList<BookLink> documents;
	public ImageLoader imageLoader;

	public BookLinkAdapter(Context context, int textViewResourceId,
			ArrayList<BookLink> items) {
		super(context, textViewResourceId, items);
		documents = items;
		imageLoader = new ImageLoader(context);
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

		BookLink doc = documents.get(position);
		if (doc != null) {
			Metadata meta = doc.getMeta();
			if (meta.getCoverImage() != null) {
				//holder.imageView.setTag(doc.getId() + doc.getCoverUrl());
				try {
					EpubDbAdapter dbAdapter = new EpubDbAdapter(
							this.getContext()).open();
					String epubFilePath = dbAdapter
							.getEpubLocation(doc.getId());
					dbAdapter.close();
					if (epubFilePath != null) {
						Decrypter decrypter = new Decrypter(epubFilePath,
								this.getContext());

						Resource coverResource = decrypter.decrypt(meta
								.getCoverImage());

						imageLoader.DisplayImage(doc.getId() + doc.getCoverUrl(),
								coverResource.getInputStream(),
								holder.imageView);
					}
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				//holder.imageView.setTag(doc.getCoverUrl());
				imageLoader.DisplayImage(doc.getCoverUrl(), holder.imageView);
			}

			if (!meta.getSubjects().isEmpty())
				holder.textViewTop.setText(meta.getFirstTitle());
			if (!meta.getAuthors().isEmpty())
				holder.textViewBottom.setText(meta.getAuthors().get(0)
						.toString());
		}

		return view;
	}

}
