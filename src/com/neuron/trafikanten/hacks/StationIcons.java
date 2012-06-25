package com.neuron.trafikanten.hacks;

import com.neuron.trafikanten.R;

/*
 * DEPRECATED
 * This is currently only used by the maps.
 * This is due to realtime api previously not sending vehiclemode.
 */

public class StationIcons {
	public static int hackGetLineIcon(String line) {
		//http://labs.trafikanten.no/ofte-stilte-sporsmal/#124
		try {
			/*
			 * Parse numeric line
			 */
			final Integer lineI = Integer.parseInt(line);
			if (lineI >= 1 && lineI <= 9) {
				return R.drawable.icon_line_underground;				
			} else if (lineI >= 11 && lineI <= 19) {
				return R.drawable.icon_line_tram;
			} else if (lineI == 300 || lineI == 400 || lineI == 440 || lineI == 450 || lineI == 460 || lineI == 500 || lineI == 550 || lineI == 560) {
				return R.drawable.icon_line_train;
			} else if ((lineI >= 91 && lineI <= 94) || lineI == 256 || lineI == 601 || lineI == 602 || lineI ==  716) {
				return R.drawable.icon_line_boat;
			}
		} catch (NumberFormatException e) {
			// if we can't parse we default to bus
		}
		/*
		 * Parse string lines
		 */
		if (line.equals("R01") || line.equals("R04") || line.equals("R20") || line.equals("R21") || line.equals("R22") || line.equals("R25") || line.equals("R41") || line.equals("R50") || line.equals("R51") || line.equals("FT")) {
			return R.drawable.icon_line_train;
		}
		
		// Default to bus.
		return R.drawable.icon_line_bus;
	}
	
	/*
	 * This is for the map, and is guestimated.
	 */
	public static int hackGetStationIcon(String stopNameX) {
		final String stopName = stopNameX.toLowerCase();
		if (stopName.contains("tog")) {
			return R.drawable.icon_line_train;
		} else if (stopName.contains("t-bane")) {
			return R.drawable.icon_line_underground;
		}  else if (stopName.contains("båt")) {
			return R.drawable.icon_line_boat;
		}
		return R.drawable.icon_line_bus;
	}
	
	/*
	 * converts to black station icons
	 */
	public static int getBlackStationIcons(int icon) {
		if (icon == R.drawable.icon_line_boat) {
			return R.drawable.icon_line_boat_black;
		} else if (icon == R.drawable.icon_line_bus) {
			return R.drawable.icon_line_bus_black;
		} else if (icon == R.drawable.icon_line_train) {
			return R.drawable.icon_line_train_black;
		} else if (icon == R.drawable.icon_line_tram) {
			return R.drawable.icon_line_tram_black;
		} else if (icon == R.drawable.icon_line_underground) {
			return R.drawable.icon_line_underground_black;
		} else if (icon == R.drawable.icon_line_walk) {
			return R.drawable.icon_line_walk_black;
		}
		return 0;
	}
}
