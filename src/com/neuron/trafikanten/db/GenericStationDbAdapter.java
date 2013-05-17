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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.neuron.trafikanten.dataSets.StationData;

public abstract class GenericStationDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_STATIONID = "stationid";
	public static final String KEY_STOPNAME = "stopname";
	public static final String KEY_USED = "used";
	public static final String KEY_EXTRA = "extra";
	public static final String KEY_REALTIMESTOP = "realtimestop";
	public static final String KEY_UTM_X = "utmX";
	public static final String KEY_UTM_Y = "utmY";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";

    private DatabaseHelper dbHelper;
    public SQLiteDatabase db;
    private Context context;
    
    private String database_name;
    public String table;
    private int database_version = 0;
    
    
	public static final String[] COLUMNS = new String[] { KEY_STOPNAME, 
		KEY_EXTRA,
		KEY_STATIONID,
		KEY_USED,
		KEY_REALTIMESTOP,
		KEY_UTM_X,
		KEY_UTM_Y,
		KEY_LATITUDE,
		KEY_LONGITUDE};
    
    private static final String DATABASE_CREATE_TABLE =
        "(_id integer primary key autoincrement, "
    			+ KEY_STATIONID + " int unique,"
    			+ KEY_STOPNAME + " text not null,"
    			+ KEY_USED + " int not null,"
    			+ KEY_EXTRA + " text,"
    			+ KEY_REALTIMESTOP + " boolean not null,"
    			+ KEY_UTM_X + " int,"
    			+ KEY_UTM_Y + " int,"
    			+ KEY_LATITUDE + " real,"
    			+ KEY_LONGITUDE + " real);";
    
    private class DatabaseHelper extends SQLiteOpenHelper {
    	/*
    	 * Checking if the database is too old, and upgrade if neccesary.
    	 */
        DatabaseHelper(Context context) {
            super(context, database_name, null, database_version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + table + " " + DATABASE_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	Toast.makeText(context, "Upgrading database, deleting all old station data", Toast.LENGTH_SHORT).show();
            db.execSQL("DROP TABLE IF EXISTS " + table);
            onCreate(db);
        }
    }

    public GenericStationDbAdapter(Context context) { this.context = context; }

    /*
     * Open the database and check if we're on an old version.
     */
    public void open(String database, int version) throws SQLException {
    	if (db == null) {
    		database_version = version;
    		database_name = database;
    		table = "Trafikanten";
	        dbHelper = new DatabaseHelper(context);
	        db = dbHelper.getWritableDatabase();
    	}
    }
    
    /*
     * Check if the database is open
     */
    public boolean isOpen() {
    	return (db != null);
    }
    
    /*
     * Close the database gracefully.
     */
	public void close() { 
		if (db != null) {
			dbHelper.close();
			db.close();
			dbHelper = null;
			db = null;
		}
	}
    
	/*
	 * Add new station to list.
	 */
    public void add(StationData station) {
    	if (station.type != StationData.TYPE_STATION) {
    		// For now we dont put non station types in history/favorites 
    		return;
    	}
    	final ContentValues values = new ContentValues();
    	values.put(KEY_STATIONID, station.stationId);
    	values.put(KEY_STOPNAME, station.stopName);
    	values.put(KEY_USED, 1);
    	values.put(KEY_EXTRA, station.extra);
    	values.put(KEY_REALTIMESTOP, station.realtimeStop);
    	values.put(KEY_UTM_X, station.utmCoords[0]);
    	values.put(KEY_UTM_Y, station.utmCoords[1]);
    	values.put(KEY_LATITUDE, station.latLongCoords[0]);
    	values.put(KEY_LONGITUDE, station.latLongCoords[1]);
    	
    	db.insert(table, null, values);
    }
    
    /*
     * Delete station from list.
     */
    public boolean delete(int stationId) {
    	return db.delete(table, KEY_STATIONID + "=" + stationId, null) > 0;
    }
    
    /*
     * Get list of STATIONID's.
     */
    public Cursor getIds() {
    	return db.query(table, new String[] { KEY_STATIONID }, null, null, null, null, null);
    }

    protected Context getContext() {
        return context;
    }
}