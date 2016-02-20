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
package com.xanderfehsenfeld.tigertest;

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
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Test speed of our network connection
 * @author Xander Fehsenfeld
 * @version 1.0
 *
 */
public class SpeedTestLauncher extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	/* key values for use in the 'data' hashmap, which will be sent to the server
			These values must match the values on the server end for this to properly work,
			so ANY CHANGES HERE SHOULD BE REFLECTED ON THE SERVER
	 */
	private static final String NETWORK_STRING = "network";
	private static final String LAT_STRING = "latitude";
	private static final String LONG_STRING = "longitude";
	private static final String DOWNLOAD_STRING = "fileDownloaded";
	private static final String ALT_STRING = "altitude";
	private static final String TIMESTAMP_STRING = "timeStamp";
	private static final String TIMESTAMPFMT_STRING = "timeStampFmt";
	private static final String CONNTIME_STRING = "connectionTime";
	private static final String CONNTIMEUNIT_STRING = "connectionTimeUnit";
	private static final String BYTES_STRING = "bytesIn";
	private static final String SPEED_STRING = "downSpeed";
	private static final String MAC_STRING = "MACAddr";
	private static final String ID_STRING = "uuid";


	//Private fields
	private static final String TAG = SpeedTestLauncher.class.getSimpleName();
	private static int EXPECTED_SIZE_IN_BYTES = 5 * 1000000;//5MB 1024*1024

	private static final String TIMESTAMP_FORMAT = "dd/MM/yy HH:mm:ss";
	private String SERVER;

	private String download_file_url = "http://www.smdc.army.mil/smdcphoto_gallery/Missiles/IFT_13B_Launch/IFT13b-3-02.jpg";


	//private static final double EDGE_THRESHOLD = 176.0;
	private static final double BYTE_TO_KILOBIT = 0.0078125;
	private static final double KILOBIT_TO_MEGABIT = 0.0009765625;

	private Button mBtnStart;
	private TextView mResultViewer;
	private LinearLayout mResultContainer;
	//private HorizontalScrollView mScroller;
	private ScrollView mScroller;

	/* popup window */
	PopupWindow pwindo;


	/* connection time */
	private double mConnectionTime = 0;

	/* store current location */
	private double mLatitude = 0;
	private double mLongitude = 0;
    private double mAltitude = 0;

	/* HashMap to store resultant data */
	HashMap<String, String> data;

	private final int MSG_UPDATE_STATUS=0;
	private final int MSG_UPDATE_CONNECTION_TIME=1;
	private final int MSG_COMPLETE_STATUS=2;
	private final static int UPDATE_THRESHOLD=200;

	/* location */
	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;

	/* sound effects */
	private MediaPlayer mp;
	private Vibrator v;

	/* animations */
	private Animation animationFlyIn;
	private Animation scrollerFlyIn;
	private LinkedList<String> textAnim;
    private LinkedList<HorizontalScrollView> scrollersToAnimate;
    private HorizontalScrollView network_scroller;
    private HorizontalScrollView downspeed_scroller;
    private HorizontalScrollView ping_scroller;



    private HashMap<String, Integer> textAndColor;


	private DecimalFormat mDecimalFormater;

	private boolean mRaining = false;


	private Point screenDimens;



	/** Called when the activity is first created. */
	//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* get screenDimens width and height */
		screenDimens = new Point();
		Display display = getWindowManager().getDefaultDisplay();
		display.getSize(screenDimens);


		SERVER = getString(R.string.server_address);

		mDecimalFormater=new DecimalFormat("##.##");
		//Request the progress bar to be shown in the title
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);
		bindListeners();

		buildGoogleApiClient();
		//mGoogleApiClient.connect();


		mResultViewer = (TextView)findViewById(R.id.resultviewer);
		mResultViewer.setText("");

		/* initialize the popup window */
		initPopup();

		mResultContainer = (LinearLayout) findViewById(R.id.resultContainer);
		mScroller = (ScrollView) findViewById(R.id.horizontalScrollView);

        mResultContainer.removeView(mResultViewer);



		/* data structures to keep track of text and color */
		textAnim = new LinkedList();
        scrollersToAnimate = new LinkedList<>();
		textAndColor = new HashMap<>();

		v = (Vibrator) SpeedTestLauncher.this.getSystemService(Context.VIBRATOR_SERVICE);

        scrollerFlyIn = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_PARENT, .5f, Animation.ABSOLUTE, -1000);
        scrollerFlyIn.setDuration(300);
        scrollerFlyIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                /* vibrate for 250 ms */
                v.vibrate(250);

                if (!scrollersToAnimate.isEmpty()) {

                    HorizontalScrollView last = scrollersToAnimate.pop();


                    if (!scrollersToAnimate.isEmpty()) {
                        HorizontalScrollView next = scrollersToAnimate.peekFirst();
                        next.startAnimation(scrollerFlyIn);
                    }

                    last.setVisibility(View.VISIBLE);
                    mScroller.fling(300);
                }


            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

		/* load a flyin animation to animate text views */
		animationFlyIn = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_PARENT, .5f, Animation.ABSOLUTE, -1000);
		animationFlyIn.setDuration(300);
		animationFlyIn.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!textAnim.isEmpty()) {

					/* vibrate for 250 ms */
					v.vibrate(250);

					String last = textAnim.pop();
					int c = textAndColor.get(last);


					/* pop off the next thing to be animated off the queue and animate */
					if (!textAnim.isEmpty()) {
						String next = textAnim.peekFirst();
						runOnUiThread(new UpdateUI(next, textAndColor.get(next)));
					} else {
						mRaining = false;
					}

					/* add an item to the mScroller */
					//addStringToScroller(last, c);


					mScroller.fling(300);
				}


			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

        /* add spacers to scroll view */
        populateScrollView();

	}

	/* get the mac address of the device
	* should be called only once
	*/
	private String getMacAddr(){
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		String mac = wInfo.getMacAddress();
		return mac;

	}

    /** getNewItem
     *      returns a view to put in the main scroll view
     * @param text the text in the view
     * @param c the color of the text
     * @return a new horizontal scroll view object
     */
    private HorizontalScrollView getNewItem(String text, int c){
        /* make a text view to display in horizontal mScroller */
        TextView toAdd = new TextView(SpeedTestLauncher.this);
        HorizontalScrollView hs = new HorizontalScrollView(SpeedTestLauncher.this);
        FrameLayout fl = new FrameLayout(SpeedTestLauncher.this);
        fl.addView(toAdd);
        hs.addView(fl);

        toAdd.setText(text);
        toAdd.setTextSize(mResultViewer.getTextSize());
        toAdd.setMovementMethod(new ScrollingMovementMethod());
        toAdd.setMaxLines(1);
        toAdd.setScrollbarFadingEnabled(true);
        //toAdd.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        toAdd.setTextColor(c);

		/* if it is a spacer, it will have no content */
        if( text.equals("")){
            hs.setVisibility(View.INVISIBLE);
        } else {
            toAdd.setBackgroundColor(getResources().getColor(R.color.textColorPrimary));

            /* adjust text size so it fits all in one line */
            adjustTextView(toAdd);

        }
        return hs;
    }

    /** adjustTextView
     *      adjust tje size of the text in a textview so it is the screen's width
     * @param toAdd
     */
    private void adjustTextView( TextView toAdd ){
        /* ratio of current width to screen width */
        Paint paint = toAdd.getPaint();

        String text = "" + toAdd.getText();

        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        float ratio = (float)(screenDimens.x) / r.width();
        System.out.println("text length: " + paint.measureText(text) + ", toAdd.getWidth(): " + toAdd.getWidth() + "toAdd.getMeasuredWidth(): " + toAdd.getMeasuredWidth() + ", Screen dimens x:" + screenDimens.x + ", ratio: " + ratio);


        toAdd.setTextSize(TypedValue.COMPLEX_UNIT_PX, toAdd.getTextSize() * ratio);
    }

	/**addStringToScroller
	 *      add a textview with the text and color and do a flyin animation
	 * @param text the text of the item
	 * @param c the color of the text
	 */
	private void addStringToScroller(String text, int c){

	 	HorizontalScrollView hs = getNewItem(text, c);

        addViewToScroller(hs);

		hs.startAnimation(animationFlyIn);

	}

    /** addViewToScroller
     *      add a view to the scroller after the spacers
     * @param v the view to add
     */
    private void addViewToScroller(View v){
        /* put item at top of scroller after the 5 spacers */
        if (mResultContainer.getChildCount() > 5) mResultContainer.addView(v, 6);
        else mResultContainer.addView(v);

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

		mBtnStart.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View view) {
				changeUI(1);
				/* get initial metadata and put in a hashmap */
				if ( putMetaData() ) {
					new Thread(mWorker).start();
					playSound(0);

				} else changeUI(0);


			}
		});
	}

	private int getRandomColor(){
		Random random = new Random();
		int brightness = 100;
		return Color.argb(255, brightness + random.nextInt(255 - brightness), brightness + random.nextInt(255- brightness), brightness + random.nextInt(255 - brightness));
	}

	/** get the metadata
	 *		returns whether or not to continue with the test
	 */
	private boolean putMetaData(){

		/* initialize new hashmap to store data */
		data = new HashMap<String, String>();
		textAndColor.clear();
		textAnim.clear();

		/* store data to be animated in the ui thread animator */
		//final HashMap<String,Integer> toAnimate = new HashMap<>();


		/* decide network type */
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		/* get info on the active network */
		NetworkInfo info = cm.getActiveNetworkInfo();

		/* check if wi fi is on */
		NetworkInfo mWiFi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (!mWiFi.isConnected()) {
			showPopup( "WIFI NOT CONNECTED" );
			playSound(2);
			return false;
		}


		/* store data in hashmap */

		/* specify the file that was downloaded */
		data.put(DOWNLOAD_STRING, download_file_url);

		/* update gps location */
		updateLocation();
		data.put(LAT_STRING, mLatitude+"");
		data.put(LONG_STRING, mLongitude + "");
		data.put(ALT_STRING, mAltitude + "");

		/* get the time */
		data.put(TIMESTAMP_STRING, getTimeStamp());
		data.put(TIMESTAMPFMT_STRING, TIMESTAMP_FORMAT);

		/* get the mac addr */
		data.put(MAC_STRING, getMacAddr());

		/* name of network */
		String network = info.getExtraInfo().replace('"', ' ').trim();
		data.put(NETWORK_STRING, network);

		/* prepare data for user */
		//prepareData();

		/* give the data a unique id */
		data.put(ID_STRING, UUID.randomUUID() + "");


		if (!isAllowedNetwork(network)) {
			showPopup( network );
			playSound(2);
			return false;
		}

		return true;

	}

	/** isAllowedNetwork
	 * 		tests to see if the network name is one of the allowed networks
	 * @param input the name of the network
	 * @return whether or not is allowed
	 */
	private boolean isAllowedNetwork( String input ){
		/* networks which are allowed to test */
		String[] allowedNetworks = new String[]{"WiOfTheTiger", "WiOfTheTiger-Employee", "NETGEAR64"};

		for (String network : allowedNetworks){
			if (input.equals(network)) return true;
		}

		return false;
	}

    /* add empty textviews to use as spacers */
    private void populateScrollView(){

        //add spacers
        for (int i = 0; i < 5; i ++){
            addStringToScroller("", getRandomColor());
        }

        /* initialize scrollers */
        network_scroller = getNewItem("<network scroller>", getResources().getColor(R.color.networkTextColor));
        ping_scroller = getNewItem("<ping_scroller>", getResources().getColor(R.color.PingTextColor));
        downspeed_scroller = getNewItem("<downspeed_scroller>", getResources().getColor(R.color.downSpeedTextColor));

        /* by default should be invisible */
        network_scroller.setVisibility(View.INVISIBLE);
        ping_scroller.setVisibility(View.INVISIBLE);
        downspeed_scroller.setVisibility(View.INVISIBLE);

        /* order they will appear */
        mResultContainer.addView(network_scroller);
        mResultContainer.addView(ping_scroller);
        mResultContainer.addView(downspeed_scroller);


    }

	private void changeUI( int mode ){
		if (mode == 1){
			setProgressBarVisibility(true);
			mResultViewer.setText("");

            /* remove the items from scroller that are not spacers */
            //int maxIndex = mResultContainer.getChildCount()-1;
            //int removeCount = Math.max(mResultContainer.getChildCount() - 6, 0);
			//mResultContainer.removeViews(maxIndex - removeCount + 1, removeCount);
            downspeed_scroller.setVisibility(View.INVISIBLE);
            ping_scroller.setVisibility(View.INVISIBLE);
            network_scroller.setVisibility(View.INVISIBLE);




			//if (mResultContainer.getChildCount() > 5) mResultContainer.removeViews(5, mResultContainer.getChildCount()-1);

			//mTxtSpeed.setText("Test started");
			mBtnStart.setEnabled(false);
			mBtnStart.setText("TESTING...");
			mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_pressed));

		} else if ( mode == 0){
			mBtnStart.setEnabled(true);
			mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_notpressed));
			mBtnStart.setText("START TEST");
			setProgressBarVisibility(false);
		}
	}



	private final Handler mHandler=new Handler(){
		@Override
		public void handleMessage(final Message msg) {
			switch(msg.what){
			case MSG_UPDATE_STATUS:
				final SpeedInfo info1=(SpeedInfo) msg.obj;
				//mTxtSpeed.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.kilobits)));
				// Title progress is in range 0..10000
				setProgress(100 * msg.arg1);
				//mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg2, EXPECTED_SIZE_IN_BYTES));

				break;
			case MSG_UPDATE_CONNECTION_TIME:
				//mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
				mConnectionTime = msg.arg1;
				break;				
			case MSG_COMPLETE_STATUS:


				final SpeedInfo info2 = (SpeedInfo) msg.obj;

				/* change the ui and play a sound */
				changeUI(0);
				playSound(1);

				/* store data in hashmap */

				/* download speed and total bytes downloaded */
				data.put(SPEED_STRING, info2.kilobits + "");
				data.put(BYTES_STRING, msg.arg1 + "");

				/* connection time in ms */
				data.put(CONNTIME_STRING, mConnectionTime + "");
				data.put(CONNTIMEUNIT_STRING, "ms");


				/* prepare data for user */
				prepareData();

				/* if raining animation is already started, then the above added items will rain */
				if (!mRaining) startRainAnimation();


				//mResultViewer.setText(result);

				/* post the data to the server */
				try {
                    Toast toast = Toast.makeText( getApplicationContext(), "Posting to server...", Toast.LENGTH_LONG);
                    toast.show();
                    ServerRequestor.post( SERVER, data ) ;
                    toast.setText("Post Successful!");
                    toast.show();

				} catch (IOException e) {
                    Toast toast = Toast.makeText( getApplicationContext(), "Failed to contact server", Toast.LENGTH_LONG);
                    e.printStackTrace();
                    toast.show();
                }


				break;
			default:
				super.handleMessage(msg);		
			}
		}
	};

	/** prepareData
	 * 		display select data to the user with hardcoded style
	 */

	private void prepareData(){
		//NOTE order matters! the text will appear in the scroller in the reverse order of the code

        // download speed
        String z = data.get(SPEED_STRING) + "bytes/sec";
        TextView tv;
		textAndColor.put(z, getResources().getColor(R.color.downSpeedTextColor));
		textAnim.add(z);
        tv = ((TextView)((FrameLayout) downspeed_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(downspeed_scroller);


		//connection time
		z = "Ping: " + data.get(CONNTIME_STRING) + data.get(CONNTIMEUNIT_STRING);
		textAndColor.put(z, getResources().getColor(R.color.PingTextColor));
		textAnim.add(z);
        tv = ((TextView)((FrameLayout) ping_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(ping_scroller);

		//network name
		z = data.get(NETWORK_STRING);
		textAndColor.put(z, getResources().getColor(R.color.networkTextColor));
		textAnim.add(z);
        tv = ((TextView)((FrameLayout) network_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(network_scroller);


	}


	/* animates the data in the textAnim queue*/
	private void startRainAnimation(){

        if (!scrollersToAnimate.isEmpty()){
            HorizontalScrollView first = scrollersToAnimate.peekFirst();
            first.startAnimation(scrollerFlyIn);
        }
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

	/** initPopup
	 * 		set up the popup view for later use
	 */
	private void initPopup(){
		/* get the viewGroup to be displayed on the popup */
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.popupwindow, null);

		/* make popupwindow the appropriate size */
		int width = screenDimens.x;
		int height = screenDimens.y;
		pwindo = new PopupWindow(layout, width - 100, height/ 2, true);

		/* aniamation */
		pwindo.setAnimationStyle(R.style.Animation);

		/* create onclick listener to close the popup */
		OnClickListener cl = new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				pwindo.dismiss();
				playSound(4);
			}
		};

		/* make so clicking on the popup or circular_button_background closes the window */
		Button btn_closepopup = (Button)layout.findViewById(R.id.close_popup_btn);
		btn_closepopup.setOnClickListener(cl);
		layout.setOnClickListener(cl);

		/* map the settings button */
		Button btn_settings = (Button)layout.findViewById(R.id.settings_btn);
		btn_settings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				/* go to WIFI settings */
				startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);
			}
		});
	}


	/** showPopup
	 *
	 * @param badNetwork the name of the network currently connected to
	 */
	private void showPopup( String badNetwork ){

		/* get the viewGroup to be displayed on the popup */
		RelativeLayout layout = (RelativeLayout) pwindo.getContentView();

		/* set the error message on the popup */
		TextView errorMessage = (TextView) layout.findViewById(R.id.errorMessage);
		errorMessage.setText("Invalid network: " + badNetwork + ". Valid networks: WiOfTheTiger, WiOfTheTiger-Employee.");


		/* display popup */
		pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

		playSound(3);

	}

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

			mLatitude = mLastLocation.getLatitude();
			mLongitude = mLastLocation.getLongitude();

            mAltitude = mLastLocation.getAltitude();
		} else {
			Toast toast = Toast.makeText(SpeedTestLauncher.this, getResources().getString(R.string.location_unavailable_message), Toast.LENGTH_SHORT);
			System.out.println("last location not available");
			toast.show();
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		System.out.println("Connected!");
		Toast toast = Toast.makeText(SpeedTestLauncher.this, getResources().getString(R.string.location_connected_message), Toast.LENGTH_SHORT);
		toast.show();


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

	/** playSound
	 *
	 * @param sound the number to represent the sound
	 */
	private void playSound( int sound ){
		if ( sound == 0 ){
			mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.test_started);
		} else if ( sound == 1){
			mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.test_complete);
		} else if (sound == 2){
			mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.error);
		} else if (sound == 3){
			mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.popup_show);

		} else if (sound == 4){
			mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.popup_hide);
		}
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mp.start();
	}

	/* a e to update ui */
	class UpdateUI implements Runnable
	{
		String updateString;
		//HashMap<String,Integer> toAnimate;
		int color;

		public UpdateUI(String updateString, int color) {

			this.updateString = updateString;
			this.color = color;
		}
		public void run() {

			addStringToScroller(updateString, color);

		}
	}


}