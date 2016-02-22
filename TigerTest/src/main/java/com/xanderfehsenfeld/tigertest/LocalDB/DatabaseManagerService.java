package com.xanderfehsenfeld.tigertest.LocalDB;


/**
 * created by Xander
 */

import android.app.Service;
import android.content.Intent;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xanderfehsenfeld.tigertest.R;
import com.xanderfehsenfeld.tigertest.ServerRequestor;
import com.xanderfehsenfeld.tigertest.SpeedTestLauncher;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class DatabaseManagerService extends Service
{
    private static final String TAG = "DatabaseManagerService";
    private static final String LOCATION_SERVICES_TAG = "DatabaseManagerService";
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    public static final int MSG_RECOGNIZER_START_LISTENING = 1;
    public static final int MSG_RECORD_ADDED = 2;


    /* send data back to MainActivity */
    ResultReceiver resultReceiver;

    /* keep speech recognizer going */
    CountDownTimer mTimer;

    /* db with wrapper class */
    MyDbWrapper db;
    private boolean mTransferInProgress = false;


    @Override
    public void onCreate()
    {

        Log.d(TAG, "onCreate"); //$NON-NLS-1$



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$



        return START_STICKY;



    }

    /* IncomingHandler */
    protected class IncomingHandler extends Handler
    {
        private WeakReference<DatabaseManagerService> mtarget;

        IncomingHandler(DatabaseManagerService target)
        {
            mtarget = new WeakReference<DatabaseManagerService>(target);
            Log.d(TAG, "IncomingHandler");
        }


        @Override
        public void handleMessage(Message msg)
        {
            final DatabaseManagerService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    Log.d(TAG, "message start listening");
                    break;

                case MSG_RECORD_ADDED:

                    Log.d(TAG, "message record added"); //$NON-NLS-1$

                    if( !mTransferInProgress ){
                        transferRecords();
                    }
                    break;
            }
        }
    }

    /** transferRecords
     *      transfer all the records to the server
     *      the server requester deletes the records upon response from server so the db should eventually dwindle to empty
     */
    private void transferRecords(){
        mTransferInProgress = true;
        //db.printDb();


        if ( db.getRecordCount() > 0 ) {
            for ( String id : db.getIds()) {
                HashMap<String,String> record = db.retrieveRecord(id);


                //HashMap<String, String> record = db.getARecord();
                if ((record != null)) {

                    String toLog = "";
                    for (String key : record.keySet()) {
                        toLog += "|" + key + ":" + record.get(key);
                    }
                    Log.d(TAG, toLog);
                    try {
                        interpretResponse( ServerRequestor.post(SpeedTestLauncher.SERVER_URL, db.getARecord(), db) );
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Record from getArecord == null");
                }
            }
//        while( db.getRecordCount() >0 ) {
//
//        }
        }
        mTransferInProgress = false;

    }

    /** interpretResponse
     *      if the server returns a uuid, this function will remove that record from the db
     * @param response the response string
     */
    private void interpretResponse(String response){
            if (db.retrieveRecord(response) != null) db.removeRecord(response);
    }



    @Override
    public void onDestroy()
    {
        super.onDestroy();


        /* send end code to parent activity */
        Bundle bundle = new Bundle();
        //bundle.putString("end", "Timer Stopped....");
        resultReceiver.send(200, bundle);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");  //$NON-NLS-1$

        resultReceiver = intent.getParcelableExtra("receiver");

        db = intent.getParcelableExtra("db");
        if ( db != null ) {
            Log.d(TAG, "recieved db not null");
        } else{
            Log.e(TAG, "recieved db is null");
        }


        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getApplicationContext());
        db = new MyDbWrapper(mDbHelper.getWritableDatabase());
        Log.d(TAG, "opened writeable db:");
        db.printDb();


        /* send an initial amount */
        return mServerMessenger.getBinder();
    }



    /* send results out */
    private void sendResults( String result ){
        /* send info back to parent activity */
//        Bundle bundle = new Bundle();
//        bundle.putInt("score", currentScore);
//        //bundle.putStringArrayList("badwords", SpeechAnalyzer.getBadWords(result));
//        bundle.putString("whatWasSaid", result);
//        resultReceiver.send(2, bundle);
    }




}