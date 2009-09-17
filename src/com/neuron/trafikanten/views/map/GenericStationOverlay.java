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

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.SearchStationData;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;

// TODO : Rename to StationOverlay
public class GenericStationOverlay extends ItemizedOverlay<SearchStationOverlayItem> {
	private ArrayList<SearchStationOverlayItem> items = new ArrayList<SearchStationOverlayItem>();
	private boolean warnAboutMissingCoordinates = true;

	public GenericStationOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	/*
	 * Simple helper functions
	 */
	@Override
	protected SearchStationOverlayItem createItem(int i) { return items.get(i); }
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
	 * Add list of items
	 */
	public void add(Activity activity, ArrayList<SearchStationData> stationList) {
		final Resources resources = activity.getResources();
		
		for(SearchStationData station : stationList) {
			final double[] location = station.getLongLat();
			if (location[0] == 0) {
				if (warnAboutMissingCoordinates) {
					Toast.makeText(activity, R.string.stationsMissingCoordsWillBeHidden, Toast.LENGTH_SHORT).show();
					warnAboutMissingCoordinates = false;
				}
				continue;
			}
			final GeoPoint point = new GeoPoint((int)(location[0] * 1E6), (int)(location[1] * 1E6));
			final SearchStationOverlayItem item = new SearchStationOverlayItem(point, station);
			if (station.isFavorite) {
				item.setMarker(boundCenterBottom(resources.getDrawable(R.drawable.icon_mapmarker_favorite)));
			} else {
				item.setMarker(null);
			}
			items.add(item);
		}
		populate();
	}
}
