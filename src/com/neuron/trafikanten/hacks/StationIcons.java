package com.neuron.trafikanten.hacks;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.StationData;

/*
 * Station icons are currently not sent by the realtime api.
 * This is code to parse input and try to figure out what icons to use.
 * 
 * See also HACK_STATIONICONS in RealtimeView.java
 */

public class StationIcons {
	public static int hackGetLineIcon(StationData station, String line) {
		final String stopName = station.stopName.toLowerCase();
		if (stopName.contains("tog")) {
			return R.drawable.icon_train;
		} else if (stopName.contains("t-bane")) {
			return R.drawable.icon_tram;
		} else if (stopName.contains("buss")) {
			return R.drawable.icon_bus;
		}
		
		final Integer lineI = Integer.parseInt(line);
		if (lineI != null) {
			if (lineI <= 6) {
				return R.drawable.icon_subway;
			} else if (lineI <= 19) {
				return R.drawable.icon_tram;
			}
		}
		
		// Default to bus.
		return R.drawable.icon_bus;
	}
}
