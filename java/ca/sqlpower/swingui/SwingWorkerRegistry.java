package ca.sqlpower.swingui;

/**
 * Classes that implement this interface shall keep track of SPSwingWorkers
 * registered with it. The main use case for this is to allow application
 * sessions to cancel running SPSwingWorker threads when they close. 
 */
public interface SwingWorkerRegistry {
    /**
     * Makes the session aware of the given ArchitectSwingWorker instance.
     * When the session dies, it can then tell the ArchitectSwingWorker
     * instances it keeps track of to stop running. 
     */
    public void registerSwingWorker(SPSwingWorker worker);

    /**
     * Removes knowledge of this ArchitectSwingWorker from this session.
     * This should only happen when the ArchitectSwingWorker is finished
     * before the session is closed.
     */
    public void removeSwingWorker(SPSwingWorker worker); 
}
