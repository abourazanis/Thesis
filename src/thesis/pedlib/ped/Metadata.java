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
	
	Metadata(String title,String language, String description, String subject, String date, List<String> authors, List<QName> otherMeta){
		this.title = title;
		this.language = language;
		this.description = description;
		this.subject = subject;
		this.date = date;
		this.authors = authors;
		this.otherMeta = otherMeta;
	}
	
	Metadata(String title,String language){
		this.title = title;
		this.language = language;
		this.authors = new ArrayList<String>();
		this.otherMeta = new ArrayList<QName>();
	}
	
	String getTitle(){
		return title;
	}
	
	void setTitle(String title){
		this.title = title;
	}
	
	String getLanguage(){
		return language;
	}
	
	void setLanguage(String language){
		this.language = language;
	}
	
	String getSubject(){
		return subject;
	}
	
	void setSubject(String subject){
		this.subject = subject;
	}
	
	String getDescription(){
		return description;
	}
	
	void setDescription(String description){
		this.description = description;
	}
	
	String getDate(){
		return date;
	}
	
	void setDate(String date){
		this.date = date;
	}
	
	List<String> getAuthors(){
		return authors;
	}
	
	List<QName> getOtherMeta(){
		return otherMeta;
	}
	
	void addAuthor(String name){
		authors.add(name);
	}
	
	void addOtherMeta(String name, String value){
		QName meta = new QName(name,value);
		otherMeta.add(meta);
	}
	
	void addOtherMeta(QName metadata){
		otherMeta.add(metadata);
	}

}
