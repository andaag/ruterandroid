package com.neuron.trafikanten.dataSets.realtime.renderers;

import android.os.Parcel;

import com.neuron.trafikanten.dataSets.RealtimeData;
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
	
	public int _platformNumber = -1;
	public int getPlatformNumber() {
		if (_platformNumber == -1) {
			try {
			_platformNumber = Integer.parseInt(platform);
			} catch (NumberFormatException e) {
				_platformNumber = 0;
			}
		}
		return _platformNumber;
	}
	
	public int compareTo(RealtimeData data) {
		/*
		 * Sorting based on empty platform
		 */
		if (platform.length() == 0 && data.departurePlatform.length() > 0) {
			return 1;
		}
		if (platform.length() > 0 && data.departurePlatform.length() == 0) {
			return -1;
		}
		if (platform.length() == 0 && data.departurePlatform.length() == 0) {
			return 0;
		}
		/*
		 * Sorting based on numerical platform
		 */
		final int platNum = getPlatformNumber();
		final int departurePlatNum = data.getPlatformNumber();
		if (platNum > 0 && departurePlatNum > 0) {
			if (platNum > departurePlatNum) {
				//Log.i("DEBUG CODE","XReturning sorting for " + platNum + " <=> " + departurePlatNum + " = " + 1);
				return 1;
			}
			if (platNum < departurePlatNum) {
				//Log.i("DEBUG CODE","XReturning sorting for " + platNum + " <=> " + departurePlatNum + " = " + -1);
				return -1;
			}
			//Log.i("DEBUG CODE","XReturning sorting for " + platNum + " <=> " + departurePlatNum + " = " + 0);
			return 0;
		}
		
		final int i = platform.compareTo(data.departurePlatform);
		//Log.i("DEBUG CODE","Returning sorting for " + platform + " <=> " + data.departurePlatform + " = " + i);
		return i;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(platform);
	}
}
