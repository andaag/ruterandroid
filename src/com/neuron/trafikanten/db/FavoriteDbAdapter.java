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

package com.neuron.trafikanten.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.neuron.trafikanten.dataSets.StationData;

/*
 * Class for storing favorite stations.
 */
public class FavoriteDbAdapter extends GenericStationDbAdapter {
	private static final int DATABASE_VERSION = 6;
	public FavoriteDbAdapter(Context context) {
		super(context);
		super.open("favorites", DATABASE_VERSION);
	}
	
	public void open() {
		super.open("favorites", DATABASE_VERSION);
	}
	
	/*
	 * Scan list and set .isFavorite where neccesary.
	 */
	public void refreshFavorites(ArrayList<StationData> list) {
		for (StationData station : list)
			station.isFavorite = false;
		
		Cursor cursor = getIds();
		while (cursor.moveToNext()) {
			int stationId = cursor.getInt(0);
			for (StationData station : list)
				if (station.stationId == stationId) {
					station.isFavorite = true;
					break;
				}
		}
		cursor.close();
	}
	
	/*
	 * Toggles whether a station is favorite or not.
	 */
	public boolean toggleFavorite(StationData station) {
		if (delete(station.stationId))
			return false;
		/*
		 * Before adding, calculate proper long/lat to store in the database.
		 */
		station.getLongLat();
		add(station);
		return true;
	}
	
	/*
	 * Updates KEY_USED
	 */
	public void updateUsed(StationData station) {
		final int realtimeStop = station.realtimeStop ? 1 : 0; 
		final String sql = String.format("UPDATE %s SET %s = %s + 1, %s = %d WHERE %s = %d", table, KEY_USED, KEY_USED, KEY_REALTIMESTOP, realtimeStop, KEY_STATIONID, station.stationId);
		final Cursor c = db.rawQuery(sql, null);
		c.moveToFirst();
		c.close();
	}
	
	/*
	 * Add favorites to a station list.
	 */
    public void addFavoritesToList(boolean isRealtimeSelector, List<StationData> items) {
    	Cursor cursor = db.query(table, COLUMNS, null, null, null, null, KEY_USED + " DESC");
    	while (cursor.moveToNext()) {
    		StationData station = new StationData(cursor.getString(0), 
    				cursor.getString(1), 
    				cursor.getInt(2), 
    				new int[] {cursor.getInt(4), cursor.getInt(5)});
    		station.isFavorite = true;
    		if (isRealtimeSelector) {
    			if (station.realtimeStop) {
    				items.add(station);
    			}
    		} else {
    			items.add(station);
    		}
    		
    	}
    	cursor.close();
    }
	

}
