package thesis.pedlib.ped;

import java.util.ArrayList;
import java.util.List;



public class TOCEntry implements Comparable<TOCEntry> {
	private Resource resource;
	private int order;
	private List<TOCEntry> childs;
	
	TOCEntry(String id, int order){
		this(id,order,null,new ArrayList<TOCEntry>());
	}
	
	TOCEntry(int order,String src, String label){
		this(null,order,src,label,new ArrayList<TOCEntry>());
	}

	TOCEntry(String id,int order,String src, String label){
		this(id,order,src,label,new ArrayList<TOCEntry>());
	}
	
	TOCEntry(String id,int order, String label,ArrayList<TOCEntry> childs){
		this.resource = new Resource(id, label);
		this.order = order;
		this.childs = childs;
	}
	
	TOCEntry(String id,int order,String src, String label,ArrayList<TOCEntry> childs){
		this.resource = new Resource(id, label, src);
		this.order = order;
		this.childs = childs;
	}
	
	public void setId(String id){
		this.resource.setId(id);
	}
	
	public String getId(){
		return this.resource.getId();
	}
	
	public void setOrder(int order){
		this.order = order;
	}
	
	public int getOrder(){
		return order;
	}
	
	public void setSrc(String src){
		this.resource.setHref(src);
	}
	
	public String getSrc(){
		return this.resource.getHref();
	}
	
	public void setLabel(String label){
		this.resource.setTitle(label);
	}
	
	public String getLabel(){
		return resource.getTitle();
	}
	
	public List<TOCEntry> getChilds(){
		return childs;
	}
	
	public List<TOCEntry> addChild(TOCEntry child){
		if(child != null)
			childs.add(child);
		return childs;
	}

	
	@Override
	public int compareTo(TOCEntry another) {
		if(another == null) return 1;
		return another.order > this.order? 0:1;
		
	}
}
