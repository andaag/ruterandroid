package com.neuron.trafikanten.dataProviders;

import android.os.Handler;
import android.os.Message;

import com.neuron.trafikanten.dataProviders.IGenericProvider.GenericProviderHandlerNew;

public abstract class GenericDataProviderThread<T> extends Thread {
	private final static int MSG_DATA = 0;
	private final static int MSG_POSTEXECUTE = 1;
	
	private final GenericProviderHandlerNew<T> handler;
	private final Handler threadHandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if (isInterrupted()) return;
			switch (msg.what) {
			case MSG_DATA:
				handler.onData( (T) msg.obj);
				break;
			case MSG_POSTEXECUTE:
				handler.onPostExecute((Exception) msg.obj);
				break;
			}
		}
	};
	
	public GenericDataProviderThread(GenericProviderHandlerNew<T> handler) {
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
