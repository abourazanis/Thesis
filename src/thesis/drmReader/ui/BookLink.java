package thesis.drmReader.ui;

import thesis.drmReader.util.Constants;
import nl.siegmann.epublib.domain.Metadata;


/**
 * Class that stores information about epub items in lists
 * @author tas0s
 *
 */
public class BookLink {
	private Metadata meta;
	private String id;
	private String coverUrl;

	public BookLink() {
		this(null, null);
	}
	
	public BookLink(Metadata meta){
		this(meta,null);
	}

	public BookLink(Metadata meta,String id) {
		this.meta = meta;
		this.id = id;
	}

	public Metadata getMeta() {
		return meta;
	}

	public void setMeta(Metadata meta) {
		this.meta = meta;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCoverUrl() {
		return Constants.COVERS_URL + coverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		this.coverUrl = coverUrl;
	}

	

	
}
