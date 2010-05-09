package com.neuron.trafikanten.dataProviders;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Message;

public abstract class GenericDataProviderThread<T> extends Thread {
	private final static int MSG_DATA = 0;
	private final static int MSG_POSTEXECUTE = 1;
	private AtomicBoolean stopped = new AtomicBoolean(false);
	
	private final IGenericProviderHandler<T> handler;
	public final Handler threadHandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if (stopped.get()) return;
			switch (msg.what) {
			case MSG_DATA:
				handler.onData( (T) msg.obj);
				break;
			case MSG_POSTEXECUTE:
				handler.onPostExecute((Exception) msg.obj);
				break;
			default:
				handler.onExtra(msg.what, msg.obj);
				break;
			}
		}
	};
	
	public void kill() {
		stopped.set(true);
		interrupt();
	}
	
	public GenericDataProviderThread(IGenericProviderHandler<T> handler) {
		this.handler = handler;
		handler.onPreExecute();
	}
	   
    /*
     * Can be used from a thread:
     */

    public void ThreadHandlePostExecute(Exception e) {
    	Message msg = threadHandler.obtainMessage(MSG_POSTEXECUTE);
    	msg.obj = e;
    	threadHandler.sendMessage(msg);
    }
    public void ThreadHandlePostData(T data) {
    	Message msg = threadHandler.obtainMessage(MSG_DATA);
    	msg.obj = data;
    	threadHandler.sendMessage(msg);
    }
}
