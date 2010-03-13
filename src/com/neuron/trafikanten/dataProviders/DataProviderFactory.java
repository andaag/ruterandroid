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

import com.neuron.trafikanten.dataProviders.IRouteProvider.RouteProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRoute;

/*
 * Class used for getting the right Search/Realtime/Route etc provider.
 * 
 */

public class DataProviderFactory {	
	
	/*
	 * Get data provider, this is used in database tables, incase we add more plugins here.
	 * That way station id's etc can be seperated.
	 */
	public static String getDataProviderString() {
		return "Trafikanten";
	}
	
	/*
	 * Get Route Provider
	 */
	public static IRouteProvider getRouteProvider(Resources resources, RouteProviderHandler handler) {
		return new TrafikantenRoute(resources,handler);
	}
}
