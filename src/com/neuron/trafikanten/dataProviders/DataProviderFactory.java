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

package com.neuron.trafikanten.dataProviders;

import com.neuron.trafikanten.MySettings;
import com.neuron.trafikanten.dataProviders.trafikanten.Trafikanten;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRoute;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenSearch;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenTransports;

import android.os.Handler;

/*
 * Class used for getting the right Search/Realtime/Route etc provider.
 * 
 * IMPORTANT : When using these functions ALWAYS check the result, events when rotating screen may be sent
 * multiple times, and these functions can be called ONCE to get the result (after that memory is cleared).
 */

// TODO : Settings for provider
public class DataProviderFactory {
	/*
	 * Gets all data providers
	 */
	public static String[] getDataProviders() {
		return new String[] { "Trafikanten" };
	}
	
	/*
	 * Get data provider string
	 */
	public static String getDataProviderString() {
		return getDataProviders()[MySettings.SETTING_DATAPROVIDER - 1];
	}
	
	/*
	 * Get Search Provider
	 */
	public static ISearchProvider getSearchProvider(Handler handler) {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return new TrafikantenSearch(handler);
		}
		return null;
	}
	
	/*
	 * Get Realtime Provider
	 */
	public static IRealtimeProvider getRealtimeProvider(Handler handler) {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return new TrafikantenRealtime(handler);
		}
		return null;
	}
	
	/*
	 * Get Route Provider
	 */
	public static IRouteProvider getRouteProvider(Handler handler) {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return new TrafikantenRoute(handler);
		}
		return null;
	}
	
	/*
	 * Get Transports Provider
	 */
	public static int getImageResource(int transportType) {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return TrafikantenTransports.getImageResource(transportType);
		}
		return 0;
	}
}
