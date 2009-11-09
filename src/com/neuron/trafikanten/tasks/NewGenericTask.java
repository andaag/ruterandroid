package com.neuron.trafikanten.tasks;

import android.app.Dialog;

// TODO : Make IGenericTask
public interface NewGenericTask {
	public abstract void onPrepareDialog(int id, Dialog dialog);
	public abstract Dialog onCreateDialog(int id);
	
	public abstract void Stop();
}
