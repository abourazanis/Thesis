package thesis.pedlib.ped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import thesis.pedlib.util.MediaType;
import thesis.pedlib.util.MediaTypeService;

import com.google.gdata.util.io.base.UnicodeReader;

public class Resource {
	private String id;
	private String title;
	private String href;
	private MediaType mediaType;
	private byte[] data;
	
	public Resource(byte[] data, MediaType type){
		this(null,null,null,type,data);
	}
	
	public Resource(byte[] data, String href, MediaType type){
		this(null,null,href,type,data);
	}
	
	public Resource(String id, String title, String href){
		this(id,title,href,MediaTypeService.determineType(href),new byte[0]);
	}
	
	public Resource(String id, String title, String href, MediaType type, byte[] data ){
		this.id = id;
		this.title = title;
		this.href = href;
		this.mediaType = type;
		this.data = data;
	}
	
	public Resource(InputStream in, String href) throws IOException {
		this(null,null,href, MediaTypeService.determineType(href),in.toString().getBytes());
	}
	
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(data);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	
	public MediaType getMediaType() {
		return mediaType;
	}
	
	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}
	
	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
	
	public Reader getReader() throws IOException {
		return new UnicodeReader(new ByteArrayInputStream(data), "UTF-8");
	}


}
