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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;


import com.xanderfehsenfeld.tigertest.LocalDB.DatabaseManagerService;
import com.xanderfehsenfeld.tigertest.LocalDB.FeedReaderDbHelper;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.xanderfehsenfeld.tigertest.GPS.GPSTracker;
import com.xanderfehsenfeld.tigertest.LocalDB.MyDbWrapper;

/**
 * Test speed of our network connection
 * @author Xander Fehsenfeld
 * @version 1.0
 *
 */
public class SpeedTestLauncher extends Activity {

    /* contants for the method 'playsound' */
    public static final int SOUND_TEST_STARTED = 0;
    public static final int SOUND_TEST_COMPLETE = 1;
    public static final int SOUND_ERROR = 2;
    public static final int SOUND_POPUP_SHOW = 3;
    public static final int SOUND_POPUP_HIDE = 4;


	//Private fields
	public static final String TAG = SpeedTestLauncher.class.getSimpleName();
	private static final String DB_TAG = "DB";
	private static final String LOCATION_SERVICES_TAG = "Location Service";

	protected static int EXPECTED_SIZE_IN_BYTES = 5 * 1000000;//5MB 1024*1024

	private static final String TIMESTAMP_FORMAT = "dd/MM/yy HH:mm:ss";
	public static String SERVER_URL;

	protected String download_file_url = "http://www.smdc.army.mil/smdcphoto_gallery/Missiles/IFT_13B_Launch/IFT13b-3-02.jpg";


	//private static final double EDGE_THRESHOLD = 176.0;


	protected Button mBtnStart;
	private TextView mResultViewer;
	private LinearLayout mResultContainer;
	//private HorizontalScrollView mScroller;
	private ScrollView mScroller;
	private ScrollView mTopScroller;

    private RelativeLayout mStartBtnContainer;
    private float ratioHeightBtnParent;
    private float ratioHeightSpacer;

    /* popup window */
	PopupWindow pwindo;


	/* connection time */
	private double mConnectionTime = 0;

	/* store current location */
	protected double mLatitude = 0;
	protected double mLongitude = 0;
    protected double mAltitude = 0;

	/* HashMap to store resultant data */
	HashMap<String, String> data;

	protected final int MSG_UPDATE_STATUS=0;
	protected final int MSG_UPDATE_CONNECTION_TIME=1;
	protected final int MSG_COMPLETE_STATUS=2;
	protected final static int UPDATE_THRESHOLD=200;

	/* location */
	private GoogleApiClient mGoogleApiClient;
	protected Location mLastLocation;

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
    private HorizontalScrollView mSettingsBtnScroller;

    /* progress bar */
    private ProgressBar mCustomProgressBar;


    private HashMap<String, Integer> textAndColor;


	private DecimalFormat mDecimalFormater;

	protected boolean mRaining = false;

    /* store the screen dimensions */
	private Point screenDimens;
    private Animation animFadeIn;
    private Animation animFadeOut;


    /* for binding with service */
    private int mBindFlag;
    protected Messenger mServiceMessenger;

    /* time limit of test in seconds */
    public int timeLimit = 5;

    /* A service connection to connect this Activity with the speech recognition service */
    final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public static final boolean DEBUG = true;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            if (DEBUG) {Log.d(TAG, "onServiceConnected");} //$NON-NLS-1$
            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            //msg.what = MyGPSLocationService.MSG_RECOGNIZER_START_LISTENING;

