package com.neuron.trafikanten.hacks;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GoogleAnalyticsCleanup extends SQLiteOpenHelper {
    // The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/com.neuron.trafikanten/databases/";

    private static String DB_NAME = "google_analytics";

    private SQLiteDatabase myDataBase;

    /**
     * Constructor Takes and keeps a reference of the passed context in order to
     * access to the application assets and resources.
     * 
     * @param context
     */
    public GoogleAnalyticsCleanup(Context context) {
        super(context, DB_NAME, null, 1);
    }

    public void openDataBase() throws SQLException {

        // Open the database
        String myPath = DB_PATH + DB_NAME + ".db";
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);

    }

    @Override
    public synchronized void close() {

        if (myDataBase != null) myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int deleteEvents() {
        return myDataBase.delete("events", "label LIKE '% %' OR action LIKE '% %'", null);
    }

}
