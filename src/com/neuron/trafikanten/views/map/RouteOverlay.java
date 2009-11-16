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

import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.StationData;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;


/*
 * CURRENTLY NOT IN USE, Trafikanten.no does not give out coordinates on routes.
 */

/*
 * Even the route is using StationData, it's a direct list, route goes from .get(0) to .get(.size())
 */
public class RouteOverlay extends ItemizedOverlay<StationOverlayItem> {
	private ArrayList<StationOverlayItem> items = new ArrayList<StationOverlayItem>();

	public RouteOverlay(Drawable defaultMarker) {
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
		//GenericMap.onStationTap(items.get(index).station);
		// TODO : Implement onTap system for route data, currently it'll fail as stationList.size == 0.
		return false;
	}
	
	private void addStation(StationData station) {
		final double[] location = station.getLongLat();
		final GeoPoint point = new GeoPoint((int)(location[0] * 1E6), (int)(location[1] * 1E6));
		final StationOverlayItem item = new StationOverlayItem(point, station);
		items.add(item);
	}
	
	public void add(ArrayList<RouteData> routeDataList) {
		final int size = routeDataList.size();
		
		for(int i = 0; i < size; i++) {
			final RouteData routeData = routeDataList.get(i);
			/*
			 * Add fromStation
			 */
			addStation(routeData.fromStation);
		}
		{
			/*
			 * Add last station destination
			 */
			final RouteData routeData = routeDataList.get(size - 1);
			addStation(routeData.toStation);
		}
		populate();
	}
	
	private void drawRoute(Canvas canvas, Projection projection, StationData fromStation, StationData toStation) {
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(5);
		paint.setAlpha(110);
		
		final Point pointA = new Point();
		final double[] locationA = fromStation.getLongLat();
		projection.toPixels(new GeoPoint((int)(locationA[0] * 1E6), (int)(locationA[1] * 1E6)), pointA);
		
		final Point pointB = new Point();
		final double[] locationB = toStation.getLongLat();
		projection.toPixels(new GeoPoint((int)(locationB[0] * 1E6), (int)(locationB[1] * 1E6)), pointB);
		
		canvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paint);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		Projection projection = mapView.getProjection();
		
		int size = items.size() - 1;
		for (int i = 0; i < size; i++) {
			final StationData from = items.get(i).station;
			final StationData to = items.get(i + 1).station;
			drawRoute(canvas, projection, from, to);
		}
		super.draw(canvas, mapView, shadow);
	}
	
	
	
}