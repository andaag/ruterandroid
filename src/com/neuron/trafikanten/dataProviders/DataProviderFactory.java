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

import android.content.res.Resources;
import android.os.Handler;

import com.neuron.trafikanten.dataProviders.IRealtimeProvider.RealtimeProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRoute;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenSearch;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenTransports;

/*
 * Class used for getting the right Search/Realtime/Route etc provider.
 * 
 * IMPORTANT : When using these functions ALWAYS check the result, events when rotating screen may be sent
 * multiple times, and these functions can be called ONCE to get the result (after that memory is cleared).
 */

// TODO : Settings for provider
public class DataProviderFactory {	
	
	/*
	 * Get data provider, this is used in database tables, incase we add more plugins here.
	 * That way station id's etc can be seperated.
	 */
	public static String getDataProviderString() {
		return "Trafikanten";
	}
	/*
	 * Get Search Provider
	 */
	public static ISearchProvider getSearchProvider(Resources resources, Handler handler) {
		return new TrafikantenSearch(resources, handler);

	}
	
	/*
	 * Get Realtime Provider
	 */
	public static IRealtimeProvider getRealtimeProvider(RealtimeProviderHandler handler) {
		return new TrafikantenRealtime(handler);
	}
	
	/*
	 * Get Route Provider
	 */
	public static IRouteProvider getRouteProvider(Resources resources, Handler handler) {
		return new TrafikantenRoute(resources,handler);
	}
	
	/*
	 * Get Transports Provider
	 */
	public static int getImageResource(int transportType) {
		return TrafikantenTransports.getImageResource(transportType);
	}
}
