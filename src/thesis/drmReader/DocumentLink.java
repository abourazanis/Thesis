package thesis.drmReader;

import thesis.pedlib.ped.Resource;

public class DocumentLink {
	private String title;
	private String author;
	private String subject;
	private String coverUrl;
	private Resource coverResource;
	private String id;

	public DocumentLink() {
		this(null, null, null);
	}

	public DocumentLink(String title, String author, String subject) {
		this(title, author, subject, null, null);
	}

	public DocumentLink(String title, String author, String subject,
			String coverUrl, String id) {
		this.title = title;
		this.author = author;
		this.subject = subject;
		this.coverUrl = coverUrl;
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getCoverUrl() {
		return coverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		this.coverUrl = coverUrl;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Resource getCoverResource() {
		return coverResource;
	}

	public void setCoverResource(Resource coverResource) {
		this.coverResource = coverResource;
	}

}
