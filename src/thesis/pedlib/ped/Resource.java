package thesis.pedlib.ped;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	
	public Resource(String id, String title){
		this(id,title,null,null,new byte[0]);
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
		this(null,null,href, MediaTypeService.determineType(href),toByteArray(in));
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
		this.mediaType = MediaTypeService.determineType(href);
	}
	
	public Reader getReader() throws IOException {
		return new UnicodeReader(new ByteArrayInputStream(data), "UTF-8");
	}
	
	private static byte[] toByteArray(InputStream in) throws IOException{
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
        }
        
		return out.toByteArray();

		
		/*
		InputStreamReader ir = new InputStreamReader(in);
		char[] buffer = new char[1024];
		StringBuilder builder = new StringBuilder();
		while (ir.read(buffer, 0, buffer.length) != -1){
			builder.append(buffer.toString());
		}
		
		return builder.toString().getBytes();
		*/
	}


}
