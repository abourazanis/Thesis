package thesis.pedlib.ped;

import java.util.ArrayList;
import java.util.List;

public class TOCEntry implements Comparable<TOCEntry> {
	private String id;
	private int order;
	private String src;
	private String label;
	private List<TOCEntry> childs;
	
	TOCEntry(String id, int order){
		this(id,order,null,null);
	}
	
	TOCEntry(int order,String src, String label){
		this(null,order,src,label,new ArrayList<TOCEntry>());
	}

	TOCEntry(String id,int order,String src, String label){
		this(id,order,src,label,new ArrayList<TOCEntry>());
	}
	
	TOCEntry(String id,int order,String src, String label,ArrayList<TOCEntry> childs){
		this.id = id;
		this.order = order;
		this.src = src;
		this.label = label;
		this.childs = childs;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public String getId(){
		return id;
	}
	
	public void setOrder(int order){
		this.order = order;
	}
	
	public int getOrder(){
		return order;
	}
	
	public void setSrc(String src){
		this.src = src;
	}
	
	public String getSrc(){
		return src;
	}
	
	public void setLabel(String label){
		this.label = label;
	}
	
	public String getLabel(){
		return label;
	}
	
	public List<TOCEntry> getChilds(){
		return childs;
	}
	
	public List<TOCEntry> addChild(TOCEntry child){
		if(child != null)
			childs.add(child);
		return childs;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TOCEntry another) {
		if(another == null) return 1;
		return another.order > this.order? 0:1;
		
	}
}
