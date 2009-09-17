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

import com.neuron.trafikanten.dataSets.RouteData;

public interface IRouteProvider extends IGenericProvider {

	/*
	 * Generic transportation id's, note that .getImageResource is called, so feel free to use custom transportation types.
	 */
	public final static int TRANSPORT_UNKNOWN = 0;
	public final static int TRANSPORT_TRAIN = 1;
	public final static int TRANSPORT_TRAM = 2;
	public final static int TRANSPORT_BUS = 3;
	public final static int TRANSPORT_SUBWAY = 4;
	public final static int TRANSPORT_WALK = 5;
	

	public void Search(RouteData routeData);
	public void Stop();
}
