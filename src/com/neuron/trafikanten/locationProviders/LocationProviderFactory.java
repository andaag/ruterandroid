/**
 *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neuron.trafikanten.locationProviders;

import android.content.Context;
import android.os.Handler;

import com.neuron.trafikanten.locationProviders.skyhook.SkyhookLocation;

// TODO : Settings for locationprovider (including a requirement for accuracy, maybe an option to keep scanning and have a refresh button)
public class LocationProviderFactory {
	public static final int SETTING_LOCATION_ACCURACY = 5; // Needed accuracy for auto continue when waiting for a fix.	
	/*
	 * Gets all data providers, this is IN ORDER, .get[0] == PROVIDER_<.get[0]>
	 */
	public static String[] getLocationProviders() {
		return new String[] { "Skyhook", "Android" };
	}
	
	/*
	 * Location information:
	 */
	private static double _longitude = 0;
	private static double _latitude = 0;
	private static double _accuracy = 0;
	public static void setLocation(double latitude, double longitude, double accuracy) {
		_longitude = longitude;
		_latitude = latitude;
		_accuracy = accuracy;
	}
	
	public static double[] getLocation() {
		return new double[] { _latitude,  _longitude, _accuracy };
	}
	
	/*
	 * Get Search Provider
	 */
	public static ILocationProvider getLocationProvider(Context context, Handler handler) {
		return new SkyhookLocation(context, handler);
	}
	
	
}
