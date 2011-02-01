package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;

import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;

public class PlatformRenderer extends GenericRealtimeRenderer {
	public String platform = null;
	
	public PlatformRenderer(String platform) {
		super(GenericRealtimeList.RENDERER_PLATFORM);
		this.platform = platform;
	}
	
	public PlatformRenderer(Parcel in) {
		super(GenericRealtimeList.RENDERER_PLATFORM);
		this.platform = in.readString();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(platform);
	}
}
