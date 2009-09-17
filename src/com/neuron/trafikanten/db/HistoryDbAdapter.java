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

import com.neuron.trafikanten.dataSets.SearchStationData;

import android.content.Context;
import android.database.Cursor;

/*
 * Class for storing last X used stations.
 */
public class HistoryDbAdapter extends GenericStationDbAdapter {
	private static final int DATABASE_VERSION = 1;
	public HistoryDbAdapter(Context context) {
		super(context);
		super.open("history", DATABASE_VERSION);
	}
	
	public void open() {
		super.open("history", DATABASE_VERSION);
	}
	
	public void addHistoryToList(ArrayList<SearchStationData> items) {
		/*
		 * Get all items from the list
		 */
    	Cursor cursor = db.query(table, COLUMNS, null, null, null, null, KEY_STOPNAME);
    	while (cursor.moveToNext()) {
    		SearchStationData station = new SearchStationData(cursor.getString(0), 
    				cursor.getString(1), 
    				cursor.getInt(2), 
    				new int[] {cursor.getInt(3), cursor.getInt(4)});
    		/*
    		 * Check for duplicates
    		 */
    		boolean foundDuplicate = false;
    		for (SearchStationData listStation : items) {
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
	
	public void updateHistory(SearchStationData station) {
		/*
		 * First delete the station, this is to give it a higher id if we use it twice.
		 */
		delete(station.stationId);
		
		/*
		 * Then readd it
		 */
		add(station);
		
		/*
		 * Then delete entries too old from the list.
		 * We maintain a quite big cache (30 stations), this is due to favorites most likely having duplicates.
		 */
		db.delete(table, KEY_ROWID +
				" < (select min(_id) from (select _id from " + table + " order by _id desc limit 5))", null);
	}

}
