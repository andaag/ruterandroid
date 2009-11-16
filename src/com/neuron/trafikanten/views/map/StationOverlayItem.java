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

package com.neuron.trafikanten.views.map;

import com.neuron.trafikanten.dataSets.SearchStationData;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class SearchStationOverlayItem extends OverlayItem {
	public SearchStationData station;

	public SearchStationOverlayItem(GeoPoint point, SearchStationData station) {
		super(point, station.stopName, station.extra);
		this.station = station;
	}
}
