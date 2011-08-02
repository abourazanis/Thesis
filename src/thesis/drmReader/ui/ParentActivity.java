package thesis.drmReader.ui;

public interface ParentActivity {
	
	 void displayDialog(int dialogID);
	 void hideDialog(int dialogID);
	 
	 void downloadDocument(BookLink document);

}
