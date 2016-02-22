package com.xanderfehsenfeld.tigertest.LocalDB;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Xander on 2/21/16.
 *
 *      A 'wrapper' class to perform functions of the db. These are the only functions I need, and I need to pass the
 *      wrapper class to other functions. I wanted to be able to access both the functions and db from one space
 */
public class MyDbWrapper {

    private static final String DB_TAG = "MyDbWrapper";
    public SQLiteDatabase db;


    public MyDbWrapper(SQLiteDatabase db){
        this.db = db;

        /* create the table */
        //db.execSQL(FeedReaderDbHelper.SQL_DELETE_ENTRIES);
        createTable();
    }

    public void dropTable(){
        db.execSQL(FeedReaderDbHelper.SQL_DELETE_ENTRIES);
    }
    public void createTable(){
        db.execSQL(FeedReaderDbHelper.SQL_CREATE_ENTRIES);
    }


    /** getRecordCount
     * @return the number of records
     */
    public int getRecordCount(){
        Cursor mCount= db.rawQuery("select count(*) from " + FeedReaderDbHelper.TABLE_NAME, null);
        mCount.moveToFirst();
        int count= mCount.getInt(0);
        mCount.close();
        return count;
    }

    /**saveRecord
     *
     * @param record record to be stored
     * @return the new row id or -1 if the statement was unsuccessful
     */
    public long saveRecord(HashMap<String, String> record){

        int initial = getRecordCount();

        ContentValues values = new ContentValues();


        /* put the data in the database */
        for (String key : record.keySet()){
            values.put(key, record.get(key));
        }

        // Insert the new row, returning the primary key value of the new row
        long newRowId;

        db.beginTransaction();
        try {
            newRowId = db.insert(FeedReaderDbHelper.TABLE_NAME, "null", values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        int fin = getRecordCount();

        String toLog = "";
        if ( newRowId != -1){
            toLog += "saveRecord. added one item to db with uuid: "
                    + record.get(FeedReaderDbHelper.ID_STRING);

        } else {
            toLog += "saveRecord. did not add item with uuid: " + record.get(FeedReaderDbHelper.ID_STRING);
        }
        toLog += " init, fin: " + initial +", " + fin;
        Log.d(DB_TAG, toLog);

        return newRowId;


    }

    public void printDb(){
        System.out.println(  FeedReaderDbHelper.getTableAsString(db, FeedReaderDbHelper.TABLE_NAME));
    }

    /**removeRecord
     * @param uuid the record to be removed
     * @return number of records removed
     */
    public int removeRecord(String uuid){

        /* test to see if record is present */
        //retrieveRecord(uuid);
        int countI = getRecordCount();

        String tableName = FeedReaderDbHelper.TABLE_NAME;
        String selection = FeedReaderDbHelper.ID_STRING + " = ?";
        String[] selectionArgs = new String[] {uuid};


        int deleted = 0;
        db.beginTransaction();
        try {
            deleted = db.delete(tableName, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        String result = "removeRecord. ";
        if (deleted > 0){
            result += "deleted one or more items with uuid " + uuid;
        } else {
            result += "did not delete one or more items with uuid " + uuid;
            printDb();
        }
        result += "(init, fin): " + countI + ", " + getRecordCount();
        Log.d(DB_TAG, result);

        return deleted;

    }

    /** retrieveRecord
     *      retrieve a row from the local db
     * @param uuid the id of the row
     * @return the hashmap containing the record, or null if record not in db
     */
    public HashMap<String, String> retrieveRecord(String uuid){
        /* we want every column */
        String[] projection = FeedReaderDbHelper.types.keySet().toArray(new String[FeedReaderDbHelper.types.keySet().size()]);

        // How you want the results sorted in the resulting Cursor
        String sortOrder = "";

        String selection = FeedReaderDbHelper.ID_STRING;
        String[] selectionArgs = new String[]{uuid};

        Cursor c = db.rawQuery("SELECT * FROM " + FeedReaderDbHelper.TABLE_NAME + " WHERE " +
                FeedReaderDbHelper.ID_STRING + " = ?", new String[]{uuid});

        HashMap<String, String> output = new HashMap<>();

        String toPrint = "retrieveRecord.";
        if (c != null && c.moveToFirst()) {
            do {

                for (String key : FeedReaderDbHelper.types.keySet()) {
                    String val = c.getString(c.getColumnIndexOrThrow(key)
                    );
                    toPrint += " " + key + ":" + val + " ";
                    output.put(key, val);
                }

            } while (c.moveToNext());


            Log.d(DB_TAG, toPrint);
            return output;
        }
        Log.d(DB_TAG, toPrint);

        return null;



    }

    /** getARecord
     *
     * @return the top record
     */
    public HashMap<String, String> getARecord(){
        HashMap<String,String> output = new HashMap<>();
        Cursor c  = db.rawQuery("SELECT * FROM " + FeedReaderDbHelper.TABLE_NAME, null);
        if (c.moveToFirst() ){
            for (String key : FeedReaderDbHelper.types.keySet()) {
                String val = c.getString(c.getColumnIndexOrThrow(key)
                );
                output.put(key, val);
            }
        } else {
            output = null;
        }

        return output;
    }

    /** getIds
     *
     * @return an arraylist of the ids of all the records
     */
    public ArrayList<String> getIds(){
        ArrayList<String> output = null;

        Cursor allRows  = db.rawQuery("SELECT " + FeedReaderDbHelper.ID_STRING + " FROM " + FeedReaderDbHelper.TABLE_NAME, null);
        if (allRows.moveToFirst() ){
            output = new ArrayList<String>();
            do {
                output.add(allRows.getString(allRows.getColumnIndex(FeedReaderDbHelper.ID_STRING)));

            } while (allRows.moveToNext());
        }

        return output;

    }



}
