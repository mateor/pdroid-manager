package net.digitalfeed.pdroidalternative;

public interface IAsyncTaskCallback<ReturnType> {
	abstract void asyncTaskComplete(ReturnType param);
}