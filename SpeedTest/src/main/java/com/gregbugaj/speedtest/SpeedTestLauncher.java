/*
	This file is part of SpeedTest.

    SpeedTest is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SpeedTest is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SpeedTest.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.gregbugaj.speedtest;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Test speed of our network connection
 * @author Greg Bugaj http://www.gregbugaj.com
 * @version 1.0
 *
 */
public class SpeedTestLauncher extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	//Private fields
	private static final String TAG = SpeedTestLauncher.class.getSimpleName();
	private static int EXPECTED_SIZE_IN_BYTES = 5 * 1000000;//5MB 1024*1024

	private static final String TIMESTAMP_FORMAT = "dd/MM/yy HH:mm:ss";
	//private final String SERVER = getResources().getString( R.string.server_url);
	private final String SERVER = "http://172.30.44.20:8000";
	private String download_file_url = "http://www.smdc.army.mil/smdcphoto_gallery/Missiles/IFT_13B_Launch/IFT13b-3-02.jpg";


	//private static final double EDGE_THRESHOLD = 176.0;
	private static final double BYTE_TO_KILOBIT = 0.0078125;
	private static final double KILOBIT_TO_MEGABIT = 0.0009765625;

	private Button mBtnStart;
	private TextView mTxtSpeed;
	private TextView mTxtConnectionSpeed;
	private TextView mTxtProgress;
	private TextView mTxtNetwork;
	private TextView mTxtMoreInfo;
	private TextView mTxtName;
	private TextView mTxtMAC;

	/* connection time */
	private double mConnectionTime = 0;

	/* store current location */
	private double mLatitude = 0;
	private double mLongitude = 0;

	/* HashMap to store resultant data */
	HashMap<String, String> data;

	private final int MSG_UPDATE_STATUS=0;
	private final int MSG_UPDATE_CONNECTION_TIME=1;
	private final int MSG_COMPLETE_STATUS=2;

	private final static int UPDATE_THRESHOLD=200;

	/* location */
	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;


	private DecimalFormat mDecimalFormater;

	/** Called when the activity is first created. */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDecimalFormater=new DecimalFormat("##.##");
		//Request the progress bar to be shown in the title
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);
		bindListeners();

		buildGoogleApiClient();
		//mGoogleApiClient.connect();

		/* attempt to get the download url from the server */
		try {
			download_file_url = ServerRequestor.getDownloadUrl( SERVER );
		} catch (IOException e) {
			e.printStackTrace();
		}


		//testServerRequestor();
		//System.out.println(info.toString());
		//cm.setNetworkPreference();

		/** to do *
		 * 		Make app connect to a certain wi fi network
		 * 		require app only use the wi fi
		 * 		test speed
		 */

	}

	/* get the mac address of the device
	* should be called only once
	*/
	private String getMacAddr(){
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		String mac = wInfo.getMacAddress();
		mTxtMAC.setText( mac );
		return mac;

	}

	/* get the date and time */
	private String getTimeStamp(){
		//getting current date and time using Date class
		DateFormat df = new SimpleDateFormat();
		Date dateobj = new Date();
		return df.format(dateobj);
	}



	/**
	 * Setup event handlers and bind variables to values from xml
	 */
	private void bindListeners() {
		mBtnStart = (Button) findViewById(R.id.btnStart);
		mTxtSpeed = (TextView) findViewById(R.id.speed);
		mTxtConnectionSpeed = (TextView) findViewById(R.id.connectionspeeed);
		mTxtProgress = (TextView) findViewById(R.id.progress);
		mTxtNetwork = (TextView) findViewById(R.id.networktype);
		mTxtMoreInfo = (TextView) findViewById(R.id.info);
		mTxtName = (TextView) findViewById(R.id.networkname);
		mTxtMAC = (TextView) findViewById(R.id.MACAddr);

		mBtnStart.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View view) {
				setProgressBarVisibility(true);
				mTxtSpeed.setText("Test started");	
				mBtnStart.setEnabled(false);
				mTxtNetwork.setText(R.string.network_detecting);
				mTxtMoreInfo.setText("NetworkInfo:");
				mTxtName.setText("Name:");
				new Thread(mWorker).start();
			}
		});
	}



	private final Handler mHandler=new Handler(){
		@Override
		public void handleMessage(final Message msg) {
			switch(msg.what){
			case MSG_UPDATE_STATUS:
				final SpeedInfo info1=(SpeedInfo) msg.obj;
				mTxtSpeed.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.kilobits)));
				// Title progress is in range 0..10000
				setProgress(100 * msg.arg1);
				mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg2, EXPECTED_SIZE_IN_BYTES));

				break;
			case MSG_UPDATE_CONNECTION_TIME:
				mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
				mConnectionTime = msg.arg1;
				break;				
			case MSG_COMPLETE_STATUS:

				/* initialize new hashmap to store data */
				data = new HashMap<String, String>();

				final SpeedInfo info2 = (SpeedInfo) msg.obj;
				mTxtSpeed.setText(String.format(getResources().getString(R.string.update_downloaded_complete), msg.arg1, info2.kilobits));

				mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg1, EXPECTED_SIZE_IN_BYTES));

				/* decide network type */
				ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo info = cm.getActiveNetworkInfo();
				if (info.getType() == ConnectivityManager.TYPE_WIFI){
					mTxtNetwork.setText( R.string.type_wifi );
				} else {
					mTxtNetwork.setText( R.string.type_not_wifi );
				}

				/* display further info about network */
				mTxtMoreInfo.setText(info.toString());

				/* get the network's name */
				mTxtName.setText("Name: " + info.getExtraInfo());

				mBtnStart.setEnabled(true);
				setProgressBarVisibility(false);

				/* store data in hashmap */

				/* download speed and total bytes downloaded */
				data.put("downSpeed(kbit/sec)", info2.kilobits + "");
				data.put("bytesIn", msg.arg1 + "");

				/* specify the file that was downloaded */
				data.put("file_downloaded", "");

				/* update gps location */
				updateLocation();
				data.put("latitude", mLatitude+"");
				data.put("longitude", mLongitude + "");

				/* get the time */
				data.put("timeStamp(" + TIMESTAMP_FORMAT + ")", getTimeStamp());

				/* get the mac addr */
				data.put("MACAddr", getMacAddr());

				/* name of network */
				data.put("network", info.getExtraInfo());

				/* connection time in ms */
				data.put("connectionTime(ms)", mConnectionTime + "");
				data.put("file_downloaded", download_file_url);

				/* post the data to the server */
				try {
					ServerRequestor.post( SERVER, data ) ;
				} catch (IOException e) {
					e.printStackTrace();
				}


				break;
			default:
				super.handleMessage(msg);		
			}
		}
	};

	/** get the download file's url */
	private void getUrl(){


	}

	/**
	 * Our Slave worker that does actually all the work
	 */
	private final Runnable mWorker=new Runnable(){
		
		@Override
		public void run() {
			InputStream stream=null;
			try {
				int bytesIn=0;

				/* the file to be downloaded */
				//String downloadFileUrl="http://www.kenrockwell.com/contax/images/g2/examples/31120037-5mb.jpg";
				long startCon = System.currentTimeMillis();
				URL url = new URL(download_file_url);
				URLConnection con=url.openConnection();
				con.setUseCaches(false);
				long connectionLatency = System.currentTimeMillis()- startCon;
				stream = con.getInputStream();

				EXPECTED_SIZE_IN_BYTES = con.getContentLength();

				Message msgUpdateConnection=Message.obtain(mHandler, MSG_UPDATE_CONNECTION_TIME);
				msgUpdateConnection.arg1=(int) connectionLatency;
				mHandler.sendMessage(msgUpdateConnection);

				long start=System.currentTimeMillis();
				int currentByte = 0;
				long updateStart=System.currentTimeMillis();
				long updateDelta=0;
				int  bytesInThreshold=0;


				/* get max bytes to read
				* There may not be this amount of bytes available to read there
				* are rarely more available
				*/
				int bytesToRead = 1024;
				byte[] readBytes = new byte[bytesToRead];

				while((currentByte = stream.read(readBytes))!= -1){
					//bytesIn++;
					bytesIn += currentByte;
					bytesInThreshold+=currentByte;
					if(updateDelta >= UPDATE_THRESHOLD){
						int progress=(int)((bytesIn/(double)EXPECTED_SIZE_IN_BYTES)*100);
						Message msg=Message.obtain(mHandler, MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold));
						msg.arg1=progress;
						msg.arg2=bytesIn;
						mHandler.sendMessage(msg);
						//Reset
						updateStart=System.currentTimeMillis();
						bytesInThreshold=0;

					}
					/* reassign vars */
					bytesToRead = Math.max(stream.available(), 1024);
					readBytes = new byte[bytesToRead];
					updateDelta = System.currentTimeMillis() - updateStart;
				}

				long downloadTime=(System.currentTimeMillis()-start);

				//Prevent ArithmeticException
				if(downloadTime==0){
					downloadTime=1;
				}

				Message msg=Message.obtain(mHandler, MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
				msg.arg1=bytesIn;
				mHandler.sendMessage(msg);
			} 
			catch ( MalformedURLException e ) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());

			}finally{
				try {
					if(stream!=null){
						stream.close();
					}
				} catch (IOException e) {
					//Suppressed
				}
			}

		}
	};


	/**
	 * 	
	 * 1 byte = 0.0078125 kilobits
	 * 1 kilobits = 0.0009765625 megabit
	 * 
	 * @param downloadTime in miliseconds
	 * @param bytesIn number of bytes downloaded
	 * @return SpeedInfo containing current speed
	 */
	private SpeedInfo calculate(final long downloadTime, final long bytesIn){
		SpeedInfo info=new SpeedInfo();
		//from mil to sec
		long bytespersecond   =(bytesIn / downloadTime) * 1000;
		double kilobits=bytespersecond * BYTE_TO_KILOBIT;
		double megabits=kilobits  * KILOBIT_TO_MEGABIT;
		info.downspeed=bytespersecond;
		info.kilobits=kilobits;
		info.megabits=megabits;

		return info;
	}

	/* get the most current location */
	public void updateLocation(){
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (mLastLocation != null) {
			TextView lat = (TextView) findViewById(R.id.Latitude);
			TextView lon = (TextView) findViewById(R.id.Longitude);
			mLatitude = mLastLocation.getLatitude();
			mLongitude = mLastLocation.getLongitude();
			lat.setText("Latitude: " + mLatitude + "");
			lon.setText("Longitude: " + mLongitude + "");
		} else {
			System.out.println("last location not available");
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		System.out.println("Connected!");

	}

	@Override
	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}


	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

	}

	/* build a googleApiClient */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder( this )
				.addConnectionCallbacks( this )
				.addOnConnectionFailedListener( this )
				.addApi( LocationServices.API )
				.build();
	}


	/**
	 * Transfer Object
	 * @author devil
	 *
	 */
	private static class SpeedInfo{
		public double kilobits=0;
		public double megabits=0;
		public double downspeed=0;		
	}




}