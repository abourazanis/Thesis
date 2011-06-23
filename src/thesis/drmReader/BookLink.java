package thesis.drmReader;

import nl.siegmann.epublib.domain.Metadata;



public class BookLink {
	private Metadata meta;
	private String id;

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

	

	
}
