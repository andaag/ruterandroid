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

import java.util.ArrayList;

import com.neuron.trafikanten.MySettings;
import com.neuron.trafikanten.dataProviders.trafikanten.Trafikanten;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRoute;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenSearch;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.SearchStationData;

public class ResultsProviderFactory {
	
	/*
	 * Search data
	 */
	public static ArrayList<SearchStationData> GetSearchResults() {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return TrafikantenSearch.GetStationList();
		}
		return null;
	}
	
	/*
	 * Realtime data
	 */
	public static ArrayList<RealtimeData> getRealtimeResults() {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return TrafikantenRealtime.GetRealtimeList();
		}
		return null;
	}
	
	/*
	 * Route data
	 */
	public static ArrayList<RouteData> getRouteResults() {
		if (MySettings.SETTING_DATAPROVIDER == Trafikanten.PROVIDER_TRAFIKANTEN) {
			return TrafikantenRoute.GetRouteList();
		}
		return null;
	}
}