            try
            {
                //mServerMessenger.send(msg);
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            if (DEBUG) {Log.d(TAG, "onServiceDisconnected");} //$NON-NLS-1$
            mServiceMessenger = null;
        }

    }; // mServiceConnection

    MyResultReceiver resultReceiver;

    GPSTracker mGPSTRacker;

    /* local data base */
    MyDbWrapper db;
    private FeedReaderDbHelper mDbHelper;

    protected CountDownTimer mCountDownTimer;
    private Button mBtnSettings;
    private PopupWindow settingsPwindo;

    /* whether or not to test again after complete testing */
	protected boolean isContinuous = false;


    /** Called when the activity is first created. */
	//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


        /* setup result reciever */
        resultReceiver = new MyResultReceiver(null);

		/* get screenDimens width and height */
		screenDimens = new Point();
		Display display = getWindowManager().getDefaultDisplay();
		display.getSize(screenDimens);

        /* control look of layout using ratio of screen height */
        ratioHeightBtnParent = .8f;
        ratioHeightSpacer = .75f * (1 - ratioHeightBtnParent);

		SERVER_URL = getString(R.string.server_address);

		mDecimalFormater=new DecimalFormat("##.##");
        //Request the progress bar to be shown in the title
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);

		mResultViewer = (TextView)findViewById(R.id.resultviewer);
		mResultViewer.setText("");

		/* initialize the popup window */
		initPopup();
        initSettingsPopup();

		mResultContainer = (LinearLayout) findViewById(R.id.resultContainer);
		mScroller = (ScrollView) findViewById(R.id.horizontalScrollView);
        mStartBtnContainer = (RelativeLayout) findViewById(R.id.topContainerA);
        //RelativeLayout mBottomContainer = (RelativeLayout) findViewById(R.id.start_btn_parent);

        /* top scroll view */
        mTopScroller = (ScrollView) findViewById(R.id.topScrollView);
        ViewGroup temp = (ViewGroup)(mTopScroller.getChildAt(0));
        temp.setMinimumHeight(2 * screenDimens.y);
        (temp.getChildAt(0)).setMinimumHeight( screenDimens.y);


        /* initialize custom progeress bar */
        mCustomProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
        findViewById(R.id.progress_bar_container).setMinimumHeight((int) (ratioHeightSpacer * screenDimens.y));

        /* resize start button */
        int radius = (int) (screenDimens.x/4f);
        Button b = ((Button)findViewById(R.id.btnStart));
        b.setWidth(radius * 2);
        b.setHeight(radius * 2);

        /* adjust text size */
        Paint paint = b.getPaint();
        b.setMaxLines(1);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        String text = "" + b.getText();
        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        float ratio = (float)( radius * 1.5 ) / r.width();
        b.setTextSize(TypedValue.COMPLEX_UNIT_PX, b.getTextSize() * ratio);


        (findViewById(R.id.btn_settings_scroller)).setMinimumWidth(screenDimens.x * 2);
        (findViewById(R.id.btn_settings_container)).setMinimumWidth(screenDimens.x * 2);
        mBtnSettings = (Button)findViewById(R.id.btn_settings);
        mBtnSettings.setWidth(screenDimens.x / 3);
        mBtnSettings.setMinimumHeight(screenDimens.y / 5);

        mResultContainer.removeView(mResultViewer);

        bindListeners();


        /* initialize all animations and associated listeners */
        initAnimations();


        /* add spacers to scroll view */
        populateScrollView();

