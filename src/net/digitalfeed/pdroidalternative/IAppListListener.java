package net.digitalfeed.pdroidalternative;


public interface IAppListListener {
	abstract void appListLoadCompleted(Application [] appList);
	abstract void appListProgressUpdate(Integer... progress);
}