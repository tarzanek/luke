package org.getopt.luke;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * LukePlugin is a superclass of any plugin that wants to
 * be loaded automatically, and to work with the current index.
 * 
 * @author abial
 */
public abstract class LukePlugin {
    protected Luke app;
    protected Object myUi;
    protected IndexReader ir;
    protected Directory dir;
    
    /**
     * Set application context. In addition to providing all
     * Thinlet methods for UI manipulation it provides also all
     * public methods of Luke to the plugin.
     * 
     * @param app parent application.
     */
    public void setApplication(Luke app) {
        this.app = app;
    }
    
    /**
     * Get current application context.
     */
    public Luke getApplication() {
        return app;
    }
    
    /**
     * Set a reference to this plugin's UI object (a panel).
     * 
     * @param ui this plugin's UI object
     */
    public void setMyUi(Object ui) {
        myUi = ui;
    }
    
    /**
     * Returns this plugin's UI object.
     */
    public Object getMyUi() {
        return myUi;
    }
    
    /**
     * Set a reference to the IndexReader currently open in
     * the application.
     * 
     * @param ir DirectoryReader for the current index
     */
    public void setReader(IndexReader ir) {
        this.ir = ir;
    }
    
    /**
     * Returns a reference to the IndexReader currently open
     * in the application.
     */
    public IndexReader getReader() {
        return ir;
    }
    
    public void setDirectory(Directory dir) {
        this.dir = dir;
    }
    
    public Directory getDirectory() {
        return dir;
    }
    
    /**
     * Initialize this component. Parent view, this view,
     * directory and index reader should already be initialized.
     * <br>This method will be called repeatedly, whenever new
     * index is loaded into Luke.
     * 
     * @return true on success, false on non-catastrophic failure
     * @throws Exception when an unrecoverable error occurs
     */
    public abstract boolean init() throws Exception;
    
    /**
     * This method should return a fully qualified name/path of
     * the XUL resource used to build the UI for the plugin.
     * The path should follow the rules specified in the
     * ClassLoader documentation fo finding resources.
     * <br>The top level element for the UI should always be a
     * panel. The size of this panel must NOT be specified, but
     * its "halign", "valign" attributes should be set to "fill",
     * and "weightx", "weighty" attributes set to "1" - then the
     * size of the panel will be determined at runtime
     * and will always fill available space for the plugin UI.
     * 
     * @return full path to the XUL resource (filename or URL)
     */
    public abstract String getXULName();
    
    /**
     * Returns a plugin name. NOTE: this should be a short
     * (preferably one word) String, because it's length affects
     * the amount of available screen space.
     * 
     * @return short plugin name
     */
    public abstract String getPluginName();
    
    /** Return short one-line info about the plugin. */
    public abstract String getPluginInfo();
    
    /** Return URL to plugin home page or author's e-mail.
     * NOTE: this MUST be a fully qualified URL, i.e. including
     * the protocol part.
     */
    public abstract String getPluginHome();
}
