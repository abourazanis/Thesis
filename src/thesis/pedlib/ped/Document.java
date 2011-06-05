package thesis.pedlib.ped;

public class Document {
	
	private String pedPath;
	private Metadata metaData;
	private TOC toc;
	private String containerPath;
	private Resources resources;
	private Resource docResource;
	
	
	//TODO:
	//remove all pedPath relevant code
	//maybe and the containerPath
	
	public Document(){
	}
	
	public Document(String pedPath){
		this.pedPath = pedPath;
	}
	
	public String getPedPath(){
		return pedPath;
	}
	
	public void setPedPath(String path){
		pedPath = path;
	}

	public void setMetadata(Metadata metadata){
		this.metaData = metadata;
	}
	
	public String getDocumentTitle(){
		return this.metaData.getTitle();
	}
	
	public String getDocumentDescription(){
		return this.metaData.getDescription();
	}
	
	public String getContainerPath(){
		return containerPath;
	}
	
	public void setContainerPath(String containerPath){
		this.containerPath = containerPath;
	}
	
	public void setTOC(TOC toc){
		this.toc = toc;
	}
	
	public TOC getTOC(){
		return toc;
	}


	public Resources getResources() {
		return resources;
	}

	public void setResources(Resources resources) {
		this.resources = resources;
	}
	
	public Resource getDocResource() {
		return docResource;
	}

	public void setDocResource(Resource docResource) {
		this.docResource = docResource;
	}
	
	public Resource getCoverImage() {
		return metaData.getCoverImage();
	}

	public void setCoverImage(Resource coverImage) {
		if (coverImage == null) {
			return;
		}
		if (resources != null){
			if(! resources.containsByHref(coverImage.getHref())) {
				resources.add(coverImage);
			}
		}
		if(metaData != null)	
			metaData.setCoverImage(coverImage);
	}

}
