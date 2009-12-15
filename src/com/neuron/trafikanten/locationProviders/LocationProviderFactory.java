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

import com.neuron.trafikanten.locationProviders.ILocationProvider.LocationProviderHandler;
import com.neuron.trafikanten.locationProviders.skyhook.SkyhookLocation;

public class LocationProviderFactory {
	public static final int SETTING_LOCATION_ACCURACY = 80; // Needed accuracy for auto continue when waiting for a fix.	
	
	/*
	 * Get Search Provider
	 */
	public static ILocationProvider getLocationProvider(Context context, LocationProviderHandler handler) {
		return new SkyhookLocation(context, handler);
	}
}
