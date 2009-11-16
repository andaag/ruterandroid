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

import android.content.Context;
import android.database.Cursor;

import com.neuron.trafikanten.dataSets.StationData;

/*
 * Class for storing last X used stations.
 */
public class HistoryDbAdapter extends GenericStationDbAdapter {
	private static final int DATABASE_VERSION = 2;
	public HistoryDbAdapter(Context context) {
		super(context);
		super.open("history", DATABASE_VERSION);
	}
	
	public void open() {
		super.open("history", DATABASE_VERSION);
	}
	
	public void addHistoryToList(ArrayList<StationData> items) {
		/*
		 * Get all items from the list
		 */
    	Cursor cursor = db.query(table, COLUMNS, null, null, null, null, KEY_USED + " DESC");
    	while (cursor.moveToNext()) {
    		StationData station = new StationData(cursor.getString(0), 
    				cursor.getString(1), 
    				cursor.getInt(2), 
    				new int[] {cursor.getInt(4), cursor.getInt(5)});
    		/*
    		 * Check for duplicates
    		 */
    		boolean foundDuplicate = false;
    		for (StationData listStation : items) {
    			if (station.stationId == listStation.stationId) {
    				foundDuplicate = true;
    				break;
    			}
    		}
    		
    		if (!foundDuplicate) {
    			items.add(station);
    		}
    	}
    	cursor.close();
	}
	
	public boolean hasStation(int stationId) {
		Cursor c = db.query(table, new String[] { KEY_STATIONID }, KEY_STATIONID + " = " + stationId, null, null, null, null);
		boolean r = c.moveToNext();
		c.close();
		return r;
	}
	
	public void updateHistory(StationData station) {
		if (hasStation(station.stationId)) {
			/*
			 * Update ROWID to max(_id) + 1 (for auto cleaning old entries)
			 * Update used to used + 1
			 */
			final String rowIdSql = "(SELECT MAX(" + KEY_ROWID + ") + 1 FROM " + table + ")"; 
			final String sql = String.format("UPDATE %s SET %s = %s + 1, %s = %s WHERE %s = %d", table, KEY_USED, KEY_USED, KEY_ROWID, rowIdSql, KEY_STATIONID, station.stationId);
			final Cursor c = db.rawQuery(sql, null);
			c.moveToFirst();
			c.close();
		} else {
			/*
			 * Add station
			 */
			add(station);
		}
		
		/*
		 * Then delete entries too old from the list.
		 */
		db.delete(table, KEY_ROWID +
				" < (select min(_id) from (select _id from " + table + " order by _id desc limit 10))", null);
	}

}
