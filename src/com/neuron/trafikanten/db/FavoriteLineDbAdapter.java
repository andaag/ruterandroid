package com.neuron.trafikanten.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;

/*
 * This is for favorite lines, for the seperate favorites screen
 * Note, this uses a (stupid) flat layout, there's a lot of duplication in this table for no reason. But it's easy that way.
 */
public class FavoriteLineDbAdapter {
	public static final String KEY_ROWID = "_id";
    public static final String KEY_STATIONID = "stationid";
	public static final String KEY_STOPNAME = "stopname";
	public static final String KEY_DESTINATION = "destination";
	public static final String KEY_LINE = "line";
	public static final String KEY_UTM_X = "utmX";
	public static final String KEY_UTM_Y = "utmY";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";

    private DatabaseHelper dbHelper;
    public SQLiteDatabase db;
    private Context context;
    
	public static final String[] COLUMNS = new String[] {
		KEY_STATIONID,
		KEY_STOPNAME, 
		KEY_DESTINATION,
		KEY_LINE,
		KEY_UTM_X,
		KEY_UTM_Y,
		KEY_LATITUDE,
		KEY_LONGITUDE};
    
    private static final String database_name = "favoriteline";
    private static final String table = "Trafikanten";
    private int database_version = 2;
    
    private static final String DATABASE_CREATE_TABLE =
        "(_id integer primary key autoincrement, "
    			+ KEY_STATIONID + " int,"
    			+ KEY_STOPNAME + " text not null,"
    			+ KEY_DESTINATION + " text,"
    			+ KEY_LINE + " text,"
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
    
    public FavoriteLineDbAdapter(Context context) { 
    	this.context = context;
    	open();
    }

    /*
     * Open the database and check if we're on an old version.
     */
    public void open() throws SQLException {
    	if (db == null) {
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
     * Check if station is favorite
     */
	public boolean isFavorite(StationData station, String line, String destination) {
		String query = KEY_STATIONID + " = ? " +
				" AND " + KEY_LINE + " = ? " +
				" AND " + KEY_DESTINATION + " = ?";
		String args[] = new String[] { Integer.toString(station.stationId), line, destination };
		boolean isFav = false;
		final Cursor cursor = db.query(table, COLUMNS, query, args, null, null, null);
		isFav = cursor.moveToFirst();
		cursor.close();
		return isFav;
	}

	/*
	 * Toggle favorite status
	 */
	public void toggleFavorite(StationData station, String line, String destination) {
		if (isFavorite(station, line, destination)) {
			String query = KEY_STATIONID + " = ? " +
				" AND " + KEY_LINE + " = ? " +
				" AND " + KEY_DESTINATION + " = ?";
			String args[] = new String[] { Integer.toString(station.stationId), line, destination };
			db.delete(table, query, args);
		} else {
			station.getLongLat(); // make sure we store to database with long/lat saved.
	    	final ContentValues values = new ContentValues();
	    	values.put(KEY_STATIONID, station.stationId);
	    	values.put(KEY_STOPNAME, station.stopName);
	    	values.put(KEY_DESTINATION, destination);
	    	values.put(KEY_LINE, line);
	    	values.put(KEY_UTM_X, station.utmCoords[0]);
	    	values.put(KEY_UTM_Y, station.utmCoords[1]);
	    	values.put(KEY_LATITUDE, station.latLongCoords[0]);
	    	values.put(KEY_LONGITUDE, station.latLongCoords[1]);

	    	db.insert(table, null, values);
		}
	}
	
	/*
	 * Get all favorite data and return array.
	 */
	public ArrayList<FavoriteStation> getFavoriteData() {
		final Cursor cursor = db.query(table, COLUMNS, null, null, null, null, null);
		ArrayList<FavoriteStation> items = new ArrayList<FavoriteStation>();
		while (cursor.moveToNext()) {
			/*
			 * Load station
			 */
			StationData station = new StationData(cursor.getString(1), null, cursor.getInt(0), true, new int[] {cursor.getInt(4), cursor.getInt(5)});
			station.latLongCoords = new double[] {cursor.getDouble(6), cursor.getDouble(7)};
			
			/*
			 * Load destination and line
			 */
			final FavoriteData data = new FavoriteData(cursor.getString(3), cursor.getString(2));
			
			/*
			 * Find previous station in items
			 */
			{
			boolean found = false;
				for (FavoriteStation favStation : items) {
					if (favStation.station.stationId == station.stationId) {
						favStation.items.add(data);
						found = true;
						break;
					}
				}
				if (found) {
					continue;
				}
			}
			/*
			 * Couldn't find previous station, add a new one.
			 */
			FavoriteStation favStation = new FavoriteStation(station);
			favStation.items.add(data);
			items.add(favStation);
		}
		cursor.close();
		return items;
	}
	
	public static class FavoriteStation {
		public StationData station;
		public ArrayList<FavoriteData> items = new ArrayList<FavoriteData>();
		public FavoriteStation(StationData station) {
			this.station = station;
		}
	}
	
	public static class FavoriteData {
		public String line;
		public String destination;
		public FavoriteData(RealtimeData data) {
			line = data.line;
			destination = data.destination;
		}
		public FavoriteData(String line, String destination) {
			this.line = line;
			this.destination = destination;
		}
	}
}

