package com.xanderfehsenfeld.tigertest.GPS;


/**
 * created by Xander
 * much of this code is from http://stackoverflow.com/questions/14940657/android-speech-recognition-as-a-service-on-android-4-1-4-2/14950616#14950616
 * It is a service which is a "continuous" speech recognizer - not quite continuous because it pauses to analyze speech
 *
 */

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import java.lang.ref.WeakReference;

public class MyGPSLocationService extends Service
{
    private static final String TAG = "DatabaseManagerService";
    private static final String LOCATION_SERVICES_TAG = "DatabaseManagerService";
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;


    /* send data back to MainActivity */
    ResultReceiver resultReceiver;

    /* keep speech recognizer going */
    CountDownTimer mTimer;
    //private GoogleApiClient mGoogleApiClient;

    private LocationManager mLocationManager;

    // A request to connect to Location Services
    //private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private long LOCATION_REFRESH_TIME = 2000;
    private static final float LOCATION_REFRESH_DISTANCE = 1;



    /* location listener */
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            logLocation(location);
            Toast t =Toast.makeText(MyGPSLocationService.this, "onLocationChanged! ", Toast.LENGTH_SHORT);
            t.show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: " + provider + ", status: " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onPoviderEnabled: " + provider);

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onPoviderDisabled: " + provider);

        }
    };

    @Override
    public void onCreate()
    {

        Log.d(TAG, "onCreate"); //$NON-NLS-1$


        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);


        // getting GPS status
        boolean isGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.v("isGPSEnabled", "=" + isGPSEnabled);

        logLocation(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$

        return START_STICKY;



    }



    /* get the most current location */
    public void updateLocation(){
//        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if (mLastLocation != null) {
//            Log.d(LOCATION_SERVICES_TAG, "updateLocation: updated location");
//            logLocation(mLastLocation);
//
//
//
//        } else {
//
//            Log.d(LOCATION_SERVICES_TAG, "updateLocation: last Location unavailable");
//
//            Toast toast = Toast.makeText(MyGPSLocationService.this, getResources().getString(R.string.location_unavailable_message), Toast.LENGTH_SHORT);
//            toast.show();
//        }
    }





    private void logLocation( Location location ){
        Log.d(LOCATION_SERVICES_TAG, "onLocationChanged. " + "lat: " + location.getLatitude() + ", " +
                "lon: " + location.getLongitude() + " prov: " +
                location.getProvider() + ", acc: " + location.getAccuracy());
    }




    /* IncomingHandler */
    protected class IncomingHandler extends Handler
    {
        private WeakReference<MyGPSLocationService> mtarget;

        IncomingHandler(MyGPSLocationService target)
        {
            mtarget = new WeakReference<MyGPSLocationService>(target);
            Log.d(TAG, "IncomingHandler");
        }


        @Override
        public void handleMessage(Message msg)
        {
            final MyGPSLocationService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    Log.d(TAG, "message start listening");
                    break;

                case MSG_RECOGNIZER_CANCEL:

                    Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

//

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

//    /** checkLocationSetings
//     *      determine if the user's location settings are satisfactory
//     *      should only be called after connecting to play services
//     */
//    private void checkLocationSettings(){
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
//                .addLocationRequest(mLocationRequest);
//        PendingResult<LocationSettingsResult> result =
//                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
//                        builder.build());
//
//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult result) {
//                final Status status = result.getStatus();
//                final LocationSettingsStates settingsState = result.getLocationSettingsStates();
//
//                Log.d(LOCATION_SERVICES_TAG, "checkLocationSettingsResults: gps present " + settingsState.isGpsPresent()
//                        + ", gps usable: " + settingsState.isGpsUsable()
//                        + ", network present: " + settingsState.isNetworkLocationPresent()
//                        + ", network usable: " + settingsState.isNetworkLocationUsable()
//                        + ", location (present, usable): " + settingsState.isLocationPresent() + ", " + settingsState.isLocationUsable());
//
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        Log.d(LOCATION_SERVICES_TAG, "checkLocationSettings: success");
//
//                        // All location settings are satisfied. The client can
//                        // initialize location requests here.
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//
//                        Log.d(LOCATION_SERVICES_TAG, "checkLocationSettings: resolution required!");
//
//                        // Location settings are not satisfied, but this can be fixed
//                        // by showing the user a dialog.
////                        try {
////                            // Show the dialog by calling startResolutionForResult(),
////                            // and check the result in onActivityResult().
//////                            status.startResolutionForResult(
//////                                    OuterClass.this,
//////                                    REQUEST_CHECK_SETTINGS);
////                        } catch (IntentSender.SendIntentException e) {
////                            // Ignore the error.
////                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        // Location settings are not satisfied. However, we have no way
//                        // to fix the settings so we won't show the dialog.
//                        break;
//                }
//            }
//        });
//    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub
            //Log.d(TAG, "Timer: " + millisUntilFinished );


        }

        @Override
        public void onFinish()
        {
            Log.d(TAG, "countdown finished"); //$NON-NLS-1$

        }
    };



}