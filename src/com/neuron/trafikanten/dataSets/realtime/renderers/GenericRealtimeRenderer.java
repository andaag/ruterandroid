package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;


public abstract class GenericRealtimeRenderer {
	public int renderType = -1;
	
	public GenericRealtimeRenderer(int renderType) {
		this.renderType = renderType;
	}
	
	public abstract void writeToParcel(Parcel out, int flags);

}