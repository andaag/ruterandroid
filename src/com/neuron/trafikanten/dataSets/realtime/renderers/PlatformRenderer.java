package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;

import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeListAdapter;

public class PlatformRenderer extends GenericRealtimeRenderer {
	public int platform = 0;
	
	public PlatformRenderer(int platform) {
		super(GenericRealtimeListAdapter.RENDERER_PLATFORM);
		this.platform = platform;
	}
	
	public PlatformRenderer(Parcel in) {
		super(GenericRealtimeListAdapter.RENDERER_PLATFORM);
		this.platform = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(platform);
	}
}
