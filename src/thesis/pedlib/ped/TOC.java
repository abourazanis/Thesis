package thesis.pedlib.ped;

import java.util.ArrayList;
import java.util.List;

public class TOC {
	
	private List<TOCEntry> items;

	TOC(){
		items = new ArrayList<TOCEntry>();
	}
	
	TOC(List<TOCEntry> items){
		this.items = items;
	}
	
	public List<TOCEntry> getItems(){
		return items;
	}
	
	public void setItems(List<TOCEntry> items){
		this.items = items;
	}
	
	public List<TOCEntry> addItem(TOCEntry item){
		if(item != null)
			items.add(item);
		return items;
	}
}