//        RelativeLayout spacer = new RelativeLayout(SpeedTestLauncher.this);
//        spacer.setMinimumHeight( screenDimens.y);
//        mResultContainer.addView(spacer);

        /* make start button parent a certain size */
        //mBottomContainer.setMinimumHeight((int) (1.1 * ratioHeightBtnParent * screenDimens.y));
        mResultContainer.setMinimumHeight((int) (1.5 * screenDimens.y));

        /* set ui to default not testing mode */
        changeUI(0);

        mGPSTRacker = new GPSTracker(this);


        /* get a database helper */
        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        // Gets the data repository in write mode
        db = new MyDbWrapper(mDbHelper.getWritableDatabase());

	}

	protected final Handler mHandler=new Handler(){
		@Override
		public void handleMessage(final Message msg) {
			switch(msg.what){
			case MSG_UPDATE_STATUS:
				final SpeedTester.SpeedInfo info1=(SpeedTester.SpeedInfo) msg.obj;
				//mTxtSpeed.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.kilobits)));
				// Title progress is in range 0..10000
				setProgress(100 * msg.arg1);
                mCustomProgressBar.setProgress(100 * msg.arg1);
				//mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg2, EXPECTED_SIZE_IN_BYTES));

				break;
			case MSG_UPDATE_CONNECTION_TIME:
				//mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
				mConnectionTime = msg.arg1;
				break;
			case MSG_COMPLETE_STATUS:


				final SpeedTester.SpeedInfo info2 = (SpeedTester.SpeedInfo) msg.obj;

                /* play a sound depending on whether complete or not */
                if ( msg.arg2 == 1){
                    playSound(1);
                } else if (msg.arg2 == 0){
                    playSound(2);
                }

				/* change the ui and play a sound */
				changeUI(0);

				/* store data in hashmap */

				/* download speed and total bytes downloaded */
				data.put(FeedReaderDbHelper.SPEED_STRING, info2.kilobits + "");
				data.put(FeedReaderDbHelper.BYTES_STRING, msg.arg1 + "");

				/* connection time in ms */
				data.put(FeedReaderDbHelper.CONNTIME_STRING, mConnectionTime + "");
				data.put(FeedReaderDbHelper.CONNTIMEUNIT_STRING, "ms");


				/* prepare data for user */
				prepareData();

				/* if raining animation is already started, then the above added items will rain */
				if (!mRaining) startRainAnimation();


				//mResultViewer.setText(result);

                /* store data in db */
                db.saveRecord(data);
                String uuid = data.get(FeedReaderDbHelper.ID_STRING);
                //removeRecord(uuid);

                /* tell the data base service a record was added */
                Message message = Message.obtain(null, DatabaseManagerService.MSG_RECORD_ADDED);
                try {
                    mServiceMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

				/* post the data to the server */
                String response = "";
//				try {
//                    Toast toast = Toast.makeText( getApplicationContext(), "Posting to server...", Toast.LENGTH_LONG);
//                    toast.show();
//                    response = ServerRequestor.post(SERVER_URL, data, db ) ;
//                    toast.setText("Post Successful!");
//                    toast.show();
//
//				} catch (IOException e) {
//                    Toast toast = Toast.makeText( getApplicationContext(), "Failed to contact server", Toast.LENGTH_LONG);
//                    e.printStackTrace();
//                    toast.show();
//                }

                /* on continuous mode, start the test again */
                if (isContinuous) mBtnStart.performClick();



				break;
			default:
				super.handleMessage(msg);
			}
		}
	};



    /* VISUAL UI CODE */

    /**initSettingsPopup
     *      initializes the popup for settings
     */
    private void initSettingsPopup(){
        /* get the viewGroup to be displayed on the popup */
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.settingspopupwindow, null);


        ViewGroup mSettingsBody = (ViewGroup) layout.findViewById(R.id.settings_body_container);
        ToggleButton mSwitchContinuous = (ToggleButton) mSettingsBody.findViewById(R.id.switch_continuous);
        TextView records = (TextView) mSettingsBody.findViewById(R.id.records_in_db);

        /* set settings popup to 2/3 height of screen */
        ((ViewGroup) layout.findViewById(R.id.setting_layout_container)).setMinimumHeight((int) (screenDimens.y * .75));

        /* make settings body extra long so it can scroll */
        mSettingsBody.setMinimumHeight((int) (screenDimens.y ) );


        mSwitchContinuous.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            private final String NOTIFY_CONTINOUS_ON = "Mode: CONTINUOUS test";
            private final String NOTIFY_CONTINOUS_OFF = "Mode: SINGLE test";

            Toast t = Toast.makeText(SpeedTestLauncher.this, "", Toast.LENGTH_SHORT);

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    isContinuous = true;
                    playSound(3);
                    t.setText(NOTIFY_CONTINOUS_ON);
                } else {
                    isContinuous = false;
                    playSound(4);
                    t.setText(NOTIFY_CONTINOUS_OFF);

                }

                t.show();
            }
        });

        /* set up seekbar */
        final TextView mTimeLimitTv = (TextView) mSettingsBody.findViewById(R.id.time_limit_tv);
        SeekBar mTimeLimitSb = (SeekBar) mSettingsBody.findViewById(R.id.time_limit_seekbar);
        mTimeLimitSb.setProgress(timeLimit);
        mTimeLimitTv.setText("Test Time Limit: " + mTimeLimitSb.getProgress() + " seconds");

        mTimeLimitSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) seekBar.setProgress(1);
                mTimeLimitTv.setText("Test Time Limit: " + seekBar.getProgress() + "seconds");
                timeLimit = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /* make errorpopupwindow the appropriate size */
        int width = screenDimens.x;
        int height = screenDimens.y;
        settingsPwindo = new PopupWindow(layout, width - 100, height/ 2, true);
        //settingsPwindo = new PopupWindow(layout, width, height, true);


        /* set scroller to halfway */
        ScrollView mSettingsScroller = (ScrollView) settingsPwindo.getContentView().findViewById(R.id.settings_scroller);
        mSettingsScroller.setMinimumHeight((int) (screenDimens.y / 2));
        mSettingsScroller.scrollTo(mSettingsScroller.getScrollX(), mSettingsScroller.getMaxScrollAmount() / 2);


        /* aniamation */
        settingsPwindo.setAnimationStyle(R.style.Animation);

        /* create onclick listener to close the popup */
        OnClickListener cl = new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                settingsPwindo.dismiss();
            }
        };

		/* make so clicking on the popup or circular_button_background closes the window */
        layout.setOnClickListener(cl);
    }

    /** initPopup
     * 		set up the popup view for later use
     */
    private void initPopup(){
		/* get the viewGroup to be displayed on the popup */
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.errorpopupwindow, null);

        RelativeLayout settingsContainer = (RelativeLayout) layout.findViewById(R.id.setting_layout_container);

		/* make errorpopupwindow the appropriate size */
        int width = screenDimens.x;
        int height = screenDimens.y;
        pwindo = new PopupWindow(layout, width - 100, (int) (height/ 1.5), true);


		/* aniamation */
        pwindo.setAnimationStyle(R.style.Animation);

		/* create onclick listener to close the popup */
        OnClickListener cl = new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                pwindo.dismiss();
            }
        };

		/* make so clicking on the popup or circular_button_background closes the window */
        Button btn_closepopup = (Button)layout.findViewById(R.id.close_popup_btn);
        btn_closepopup.setOnClickListener(cl);
        layout.setOnClickListener(cl);

		/* map the settings button */
        Button btn_settings = (Button) layout.findViewById(R.id.settings_btn);
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
    private void showPopup( String badNetwork, int mode ){

	    /* get the viewGroup to be displayed on the popup */
        RelativeLayout layout = (RelativeLayout) pwindo.getContentView();

        /* set the error message on the popup */
        TextView errorMessage = (TextView) layout.findViewById(R.id.errorMessage);

        /* grey out background */
        findViewById(R.id.grey_out).setVisibility(View.VISIBLE);

        if ( mode == 0 ) {

            errorMessage.setText("Invalid network: " + badNetwork + ". Valid networks: WiOfTheTiger, WiOfTheTiger-Employee.");

            /* display popup + play sound*/
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } else if ( mode == 1 ){
            errorMessage.setText("TEST TIMED OUT. The test did not complete before the time limit. Go to settings to change time limit. Note: data will still be sent to server");
            Button wifiSettings = (Button)layout.findViewById(R.id.settings_btn);
            wifiSettings.setVisibility(View.GONE);

            /* display popup + play sound*/
            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } else if (mode == 2){
            layout = (RelativeLayout) settingsPwindo.getContentView();
            TextView records = (TextView) layout.findViewById(R.id.records_in_db);
            records.setText("# RECORDS IN LOCAL DB: " + db.getRecordCount());
            settingsPwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

        }


        playSound(3);

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
        RelativeLayout rl = new RelativeLayout(SpeedTestLauncher.this);
        //rl.setGravity(Gravity.CENTER);
        rl.addView(toAdd);
        hs.addView(rl);


        /* set a minimum height */
        hs.setMinimumHeight((int) (ratioHeightSpacer * screenDimens.y));

        toAdd.setGravity(Gravity.CENTER);
        toAdd.setText(text);
        toAdd.setTextSize(mResultViewer.getTextSize());
        toAdd.setMovementMethod(new ScrollingMovementMethod());
        toAdd.setMaxLines(1);
        toAdd.setScrollbarFadingEnabled(true);
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
        float ratio = (float)(screenDimens.x * .8) / r.width();
        toAdd.setTextSize(TypedValue.COMPLEX_UNIT_PX, toAdd.getTextSize() * ratio);
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

        // get the MAC address of the router
        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        //String wirelessNetworkName = wifiInfo.getSSID();
        String wirelessAccessPtMACAddr = wifiInfo.getBSSID();
        data.put(FeedReaderDbHelper.ACCESSPT_STRING, wirelessAccessPtMACAddr);


		/* decide network type */
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		/* get info on the active network */
        NetworkInfo info = cm.getActiveNetworkInfo();

		/* check if wi fi is on */
        NetworkInfo mWiFi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!mWiFi.isConnected()) {
            showPopup( "WIFI NOT CONNECTED", 0 );
            playSound(2);
            return false;
        }


		/* store data in hashmap */

		/* specify the file that was downloaded */
        data.put(FeedReaderDbHelper.DOWNLOAD_STRING, download_file_url);

		/* update gps location */
        updateLocation();
        data.put(FeedReaderDbHelper.LAT_STRING, mLatitude + "");
        data.put(FeedReaderDbHelper.LONG_STRING, mLongitude + "");
        data.put(FeedReaderDbHelper.ALT_STRING, mAltitude + "");

		/* get the time */
        data.put(FeedReaderDbHelper.TIMESTAMP_STRING, getTimeStamp());
        data.put(FeedReaderDbHelper.TIMESTAMPFMT_STRING, TIMESTAMP_FORMAT);

		/* get the mac addr */
        data.put(FeedReaderDbHelper.MAC_STRING, getMacAddr());

		/* name of network */
        String network = info.getExtraInfo().replace('"', ' ').trim();
        data.put(FeedReaderDbHelper.NETWORK_STRING, network);




		/* prepare data for user */
        //prepareData();

        /* store the location provider (either network or gps) */
        data.put( FeedReaderDbHelper.LOCATIONPROV_STRING, mLastLocation.getProvider());

		/* give the data a unique id */
        data.put(FeedReaderDbHelper.ID_STRING, UUID.randomUUID() + "");


        if (!isAllowedNetwork(network)) {
            showPopup( network, 0 );
            playSound(2);
            return false;
        }

        return true;

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



    /* get the date and time */
    private String getTimeStamp(){
        //getting current date and time using Date class
        DateFormat df = new SimpleDateFormat();
        Date dateobj = new Date();
        return df.format(dateobj);
    }


    /**
     * Our Slave worker that does actually all the work
     */
    private final SpeedTester mSpeedTester = new SpeedTester(this);



    /* get the most current location */
    public void updateLocation(){
        mLastLocation = mGPSTRacker.getLocation();

        Log.d(TAG, "updateLocation. " + "lat: " + mLastLocation.getLatitude() + ", " +
                "lon: " + mLastLocation.getLongitude() + " prov: " +
                mLastLocation.getProvider() + ", acc: " + mLastLocation.getAccuracy());

        mLatitude = mLastLocation.getLatitude();
        mLongitude = mLastLocation.getLongitude();
        mAltitude = mLastLocation.getAltitude();


    }



    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();

        startMyService();
        bindMyService();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        super.onStop();
        unbindMyService();
        stopMyService();



    }

    /* for binding to the service */
    /* Bind the the speech recog service */
    private void bindMyService(){
        Intent i = new Intent(this, DatabaseManagerService.class);
        i.putExtra("receiver", resultReceiver);

        bindService(i, mServiceConnection, mBindFlag);
    }
    /* unBind the the speech recog service */
    private void unbindMyService(){
        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
    }

    /* stop and unbind with the service */
    private void stopMyService(){

        Intent service = new Intent(SpeedTestLauncher.this, DatabaseManagerService.class);
        SpeedTestLauncher.this.stopService(service);
    }

    /* start the continuous speech service and bind to it */
    private void startMyService(){

        /* start the service */
        Intent service = new Intent(SpeedTestLauncher.this, DatabaseManagerService.class);
        /* send the reciever to the service */
        service.putExtra("receiver", resultReceiver);
        //service.putExtra("score", currentScore);
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;
        SpeedTestLauncher.this.startService(service);

        /* bind to the service */
        //bindToService();

    }


    /* update ui with results
    */
    class MyResultReceiver extends ResultReceiver
    {
        public MyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if(resultCode == 100){
                //runOnUiThread(new UpdateUI(resultData.getString("start")));
            }
            else if(resultCode == 200){
                //runOnUiThread(new UpdateUI(resultData.getString("end")));
            }
            else{
                //currentScore = resultData.getInt("score");
                //badWordQeue.addAll( resultData.getStringArrayList("badwords") );
                //runOnUiThread(new UpdateUI(resultData.getInt("score") + ""));
            }
        }
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

    /**
     * Setup event handlers and bind variables to values from xml
     */
    private void bindListeners() {
        mBtnStart = (Button) mStartBtnContainer.findViewById(R.id.btnStart);

        mBtnStart.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(final View view) {
                changeUI(1);
				/* get initial metadata and put in a hashmap */
                if ( putMetaData() ) {

                    final Thread workerThread = new Thread(mSpeedTester);
                    workerThread .start();

                    /* make the thread timeout after a certain time */
                    mCountDownTimer = new CountDownTimer(1000 * timeLimit, 1000)
                    {
                        @Override
                        public void onTick(long millisUntilFinished)
                        {}

                        @Override
                        public void onFinish()
                        {
                            workerThread.interrupt();
                            Log.d(TAG, "countdown finished"); //$NON-NLS-1$

                            if (!isContinuous) {
                                showPopup("", 1);
                            } else {
                                Toast t = Toast.makeText(SpeedTestLauncher.this, "Test timed out.", Toast.LENGTH_SHORT);
                                t.show();
                            }
                            //playSound(2);
                        }
                    };
                    mCountDownTimer.start();



                    playSound(0);

                } else changeUI(0);


            }
        });

        /* set up listener for settings button */
        mBtnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup("", 2);

                /* click the continuous toggle if it is on */
                ToggleButton mBtnContinous = (ToggleButton) settingsPwindo.getContentView().findViewById(R.id.switch_continuous);
                if (mBtnContinous != null && isContinuous) {
                    mBtnContinous.performClick();
                }
            }
        });

        /* do stuff when popups dismissed */
        PopupWindow.OnDismissListener myDismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                findViewById(R.id.grey_out).setVisibility(View.GONE);
                playSound(4);
            }
        };
        pwindo.setOnDismissListener(myDismissListener);
        settingsPwindo.setOnDismissListener(myDismissListener);



    }

    /* add empty textviews to use as spacers */
    private void populateScrollView(){


        /* initialize scrollers */
        network_scroller = getNewItem("<network scroller>", getResources().getColor(R.color.networkTextColor));
        ping_scroller = getNewItem("<ping_scroller>", getResources().getColor(R.color.PingTextColor));
        downspeed_scroller = getNewItem("<downspeed_scroller>", getResources().getColor(R.color.downSpeedTextColor));

        /* labels */
        final TextView label_network = getLabel("<network>");
        final TextView label_speed = getLabel("<downspeed (bytes per sec)");
        final TextView label_ping = getLabel("<connection latency (ms)>");

        /* by default should be invisible */
        network_scroller.setVisibility(View.INVISIBLE);
        ping_scroller.setVisibility(View.INVISIBLE);
        downspeed_scroller.setVisibility(View.INVISIBLE);

        /* order they will appear */
        mResultContainer.addView(label_network);
        mResultContainer.addView(network_scroller);
        mResultContainer.addView(label_ping);
        mResultContainer.addView(ping_scroller);
        mResultContainer.addView(label_speed);
        mResultContainer.addView(downspeed_scroller);

        //mTopScroller.setSmoothScrollingEnabled(true);
        mSettingsBtnScroller = (HorizontalScrollView)findViewById(R.id.btn_settings_scroller);

        mTopScroller.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

            int lastScrollY = mTopScroller.getScrollY();

            @Override
            public void onScrollChanged() {

                int deltaY = mTopScroller.getScrollY() - lastScrollY;
                lastScrollY = mTopScroller.getScrollY();
                mTopScroller.scrollTo(mTopScroller.getScrollX(), mTopScroller.getScrollY() + deltaY);
                mScroller.scrollTo(mScroller.getScrollX(), mScroller.getScrollY() + deltaY);

                /* percent of the whole scoller scrolled */
                /* expand views apart as you scroll up */
                //float percent_scrolled = (float)deltaY / (float)mScroller.getMaxScrollAmount();
                float percent_scrolled = (float) mTopScroller.getScrollY() / (float) mTopScroller.getMaxScrollAmount();
                int goalHeight = (int) ((float) screenDimens.y / 5f);
                float multiplier = goalHeight - (screenDimens.y * ratioHeightSpacer);
                int newminHeight = (int) ((screenDimens.y * ratioHeightSpacer) + (percent_scrolled * multiplier));

                /* scroll settings button in depending on location of start button */
                mSettingsBtnScroller.scrollTo((int) (percent_scrolled * mSettingsBtnScroller.getMaxScrollAmount()), mSettingsBtnScroller.getScrollY());


                downspeed_scroller.setMinimumHeight(newminHeight);
                network_scroller.setMinimumHeight(newminHeight);
                ping_scroller.setMinimumHeight(newminHeight);

                ArrayList<View> labels = new ArrayList<View>(){{
                    add(label_network);
                    add(label_ping);
                    add(label_speed);
                }};
                percent_scrolled = percent_scrolled / 2;
                float visibility_threshold = (float) .9;
                for ( View v : labels) {
                    if (percent_scrolled > visibility_threshold && (v.getVisibility() == View.INVISIBLE)){
                        v.setVisibility(View.VISIBLE);
                        v.startAnimation(animFadeIn);
                    } else if (percent_scrolled < visibility_threshold && (v.getVisibility() == View.VISIBLE)){
                        v.setVisibility(View.INVISIBLE);
                        v.startAnimation(animFadeOut);

                    }
                }




                //int scrollY = mTopScroller.getScrollY(); //for verticalScrollView

                //Log.d(TAG, "onScrollChanged: " + scrollY);
                //DO SOMETHING WITH THE SCROLL COORDINATES

            }
        });


    }
    private void changeUI( int mode ){
        if (mode == 1){
            setProgressBarVisibility(true);
            mCustomProgressBar.startAnimation(animFadeIn);
            mCustomProgressBar.setVisibility(View.VISIBLE);
            mResultViewer.setText("");

            /* remove the items from scroller that are not spacers */
            //int maxIndex = mResultContainer.getChildCount()-1;
            //int removeCount = Math.max(mResultContainer.getChildCount() - 6, 0);
            //mResultContainer.removeViews(maxIndex - removeCount + 1, removeCount);
            downspeed_scroller.setVisibility(View.INVISIBLE);
            ping_scroller.setVisibility(View.INVISIBLE);
            network_scroller.setVisibility(View.INVISIBLE);

            /* disable slider during test */
            ((SeekBar)settingsPwindo.getContentView().findViewById(R.id.time_limit_seekbar)).setEnabled(false);




            //if (mResultContainer.getChildCount() > 5) mResultContainer.removeViews(5, mResultContainer.getChildCount()-1);

            //mTxtSpeed.setText("Test started");
            mBtnStart.setEnabled(false);
            mBtnStart.setText("TESTING...");
            mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_pressed));

        } else if ( mode == 0){
            mCustomProgressBar.startAnimation(animFadeOut);
            mCustomProgressBar.setVisibility(View.INVISIBLE);
            mBtnStart.setEnabled(true);
            mBtnStart.setBackground(getResources().getDrawable(R.drawable.circular_button_notpressed));
            mBtnStart.setText("START TEST");
            setProgressBarVisibility(false);

            /* re-enable slider */
            ((SeekBar)settingsPwindo.getContentView().findViewById(R.id.time_limit_seekbar)).setEnabled(true);

        }
    }

    /** prepareData
     * 		display select data to the user with hardcoded style
     */

	void prepareData(){
        //NOTE order matters! the text will appear in the scroller in the reverse order of the code

        // download speed
        String z = data.get(FeedReaderDbHelper.SPEED_STRING);
        TextView tv;
        textAndColor.put(z, getResources().getColor(R.color.downSpeedTextColor));
        textAnim.add(z);
        tv = ((TextView)((RelativeLayout) downspeed_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(downspeed_scroller);


        //connection time
        z = data.get(FeedReaderDbHelper.CONNTIME_STRING);
        textAndColor.put(z, getResources().getColor(R.color.PingTextColor));
        textAnim.add(z);
        tv = ((TextView)((RelativeLayout) ping_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(ping_scroller);

        //network name
        z = data.get(FeedReaderDbHelper.NETWORK_STRING);
        textAndColor.put(z, getResources().getColor(R.color.networkTextColor));
        textAnim.add(z);
        tv = ((TextView)((RelativeLayout) network_scroller.getChildAt(0)).getChildAt(0));
        tv.setText(z);
        adjustTextView(tv);
        scrollersToAnimate.add(network_scroller);


    }


    /* animates the data in the textAnim queue*/
	void startRainAnimation(){

        if (!scrollersToAnimate.isEmpty()){
            HorizontalScrollView first = scrollersToAnimate.peekFirst();
            runOnUiThread(new UpdateUI(first));
        }
    }

    private TextView getLabel( String content ){
        final TextView label = new TextView(SpeedTestLauncher.this);
        label.setText(content);
        label.setTextColor(getResources().getColor(R.color.bloodRed));
        label.setVisibility(View.INVISIBLE);
        //label.setAlpha(0);
        return label;
    }


    /** playSound
     *
     * @param sound the number to represent the sound
     */
    private void playSound( int sound ){
        if ( sound == SOUND_TEST_STARTED ){
            mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.test_started);
        } else if ( sound == SOUND_TEST_COMPLETE){
            mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.test_complete);
        } else if (sound == SOUND_ERROR){
            mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.error);
        } else if (sound == SOUND_POPUP_SHOW ){
            mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.popup_show);

        } else if (sound == SOUND_POPUP_HIDE){
            mp = MediaPlayer.create(SpeedTestLauncher.this, R.raw.popup_hide);
        }
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.start();
    }

    /** UpdateUI
     *      a runnable to perform a scrollview animation (usually called to operate on the UI thread )
     */
    class UpdateUI implements Runnable
    {
        HorizontalScrollView horizontalScrollView;

        public UpdateUI(HorizontalScrollView horizontalScrollView) {

            this.horizontalScrollView = horizontalScrollView;
        }
        public void run() {

            horizontalScrollView.startAnimation(scrollerFlyIn);
        }
    }


    private void initAnimations(){

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
                        //next.startAnimation(scrollerFlyIn);
                        runOnUiThread(new UpdateUI(next));
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

        /* fade in and fade out animations */
        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
    }


}