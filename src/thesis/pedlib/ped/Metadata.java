package thesis.pedlib.ped;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

public class Metadata {
	
	private String title;
	private String language;
	private String description;
	private String subject;
	private String date;
	private List<String> authors;
	private List<QName> otherMeta;
	private Resource coverImage;
	
	public Metadata(){
		this(null,null);
	}
	
	public Metadata(String title,String language, String description, String subject, String date, List<String> authors, List<QName> otherMeta){
		this.title = title;
		this.language = language;
		this.description = description;
		this.subject = subject;
		this.date = date;
		this.authors = authors;
		this.otherMeta = otherMeta;
	}
	
	public Metadata(String title,String language){
		this.title = title;
		this.language = language;
		this.authors = new ArrayList<String>();
		this.otherMeta = new ArrayList<QName>();
	}
	
	public String getTitle(){
		return title;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public String getLanguage(){
		return language;
	}
	
	public void setLanguage(String language){
		this.language = language;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public void setSubject(String subject){
		this.subject = subject;
	}
	
	public String getDescription(){
		return description;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	public String getDate(){
		return date;
	}
	
	public void setDate(String date){
		this.date = date;
	}
	
	public List<String> getAuthors(){
		return authors;
	}
	
	public List<QName> getOtherMeta(){
		return otherMeta;
	}
	
	public void addAuthor(String name){
		authors.add(name);
	}
	
	public void addOtherMeta(String name, String value){
		QName meta = new QName(name,value);
		otherMeta.add(meta);
	}
	
	public void addOtherMeta(QName metadata){
		otherMeta.add(metadata);
	}
		
	public Resource getCoverImage() {
		return coverImage;
	}
	public void setCoverImage(Resource coverImage) {
		this.coverImage = coverImage;
	}

}
