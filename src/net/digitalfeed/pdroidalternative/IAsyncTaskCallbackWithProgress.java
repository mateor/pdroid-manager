package net.digitalfeed.pdroidalternative;

public interface IAsyncTaskCallbackWithProgress<ReturnType> {
	abstract void asyncTaskComplete(ReturnType result);
	abstract void asyncTaskProgressUpdate(Integer... progress);
}