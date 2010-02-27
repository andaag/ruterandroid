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
		/*final String stopName = station.stopName.toLowerCase();
		if (stopName.contains("tog")) {
			return R.drawable.icon32_line_train;
		} else if (stopName.contains("t-bane")) {
			return R.drawable.icon32_line_tram;
		} else if (stopName.contains("buss")) {
			return R.drawable.icon32_line_bus;
		}*/
		
		//http://labs.trafikanten.no/ofte-stilte-sporsmal/#124
		try {
			/*
			 * Parse numeric line
			 */
			final Integer lineI = Integer.parseInt(line);
			if (lineI >= 1 && lineI <= 9) {
				return R.drawable.icon32_line_underground;				
			} else if (lineI >= 11 && lineI <= 19) {
				return R.drawable.icon32_line_tram;
			} else if (lineI == 300 || lineI == 400 || lineI == 440 || lineI == 450 || lineI == 460 || lineI == 500 || lineI == 550 || lineI == 560) {
				return R.drawable.icon32_line_train;
			} else if ((lineI >= 91 && lineI <= 94) || lineI == 601 || lineI == 602 || lineI ==  716) {
				return R.drawable.icon32_line_boat;
			}
		} catch (NumberFormatException e) {
			// if we can't parse we default to bus
		}
		/*
		 * Parse string lines
		 */
		if (line.equals("R01") || line.equals("R20") || line.equals("R21") || line.equals("R22") || line.equals("R25") || line.equals("R41") || line.equals("R50") || line.equals("R51") || line.equals("FT")) {
			return R.drawable.icon32_line_train;
		}
		
		// Default to bus.
		return R.drawable.icon32_line_bus;
	}
}
