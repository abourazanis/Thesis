package thesis.drmReader;

public class Document {

	private String docTitle;
	private String docImageSrc;
	private String docDetails;
	
	
	Document(String documentTitle, String documentImageSrc, String documentDetails){
		docTitle = documentTitle;
		docImageSrc = documentImageSrc;
		docDetails = documentDetails;
	}
	
	public String getDocumentTitle(){
		return docTitle;
	}
	
	public void setDocumentTitle(String title){
		docTitle = title;
	}
	
	public String getDocumentImage(){
		return docImageSrc;
	}
	
	public void setDocumentImage(String imagePath){
		docImageSrc = imagePath;
	}
	
	public String getDocumentDetails(){
		return docDetails;
	}
	
	public void setDocumentDetails(String details){
		docDetails = details;
	}
}
