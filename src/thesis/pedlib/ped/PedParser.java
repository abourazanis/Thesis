package thesis.pedlib.ped;

public class PedParser {
	
	private Document doc;
	
	private MetadataReader metaReader;
	private TOCParser tocParser;
	
	PedParser(Document document){
		this.doc = document;
		metaReader = new MetadataReader(doc.getPedPath());
		tocParser = new TOCParser(doc);
		
		
	}
	
	void read(){
		
		//meta parsing
		if(metaReader.read()){
			doc.setMetadata(metaReader.getMetadata());
			doc.setContainerPath(metaReader.getContainerPath());
			
			doc.setTOC(tocParser.parse());
		}
		else{
			//handle parsing error somehow
		}
	}

}
