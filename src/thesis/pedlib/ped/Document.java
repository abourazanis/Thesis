package thesis.pedlib.ped;

public class Document {
	
	private String pedPath;
	private Metadata metaData;
	private TOC toc;
	private String containerPath;
	
	private PedParser pedParser;
	
	Document(String pedPath){
		this.pedPath = pedPath;
		
		pedParser = new PedParser(this);
		pedParser.read();
		
	}
	
	String getPedPath(){
		return pedPath;
	}

	void setMetadata(Metadata metadata){
		this.metaData = metadata;
	}
	
	String getDocumentTitle(){
		return this.metaData.getTitle();
	}
	
	String getContainerPath(){
		return containerPath;
	}
	
	void setContainerPath(String containerPath){
		this.containerPath = containerPath;
	}
	
	void setTOC(TOC toc){
		this.toc = toc;
	}
	
	TOC getTOC(){
		return toc;
	}
}
