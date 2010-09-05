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

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.hacks.StationIcons;

// TODO : Rename to StationOverlay
public class GenericStationOverlay extends ItemizedOverlay<StationOverlayItem> {
	public ArrayList<StationOverlayItem> items = new ArrayList<StationOverlayItem>();
	private boolean warnAboutMissingCoordinates = true;

	public GenericStationOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	/*
	 * Simple helper functions
	 */
	@Override
	protected StationOverlayItem createItem(int i) { return items.get(i); }
	@Override
	public int size() { return items.size(); }
	
	/*
	 * OnTap
	 */
	@Override
	protected boolean onTap(int index) {
		GenericMap.onStationTap(items.get(index).station);
		return super.onTap(index);
	}

	/*
	 * Add single item, this does not populate!
	 */
	public void add(Activity activity, StationData station, boolean populate, int transportType) {
		if (items.size() > 0 && items.get(items.size() - 1).station.stationId == station.stationId)
			return;
		
		final double[] location = station.getLongLat();
		if (location[0] == 0) {
			if (warnAboutMissingCoordinates) {
				Toast.makeText(activity, R.string.stationsMissingCoordsWillBeHidden, Toast.LENGTH_SHORT).show();
				warnAboutMissingCoordinates = false;
			}
			return;
		}
		final GeoPoint point = new GeoPoint((int)(location[0] * 1E6), (int)(location[1] * 1E6));
		final StationOverlayItem item = new StationOverlayItem(point, station);
		
		/*if (station.isFavorite) {
			item.setMarker(boundCenterBottom(activity.getResources().getDrawable(R.drawable.icon_mapmarker_favorite)));
		} else {
			item.setMarker(null);
		}*/
		final int marker = StationIcons.getBlackStationIcons(transportType > 0 ? transportType : StationIcons.hackGetStationIcon(station.stopName));
		item.setMarker(boundCenterBottom(activity.getResources().getDrawable(marker)));
		
		items.add(item);
		if (populate) {
			populate();
		}
	}

	/*
	 * Add list of items
	 */
	public void add(Activity activity, ArrayList<StationData> stationList) {
		for(StationData station : stationList) {
			add(activity, station, false, 0);
		}
		populate();
	}
}
