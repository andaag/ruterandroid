package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;

import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeListAdapter;

public class RealtimeRenderer extends GenericRealtimeRenderer  {
	public RealtimeData data;
	public RealtimeRenderer(RealtimeData data) {
		super(GenericRealtimeListAdapter.RENDERER_REALTIME);
		this.data = data;
	}
	
	public RealtimeRenderer(Parcel in) {
		super(GenericRealtimeListAdapter.RENDERER_REALTIME);
		this.data = in.readParcelable(RealtimeData.class.getClassLoader());
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(data, 0);
	}
	

}