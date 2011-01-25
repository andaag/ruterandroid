package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;

import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;

public class StationRenderer extends GenericRealtimeRenderer  {
	public StationData station;
	public StationRenderer(StationData station) {
		super(GenericRealtimeList.RENDERER_STATION);
		this.station = station;
	}
	
	public StationRenderer(Parcel in) {
		super(GenericRealtimeList.RENDERER_STATION);
		this.station = in.readParcelable(StationData.class.getClassLoader());
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(station, 0);
	}
}