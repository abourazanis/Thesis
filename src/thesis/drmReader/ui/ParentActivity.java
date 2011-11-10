
package thesis.drmReader.ui;

/**
 * interface between FragmentActivities and fragments to enable fragments to
 * call FragmentActivity's methods.
 * 
 * @author A.Bourazanis
 */
public interface ParentActivity {

    void displayDialog(int dialogID);

    void hideDialog(int dialogID);

    void downloadDocument(BookLink document);

    int getActiveFragmentIndex();

}
