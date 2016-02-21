package com.xanderfehsenfeld.tigertest.LocalDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Xander on 2/20/16.
 */
public class FeedReaderDbHelper extends SQLiteOpenHelper {

    private static final String DB_TAG = "DB";


    /* key values for use in the 'data' hashmap in the speedtest launcher, which will be sent to the server
			These values must match the values on the server end for this to properly work,
			so ANY CHANGES HERE SHOULD BE REFLECTED ON THE SERVER_URL
	 */
        public static final String NETWORK_STRING = "network";
        public static final String LAT_STRING = "latitude";
        public static final String LONG_STRING = "longitude";
        public static final String DOWNLOAD_STRING = "fileDownloaded";
        public static final String ALT_STRING = "altitude";
        public static final String TIMESTAMP_STRING = "timeStamp";
        public static final String TIMESTAMPFMT_STRING = "timeStampFmt";
        public static final String CONNTIME_STRING = "connectionTime";
        public static final String CONNTIMEUNIT_STRING = "connectionTimeUnit";
        public static final String BYTES_STRING = "bytesIn";
        public static final String SPEED_STRING = "downSpeed";
        public static final String MAC_STRING = "MACAddr";
        public static final String ACCESSPT_STRING = "wirelessAccessPointMAC";
        public static final String LOCATIONPROV_STRING = "locationProvider";
        public static final String ID_STRING = "uuid";

    static final String TABLE_NAME = "appresults";

    static String TYPE_TEXT = "TEXT";
    static String TYPE_REAL = "REAL";
    static String TYPE_INT = "INTEGER";

    String[] columnNamesTypes = new String[]{
        NETWORK_STRING, TYPE_TEXT,
        LAT_STRING, TYPE_REAL,
        LONG_STRING, TYPE_REAL,
        DOWNLOAD_STRING, TYPE_TEXT,
        ALT_STRING,TYPE_REAL,
        TIMESTAMP_STRING, TYPE_TEXT,
        TIMESTAMPFMT_STRING,TYPE_TEXT,
        CONNTIME_STRING,TYPE_REAL,
        CONNTIMEUNIT_STRING, TYPE_TEXT,
        BYTES_STRING, TYPE_INT,
        SPEED_STRING,TYPE_REAL,
        MAC_STRING,TYPE_TEXT,
        ACCESSPT_STRING,TYPE_TEXT,
        LOCATIONPROV_STRING,TYPE_TEXT,
        ID_STRING, TYPE_TEXT + " PRIMARY KEY"
        };

    public static HashMap<String,String> types = new HashMap<String, String>(){
        {
            put(NETWORK_STRING, TYPE_TEXT);
            put(LAT_STRING, TYPE_REAL);
            put(LONG_STRING, TYPE_REAL);
            put(DOWNLOAD_STRING, TYPE_TEXT);
            put(ALT_STRING, TYPE_REAL);
            put(TIMESTAMP_STRING, TYPE_TEXT);
            put(TIMESTAMPFMT_STRING, TYPE_TEXT);
            put(CONNTIME_STRING, TYPE_REAL);
            put(CONNTIMEUNIT_STRING, TYPE_TEXT);
            put(BYTES_STRING, TYPE_INT);
            put(SPEED_STRING, TYPE_REAL);
            put(MAC_STRING, TYPE_TEXT);
            put(ACCESSPT_STRING, TYPE_TEXT);
            put(LOCATIONPROV_STRING, TYPE_TEXT);
            put(ID_STRING, TYPE_TEXT + " PRIMARY KEY");

        }};


    public static String SQL_CREATE_ENTRIES = getTableCreateStatement();

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;



    HashMap<String,String> columns;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /* initialize entries */
    private static String getTableCreateStatement(){
        String output;
        /* create a 'create' statement */
        output = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (";
        ArrayList<String> keys = new ArrayList<String>(types.keySet());
        for (int i = 0; i < keys.size(); i ++){
            String toAdd = "";
            toAdd += keys.get(i) + " " + types.get(keys.get(i));
            if ( i != keys.size() - 1) toAdd += ", ";
            output += toAdd;
        }
        output += ")";

        return output;


    }
    public void onCreate(SQLiteDatabase db) {

        getTableCreateStatement();
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d("DB helper", "executed statement: " + SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

        /**
         * Helper function that parses a given table into a string
         * and returns it for easy printing. The string consists of
         * the table name and then each row is iterated through with
         * column_name: value pairs printed out.
         *
         * @param db the database to get the table from
         * @param tableName the the name of the table to parse
         * @return the table tableName as a string
         */
        public static String getTableAsString(SQLiteDatabase db, String tableName) {
            Log.d("DB", "getTableAsString called");
            String tableString = String.format("Table %s:\n", tableName);
            Cursor allRows  = db.rawQuery("SELECT * FROM " + tableName, null);
            if (allRows.moveToFirst() ){
                String[] columnNames = allRows.getColumnNames();
                do {
                    for (String name: columnNames) {
                        tableString += String.format("%s: %s", name,
                                allRows.getString(allRows.getColumnIndex(name)));
                    }
                    tableString += "\n";

                } while (allRows.moveToNext());
            }

            return tableString;
        }

}

//        columns = new HashMap(){
//            put(NETWORK_STRING, TYPE_TEXT),
//            put(LAT_STRING, TYPE_REAL),
//            put(LONG_STRING,TYPE_REAL),
//            put(DOWNLOAD_STRING, TYPE_TEXT),
//            put(ALT_STRING, TYPE_REAL),
//            put(TIMESTAMP_STRING, TYPE_TEXT),
//            put(TIMESTAMPFMT_STRING, TYPE_TEXT),
//            put(CONNTIME_STRING,TYPE_REAL),
//            put(CONNTIMEUNIT_STRING,TYPE_TEXT),
//            put(BYTES_STRING,TYPE_INT),
//            SPEED_STRING,
//            MAC_STRING,
//            ACCESSPT_STRING,
//            LOCATIONPROV_STRING,
//            ID_STRING
//        };