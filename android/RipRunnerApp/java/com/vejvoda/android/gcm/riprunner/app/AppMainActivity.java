/*
 * Copyright 2014 Mark Vejvoda
 * Under GNU GPL v3.0
 */
package com.vejvoda.android.gcm.riprunner.app;

import com.vejvoda.android.gcm.riprunner.app.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;

/**
 * Main UI for the Rip Runner Android app.
 */
public class AppMainActivity extends ActionBarActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, 
		GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,		
		LocationListener {

	// Property names to store app settings
    public static final String PROPERTY_REG_ID 				= "registration_id";
        
    public static final String PROPERTY_WEBSITE_URL 		= "host_url";
    public static final String PROPERTY_SENDER_ID 			= "sender_id";
    public static final String PROPERTY_TRACKING_ENABLED 	= "tracking_enabled";
    
    public static final String PROPERTY_FIREHALL_ID 		= "firehall_id";
    public static final String PROPERTY_USER_ID 			= "user_id";
    
    private static final String PROPERTY_APP_VERSION 		= "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * This enum describes possible callout statuses
     * @author softcoder
     *
     */
    public enum CalloutStatusType {
		Paged(0),
		Notified(1),
		Responding(2),
		Cancelled(3),
		Complete(10);
		
		private int value;

        private CalloutStatusType(int value) {
                this.value = value;
        }
        public int valueOf() {
            return this.value;
        }
        public boolean isComplete() {
    		return (this.value == CalloutStatusType.Cancelled.valueOf() ||
    				this.value == CalloutStatusType.Complete.valueOf());
        	
        }
        static public boolean isComplete(String status) {
    		return (status != null &&
    				(status.equals(String.valueOf(CalloutStatusType.Cancelled.valueOf())) ||
    				(status.equals(String.valueOf(CalloutStatusType.Complete.valueOf())))));
        	
        }
    };

    TextView mDisplay = null;
    GoogleCloudMessaging gcm = null;
    Context context = null;
    MenuItem logout_menu = null;
    ProgressDialog loadingDlg = null;
    		
    /**
     * This class contains a wrapper for accessing predefined sounds
     * @author softcoder
     */
	public class FireHallSoundPlayer {
	     public static final int SOUND_DINGLING = R.raw.dingling;
	     public static final int SOUND_LOGIN = R.raw.login;
	     public static final int SOUND_PAGE1 = R.raw.page1;
	     public static final int SOUND_PAGER_TONE_PG = R.raw.pager_tone_pg;
    }
	
    private static SoundPool soundPool;
    private static Map<Integer,Integer> soundPoolMap;

    /** The authentication object */
    FireHallAuthentication auth;
    /** The last callout information */
    FireHallCallout lastCallout;
    
    // Your activity will respond to this action String
    public static final String RECEIVE_CALLOUT = "callout_data";
    
    public static final String TRACKING_GEO = "tracking_data";
    
    /** The broadcast receiver class for getting broadcast messages */
    private BroadcastReceiver bReceiver = null;
    
    // The location client that receives GPS location updates
    //LocationClient mLocationClient = null;
    private GoogleApiClient mGoogleApiClient = null;
    
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    
    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    Location lastTrackedGEOLocation = null;
    boolean mUpdatesRequested = true;
    
    PendingIntent geoTrackingIntent = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(Utils.TAG, Utils.getLineNumber() + ": Starting up Rip Runner.");
        
        setContentView(R.layout.main);
        mDisplay = (TextView) findViewById(R.id.display);
        context = getApplicationContext();
        initSounds(context);
        
        AppMainBroadcastReceiver.setMainApp(this);
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_CALLOUT);
        intentFilter.addAction(TRACKING_GEO);
        bManager.registerReceiver(getBroadCastReceiver(), intentFilter);
        
        getProgressDialog();
        
    	final EditText etFhid = (EditText)findViewById(R.id.etFhid);
    	final EditText etUid = (EditText)findViewById(R.id.etUid);
    	EditText etUpw = (EditText)findViewById(R.id.etUpw);
    	
    	etFhid.setSelectAllOnFocus(true);
    	etUid.setSelectAllOnFocus(true);
        setupLoginUI();

        boolean focusPWd = false;
        if (checkPlayServices()) {
        	setupGPSTracking();
        	
        	if(hasConfigItem(context,PROPERTY_WEBSITE_URL,String.class) && 
        			hasConfigItem(context,PROPERTY_SENDER_ID,String.class) &&
        			hasConfigItem(context,PROPERTY_TRACKING_ENABLED,Boolean.class)) {
        		
	            etFhid.setText(getConfigItem(context,PROPERTY_FIREHALL_ID,String.class));
	            etUid.setText(getConfigItem(context,PROPERTY_USER_ID,String.class));
	            
	            startGEOAlarm();
	            
	            focusPWd = true;
        	}
        	else {
        		openSettings();
        	}
	        
	        etUpw.setText("");
        } 
        else {
            Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
        }
        
        etUid.requestFocus();
        etFhid.requestFocus();
        if(focusPWd) {
        	etUpw.requestFocus();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Saving instance state for Rip Runner.");
    	
        // Save the user's current game state
        //savedInstanceState.putInt(STATE_SCORE, mCurrentScore);
        //savedInstanceState.putInt(STATE_LEVEL, mCurrentLevel);
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    PendingIntent getGeoTrackingIntent() {
    	if(geoTrackingIntent == null) {
	    	//Intent intent = new Intent( TRACKING_GEO );
	    	//Intent intent = new Intent(this, AppMainBroadcastReceiver.class);
    		
    		//Intent intent = new Intent(this, AppMainActivity.class);
    		Intent intent = new Intent(this, AppMainBroadcastReceiver.class);
	    	intent.setAction(TRACKING_GEO);
	    	geoTrackingIntent = PendingIntent.getBroadcast( this, 0, intent, 
	    			PendingIntent.FLAG_CANCEL_CURRENT );
	    	
	    	//geoTrackingIntent = PendingIntent.getActivity(this, 0, intent, 
	    	//		PendingIntent.FLAG_CANCEL_CURRENT);
	    	
	    	//geoTrackingIntent = PendingIntent.getActivity(this, 0,
	        //        new Intent(this, AppMainActivity.class), 0);
    	}
    	return geoTrackingIntent;
    }
    
    void startGEOAlarm() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in startGEOAlarm: " + (geoTrackingIntent == null ? "null" : geoTrackingIntent));
    	
    	if(geoTrackingIntent == null) {
	    	String alarm = Context.ALARM_SERVICE;
	    	AlarmManager am = ( AlarmManager ) getSystemService( alarm );
	    	 
	    	int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
	    	//long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
	    	long interval = 60000;
	    	//long interval = 5000;
	    	long triggerTime = SystemClock.elapsedRealtime() + interval;
	    	
	    	AppMainBroadcastReceiver.setMainApp(this);
	    	am.setInexactRepeating( type, triggerTime, interval, getGeoTrackingIntent() );
    	}
    }
    
    void cancelGEOAlarm() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in cancelGEOAlarm: " + (geoTrackingIntent == null ? "null" : geoTrackingIntent));
    	
    	//if(geoTrackingIntent != null) {
	    	String alarm = Context.ALARM_SERVICE;
	    	AlarmManager am = ( AlarmManager ) getSystemService( alarm );
	    	 
	    	//Intent intent = new Intent( TRACKING_GEO );
	    	//PendingIntent pi = PendingIntent.getBroadcast( this, 0, intent, 0 );
	    	 
	    	am.cancel(getGeoTrackingIntent());
	    	geoTrackingIntent = null;
    	//}
    }
    
	void setupGPSTracking() {
		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in setupGPSTracking mLocationRequest: " + (mLocationRequest == null ? "null" : mLocationRequest));
		
		if(mLocationRequest == null) {
			// Create the LocationRequest object
			mLocationRequest = LocationRequest.create();
			// Use high accuracy
			mLocationRequest.setPriority(
			        LocationRequest.PRIORITY_HIGH_ACCURACY);
			// Set the update interval to 5 seconds
			mLocationRequest.setInterval(UPDATE_INTERVAL);
			// Set the fastest update interval to 1 second
			mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		}
		
		//Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in setupGPSTracking mLocationClient: " + (mLocationClient == null ? "null" : mLocationClient));
		
//		if(mLocationClient == null) {
//			/*
//			 * Create a new location client, using the enclosing class to
//			 * handle callbacks.
//			 */
//			mLocationClient = new LocationClient(this, this, this);
//		}
	}

    private void setupLoginUI() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": setup login auth = " + (auth == null ? "null" : auth.getUserId()));
    	
        auth = null;
        lastCallout = null;
        
// http://developer.android.com/google/gcm/adv.html#unreg-why        
//        if(gcm != null) {
//        	try {
//				gcm.unregister();
//			} 
//        	catch (IOException e) {
//				// TODO Auto-generated catch block
//				//e.printStackTrace();
//				Log.i(Utils.TAG, "GCM could not unregister: " + e.getMessage());
//			}
//        }
//        gcm = null;

        mDisplay.setText("");
        
        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
        txtMsg.setText(getResources().getString(R.string.login_credentials));
    	
        Button btnLogin = (Button)findViewById(R.id.btnLogin);
        btnLogin.setEnabled(true);
        btnLogin.setVisibility(View.VISIBLE);
        
        Button btnMap = (Button)findViewById(R.id.btnMap);
        btnMap.setEnabled(false);
        btnMap.setVisibility(View.INVISIBLE);
        
        Button btnRespond = (Button)findViewById(R.id.btnRespond);
        btnRespond.setEnabled(false);
        btnRespond.setVisibility(View.INVISIBLE);
        
        Button btnCompleteCall = (Button)findViewById(R.id.btnCompleteCall);
        btnCompleteCall.setEnabled(false);
        btnCompleteCall.setVisibility(View.INVISIBLE);
        
        Button btnCancelCall = (Button)findViewById(R.id.btnCancelCall);
        btnCancelCall.setEnabled(false);
        btnCancelCall.setVisibility(View.INVISIBLE);
        
        EditText etFhid = (EditText)findViewById(R.id.etFhid);
        etFhid.setText(getResources().getString(R.string.firehallid));
        etFhid.setVisibility(View.VISIBLE);
        
        EditText etUid = (EditText)findViewById(R.id.etUid);
        etUid.setText(getResources().getString(R.string.userid));
        etUid.setVisibility(View.VISIBLE);
        
        EditText etUpw = (EditText)findViewById(R.id.etUpw);
        etUpw.setText("");
        etUpw.setVisibility(View.VISIBLE);
        
        Button btnCallDetails = (Button)findViewById(R.id.btnCallDetails);
        btnCallDetails.setEnabled(false);
        btnCallDetails.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStart() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in onStart mLocationClient: " + (mGoogleApiClient == null ? "null" : mGoogleApiClient));
    	
    	super.onStart();
    	
        // Connect the GPS client.
    	setupGPSTracking();
//    	if(mLocationClient != null) {
//    		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in onStart mLocationClient.isConnected() = " + mLocationClient.isConnected());
//    		
//	        if (mLocationClient.isConnected() == false) {
//	        	mLocationClient.connect();
//	        }
//    	}
    	// Connect the client.
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in onStart calling mGoogleApiClient.connect()");
        mGoogleApiClient.connect();
    }

    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    protected void onStop() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in onStop: " + (mGoogleApiClient == null ? "null" : mGoogleApiClient));
    	
        // If the client is connected
//    	if(mLocationClient != null) {
//	        if (mLocationClient.isConnected()) {
//	            /*
//	             * Remove location updates for a listener.
//	             * The current Activity is the listener, so
//	             * the argument is "this".
//	             */
//	            //removeLocationUpdates(this);
//	        }
//	        /*
//	         * After disconnect() is called, the client is
//	         * considered "dead".
//	         */
//	        mLocationClient.disconnect();
//    	}
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner in onStop calling mGoogleApiClient.disconnect()");
        mGoogleApiClient.disconnect();
    	
        super.onStop();
    }
    
    @Override
    protected void onPause() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": pausing Rip Runner.");
    	
        super.onPause();
    }
    
    @Override
    protected void onResume() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": resuming Rip Runner.");
    	
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } 
            else {
                Log.i(Utils.TAG, Utils.getLineNumber() + ": This device is not supported.");
                finish();
            }
            return false;
        }
        else {
        	if(mGoogleApiClient == null) {
	            mGoogleApiClient = new GoogleApiClient.Builder(this)
		        .addApi(LocationServices.API)
		        .addConnectionCallbacks(this)
		        .addOnConnectionFailedListener(this)
		        .build();
        	}
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeConfigItem(Context context, String keyName, Object value) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        
        Log.i(Utils.TAG, Utils.getLineNumber() + ": Saving " + keyName + " on app version " + appVersion);
        
        SharedPreferences.Editor editor = prefs.edit();
        if(value instanceof String) {
        	editor.putString(keyName, (String)value);
        }
        else if(value instanceof Boolean) {
        	editor.putBoolean(keyName, (Boolean)value);
        }
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private <T> boolean hasConfigItem(Context context, String keyName, Class<T> type) {
        final SharedPreferences prefs = getGcmPreferences(context);
        //String value = prefs.getString(keyName, "");
        T value = null;
        if(type.equals(String.class)) {
        	value = type.cast(prefs.getString(keyName, ""));
        }
        else if(type.equals(Boolean.class)) {
        	value = type.cast(prefs.getBoolean(keyName, true));
        }
        
        if (value == null) {
            return false;
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return false;
        }
        return true;
    }
    
    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    <T> T getConfigItem(Context context, String keyName, Class<T> type) {
        final SharedPreferences prefs = getGcmPreferences(context);
        
        //String value = prefs.getString(keyName, "");
        T value = null;
        if(type.equals(String.class)) {
        	value = type.cast(prefs.getString(keyName, ""));
        }
        else if(type.equals(Boolean.class)) {
        	value = type.cast(prefs.getBoolean(keyName, true));
        }
        
        if (value == null) {
            Log.i(Utils.TAG, Utils.getLineNumber() + ": Config Item not found: " + keyName);
            try {
				return type.newInstance();
			} 
            catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			} 
            catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			}
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(Utils.TAG, Utils.getLineNumber() + ": App version changed.");
            try {
				return type.newInstance();
			} 
            catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			} 
            catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			}
        }
        return value;
    }

    private String getGcmDeviceRegistrationId(boolean forceNewId) throws IOException {
        if (gcm == null) {
            gcm = GoogleCloudMessaging.getInstance(context);
        }
        
        String regid = getConfigItem(context,PROPERTY_REG_ID,String.class);
        if (forceNewId == true || regid.isEmpty()) {
        	regid = gcm.register(getConfigItem(context,PROPERTY_SENDER_ID,String.class));

        	// Persist the regID - no need to register again.
        	storeConfigItem(context, PROPERTY_REG_ID, regid);
        }
    	return regid;
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
        	
        	@Override
            protected void onPreExecute() {
                super.onPreExecute();
                
                runOnUiThread(new Runnable() {
                    public void run() {
                    	showProgressDialog(true, "Loading...");
                    }
                });
        	}
        	
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                	String regid = getGcmDeviceRegistrationId(false);
                	
                    EditText etFhid = (EditText)findViewById(R.id.etFhid);
                    EditText etUid = (EditText)findViewById(R.id.etUid);
                    EditText etUpw = (EditText)findViewById(R.id.etUpw);
                    
                    auth = new FireHallAuthentication(
                    		getConfigItem(context,PROPERTY_WEBSITE_URL,String.class).toString(), 
                    		etFhid.getText().toString(),
                    		etUid.getText().toString(), 
                    		etUpw.getText().toString(),
                    		regid, false);
                    //msg = "Device registered, ID:\n[" + regid + "]";
                    msg = getResources().getString(R.string.waiting_for_callout);
                    
                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend(auth);
                } 
                catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", ex);
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void respondInBackground(final CalloutStatusType statusType) {
        new AsyncTask<Void, Void, String>() {
        	
        	@Override
            protected void onPreExecute() {
                super.onPreExecute();
                
                runOnUiThread(new Runnable() {
                    public void run() {
                    	showProgressDialog(true, "Loading...");
                    }
                });
        	}
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendResponseToBackend(auth,statusType);
                } 
                catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    Log.e(Utils.TAG, Utils.getLineNumber() + ": Error statusType" + statusType, ex);
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }
    
    // Handle onclick events
    public void onClick(final View view) {

        if (view == findViewById(R.id.btnLogin)) {
            handleLoginClick();
        }
        else if (view == findViewById(R.id.btnMap)) {
			handleCalloutMapView();
        }
        else if (view == findViewById(R.id.btnRespond)) {
            handleRespondClick();
        }
        else if (view == findViewById(R.id.btnCompleteCall)) {
        	handleCompleteCallClick();
        }
        else if (view == findViewById(R.id.btnCancelCall)) {
        	handleCancelCallClick();
        }
        else if (view == findViewById(R.id.btnCallDetails)) {
			handleCalloutDetailsView();
        }
    }

	void handleCancelCallClick() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.dialog_title_question)
				.setMessage(R.string.dialog_text_cancel_call)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, 
			new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface dialog, int whichButton) {
		        new AsyncTask<Void, Void, String>() {
		            @Override
		            protected String doInBackground(Void... params) {
		                if (checkPlayServices()) {
		                	if(isLoggedIn()) {
		                		respondInBackground(CalloutStatusType.Cancelled);
		                	}
		                } 
		                else {
		                    Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
		                }
		            	return "";
		            }

		            @Override
		            protected void onPostExecute(String msg) {
		                mDisplay.append(msg + "\n");
		            }
		        }.execute(null, null, null);
		    }})
		 .setNegativeButton(android.R.string.no, null).show();
	}

	void handleCompleteCallClick() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.dialog_title_question)
				.setMessage(R.string.dialog_text_complete_call)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, 
			new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface dialog, int whichButton) {
		        new AsyncTask<Void, Void, String>() {
		            @Override
		            protected String doInBackground(Void... params) {
		                if (checkPlayServices()) {
		                	if(isLoggedIn()) {
		                		respondInBackground(CalloutStatusType.Complete);
		                	}
		                } 
		                else {
		                    Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
		                }
		            	return "";
		            }

		            @Override
		            protected void onPostExecute(String msg) {
		                mDisplay.append(msg + "\n");
		            }
		        }.execute(null, null, null);
		    }})
		 .setNegativeButton(android.R.string.no, null).show();
	}

	void handleRespondClick() {
		new AsyncTask<Void, Void, String>() {
		    @Override
		    protected String doInBackground(Void... params) {
		        if (checkPlayServices()) {
		        	if(isLoggedIn()) {
		        		respondInBackground(CalloutStatusType.Responding);
		        	}
		        } 
		        else {
		            Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
		        }
		    	return "";
		    }

		    @Override
		    protected void onPostExecute(String msg) {
		        mDisplay.append(msg + "\n");
		    }
		}.execute(null, null, null);
	}

	void handleLoginClick() {
		new AsyncTask<Void, Void, String>() {
		    @Override
		    protected String doInBackground(Void... params) {
		        if (checkPlayServices()) {
		        	if(isLoggedIn() == false) {
		        		registerInBackground();
		        	}
		        } 
		        else {
		            Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
		        }
		    	return "";
		    }

		    @Override
		    protected void onPostExecute(String msg) {
		        mDisplay.append(msg + "\n");
		    }
		}.execute(null, null, null);
	}

    private void handleCalloutMapView() {
    	try {
    		String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%s,%s (%s)", 
					URLEncoder.encode(lastCallout.getGPSLat(), "utf-8"), 
					URLEncoder.encode(lastCallout.getGPSLong(), "utf-8"),
					URLEncoder.encode(lastCallout.getMapAddress(), "utf-8"));
    		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uri));
    		intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
        	if (intent.resolveActivity(getPackageManager()) != null) {
        		context.startActivity(intent);
        	}
        	else {
        		uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%s,%s (%s)", 
        				URLEncoder.encode(lastCallout.getGPSLat(), "utf-8"), 
        				URLEncoder.encode(lastCallout.getGPSLong(), "utf-8"), 
        				URLEncoder.encode(lastCallout.getMapAddress(), "utf-8"));
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                	context.startActivity(intent);
                }
                catch(ActivityNotFoundException innerEx) {
                	Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", innerEx);
                    Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
                }
        	}
		} 
		catch (UnsupportedEncodingException e) {
			Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			Toast.makeText(this, "UnsupportedEncodingException: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}    	
    }
        
    private void handleCalloutDetailsView() {
    	try {
    		String uri = auth.getHostURL() + 
    					"ci.php?cid=" + URLEncoder.encode(lastCallout.getCalloutId(), "utf-8")  + 
    					"&fhid=" + URLEncoder.encode(auth.getFirehallId(), "utf-8") + 
    					"&ckid=" + URLEncoder.encode(lastCallout.getCalloutKeyId(), "utf-8");
    		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uri));
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           	context.startActivity(intent);
		} 
		catch (UnsupportedEncodingException e) {
			Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			Toast.makeText(this, "UnsupportedEncodingException: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}    	
    }
    
    @Override
    protected void onDestroy() {
    	Log.i(Utils.TAG, Utils.getLineNumber() + ": destroying Rip Runner.");
    	
        super.onDestroy();
        
        stopGPSTracking();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } 
        catch (NameNotFoundException e) {
            // should never happen
        	Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        return getSharedPreferences(AppMainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    
    private boolean isGcmErrorNotRegistered(String responseString) {
    	//|GCM_ERROR:
    	if(responseString != null && responseString.contains("|GCM_ERROR:")) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private void sendRegistrationIdToBackend(FireHallAuthentication auth) throws ClientProtocolException, IOException {
    	
    	List<NameValuePair> params = new LinkedList<NameValuePair>();
    	params.add(new BasicNameValuePair("rid", auth.getGCMRegistrationId()));
    	params.add(new BasicNameValuePair("fhid", auth.getFirehallId()));
    	params.add(new BasicNameValuePair("uid", auth.getUserId()));
    	params.add(new BasicNameValuePair("upwd", auth.getUserPassword()));
    	String paramString = URLEncodedUtils.format(params, "utf-8");
    	String URL = auth.getHostURL() + "register_device.php?" + paramString;
    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(URL));
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            
            final String responseString = out.toString();
            if(isGcmErrorNotRegistered(responseString)) {
            	String regid = getGcmDeviceRegistrationId(true);
            	auth.setGCMRegistrationId(regid);
            	sendRegistrationIdToBackend(auth);
            	return;
            }
            		
            if(responseString != null && responseString.startsWith("OK=")) {
	            handleRegistrationSuccess(auth);
            }
            else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        EditText etUpw = (EditText)findViewById(R.id.etUpw);
                        etUpw.setText("");
                        
                        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
                        txtMsg.setText("Invalid login attempt: " + responseString);
                        
                        showProgressDialog(false, null);
                   }
                });            
            }
        } 
        else {
            //Closes the connection.
            response.getEntity().getContent().close();

            final String errorText = statusLine.getReasonPhrase();
            runOnUiThread(new Runnable() {
                public void run() {
                    EditText etUpw = (EditText)findViewById(R.id.etUpw);
                    etUpw.setText("");
                    
                    TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
                    txtMsg.setText("Error during login: " + errorText);
                    
                    showProgressDialog(false, null);
               }
            });            
        }    	
    }

	void handleRegistrationSuccess(FireHallAuthentication auth) {
		storeConfigItem(context, PROPERTY_FIREHALL_ID, auth.getFirehallId());
		storeConfigItem(context, PROPERTY_USER_ID, auth.getUserId());
		
		auth.setRegisteredBackend(true);
		
		runOnUiThread(new Runnable() {
		    public void run() {
		    	
		        Button btnLogin = (Button)findViewById(R.id.btnLogin);
		        btnLogin.setEnabled(false);
		        btnLogin.setVisibility(View.INVISIBLE);
		    	
		        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
		        txtMsg.setText(getResources().getString(R.string.login_success));

		        // Enable when debugging
		        //mDisplay.setText(responseString);
		        
		        EditText etFhid = (EditText)findViewById(R.id.etFhid);
		        etFhid.setText("");
		        etFhid.setVisibility(View.GONE);
		        EditText etUid = (EditText)findViewById(R.id.etUid);
		        etUid.setText("");
		        etUid.setVisibility(View.GONE);
		        EditText etUpw = (EditText)findViewById(R.id.etUpw);
		        etUpw.setText("");
		        etUpw.setVisibility(View.GONE);
		        
		        playSound(context,FireHallSoundPlayer.SOUND_LOGIN);
		        
		        showProgressDialog(false, null);
		        
		        InputMethodManager imm = (InputMethodManager)getSystemService(
		        	      Context.INPUT_METHOD_SERVICE);
		        imm.hideSoftInputFromWindow(etUpw.getWindowToken(), 0);		        
		   }
		});
	}

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private void sendResponseToBackend(FireHallAuthentication auth,
    		final CalloutStatusType statusType) throws ClientProtocolException, IOException {
    	
    	List<NameValuePair> params = new LinkedList<NameValuePair>();
    	params.add(new BasicNameValuePair("cid", lastCallout.getCalloutId()));
    	params.add(new BasicNameValuePair("ckid", lastCallout.getCalloutKeyId()));
    	params.add(new BasicNameValuePair("fhid", auth.getFirehallId()));
    	params.add(new BasicNameValuePair("uid", auth.getUserId()));
    	params.add(new BasicNameValuePair("upwd", auth.getUserPassword()));
   		params.add(new BasicNameValuePair("lat", String.valueOf(getLastGPSLatitude())));
   		params.add(new BasicNameValuePair("long", String.valueOf(getLastGPSLongitude())));
    	params.add(new BasicNameValuePair("status", String.valueOf(statusType.valueOf())));
    	
    	String paramString = URLEncodedUtils.format(params, "utf-8");
    	String URL = auth.getHostURL() + "cr.php?" + paramString;
    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(URL));
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            
            final String responseString = out.toString();
            if(responseString != null && responseString.startsWith("OK=")) {
	            handleResponseSuccess();
            }
            else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        
                        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
                        txtMsg.setText("Invalid server response: " + 
                        		(responseString != null ? responseString : "null"));
                        
                        showProgressDialog(false, null);
                   }
                });            
            }
        } 
        else {
            //Closes the connection.
            response.getEntity().getContent().close();

            final String errorText = statusLine.getReasonPhrase();
            runOnUiThread(new Runnable() {
                public void run() {
                    TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
                    txtMsg.setText("Error during server response: " + 
                    			(errorText != null ? errorText : "null"));
                    
                    showProgressDialog(false, null);
               }
            });            
        }    	
    }

	void handleResponseSuccess() {
		runOnUiThread(new Runnable() {
		    public void run() {
		    	
		        Button btnRespond = (Button)findViewById(R.id.btnRespond);
		        btnRespond.setVisibility(View.VISIBLE);
		        btnRespond.setEnabled(false);
		    	
		        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
		        txtMsg.setText(getResources().getString(R.string.callout_respond_success));
		        
		        playSound(context,FireHallSoundPlayer.SOUND_DINGLING);
		        
		        showProgressDialog(false, null);
		   }
		});
	}

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    public String sendGeoTrackingToBackend() {
    	
    	if(isLoggedIn() && lastCallout != null &&
	    	CalloutStatusType.isComplete(lastCallout.getStatus()) == false) {

    		runOnUiThread(new Runnable() {
     		   public void run() {
    		
	    		Toast.makeText(context, "Tracking GEO Coordinates: " + 
	    					String.valueOf(getLastGPSLatitude()) + "," + 
	    					String.valueOf(getLastGPSLongitude()), Toast.LENGTH_LONG).show();
    		   }
    		});
    		
    		boolean track_geo_coords = (getLastGPSLatitude() != 0 && getLastGPSLongitude() != 0);
    		//boolean track_geo_coords = true;
    		if(track_geo_coords) {
    			
		    	List<NameValuePair> params = new LinkedList<NameValuePair>();
		    	params.add(new BasicNameValuePair("fhid", auth.getFirehallId()));
		    	params.add(new BasicNameValuePair("cid", lastCallout.getCalloutId()));
		    	params.add(new BasicNameValuePair("uid", auth.getUserId()));
		    	params.add(new BasicNameValuePair("ckid", lastCallout.getCalloutKeyId()));
	
		    	params.add(new BasicNameValuePair("upwd", auth.getUserPassword()));
		   		params.add(new BasicNameValuePair("lat", String.valueOf(getLastGPSLatitude())));
		   		params.add(new BasicNameValuePair("long", String.valueOf(getLastGPSLongitude())));
		    	
		    	String paramString = URLEncodedUtils.format(params, "utf-8");
		    	String URL = auth.getHostURL() + "ct.php?" + paramString;
		    	
		    	HttpClient httpclient = new DefaultHttpClient();
		        
				try {
					HttpResponse response = httpclient.execute(new HttpGet(URL));
			        StatusLine statusLine = response.getStatusLine();
			        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			            ByteArrayOutputStream out = new ByteArrayOutputStream();
			            response.getEntity().writeTo(out);
			            out.close();
			            
			            final String responseString = out.toString();
			            if(responseString != null && responseString.startsWith("OK=")) {
			        		runOnUiThread(new Runnable() {
			        		   public void run() {
			
			        			   Toast.makeText(context, "Success tracking GEO Coordinates now.", Toast.LENGTH_LONG).show();
			        		   }
			        		});
			        		
			        		return responseString;
			            }
			            else if(responseString != null && responseString.startsWith("CALLOUT_ENDED=")) {
			            	
			        		runOnUiThread(new Runnable() {
				        		   public void run() {
			            	
				        			   Toast.makeText(context, "CALLOUT ENDED - GEO Coordinates check.", Toast.LENGTH_LONG).show();
				        		   }
				        	});
			        		
			        		return responseString;
			            }
			            else {
			                runOnUiThread(new Runnable() {
			                    public void run() {
			                        
			                        TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
			                        txtMsg.setText("Invalid server response: " + 
			                        		(responseString != null ? responseString : "null"));
			                        
			                        //showProgressDialog(false, null);
			                   }
			                });
			                
			                return responseString;
			            }
			        } 
			        else {
			            //Closes the connection.
			            response.getEntity().getContent().close();
			
			            final String errorText = statusLine.getReasonPhrase();
			            
			            runOnUiThread(new Runnable() {
			                public void run() {
			                    TextView txtMsg = (TextView)findViewById(R.id.txtMsg);
			                    txtMsg.setText("Error during server response: " + 
			                    			(errorText != null ? errorText : "null"));
			                    
			                    //showProgressDialog(false, null);
			               }
			            });
			            
			            return errorText;
			        }
				} 
				catch (ClientProtocolException e) {
	            	Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
	            	
	            	final IOException ex = e;
	        		runOnUiThread(new Runnable() {
		        		   public void run() {
	            	
		        			   Toast.makeText(context, "Error detected: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		                   }
	                });            
				} 
				catch (IOException e) {
	            	Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);

	            	final IOException ex = e;
	        		runOnUiThread(new Runnable() {
		        		   public void run() {
	            	
		        			   Toast.makeText(context, "Error detected: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		                   }
	                });            
				}
    		}
    	}
    	return "";
    }
	
    ProgressDialog getProgressDialog() {
    	if(loadingDlg == null) {
    		loadingDlg = new ProgressDialog(AppMainActivity.this);	
    	}
    	return loadingDlg;
    }
    
    void showProgressDialog(boolean show, String msg) {
    	if(show) {
    		getProgressDialog().setMessage(msg);
    		getProgressDialog().setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		getProgressDialog().setIndeterminate(true);
    		getProgressDialog().setCancelable(false);
    		getProgressDialog().show();
    	}
    	else {
    		getProgressDialog().hide();
    	}
    }

    public void stopGPSTracking() {
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity_actions, menu);
	    logout_menu = (MenuItem) menu.findItem(R.id.action_logout);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	
		logout_menu.setEnabled(isLoggedIn()); // here pass the index of save menu item
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
		Log.i(Utils.TAG, Utils.getLineNumber() + ": handling selected option in Rip Runner.");
		
	    switch (item.getItemId()) {
	        case R.id.action_logout:
	            logout();
	            return true;
	        case R.id.action_clear:
	            clearUI();
	            return true;
	        case R.id.action_mapmygps:
	            mapCurrentLocation();
	            return true;
	    
	        case R.id.action_settings:
	            openSettings();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void mapCurrentLocation() {
    	try {
    		
//			String uri = String.format(Locale.ENGLISH, "geo:%s,%s?q=%s", 
//					URLEncoder.encode(lastCallout.getGPSLat(), "utf-8"), 
//					URLEncoder.encode(lastCallout.getGPSLong(), "utf-8"),
//					URLEncoder.encode(lastCallout.getMapAddress(), "utf-8"));
//			
//        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
//        	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
    		String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?q=%s,%s", 
					URLEncoder.encode(String.valueOf(getLastGPSLatitude()), "utf-8"), 
					URLEncoder.encode(String.valueOf(getLastGPSLongitude()), "utf-8"));
    		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(uri));
    		intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
        	if (intent.resolveActivity(getPackageManager()) != null) {
        		context.startActivity(intent);
        	}
        	else {
        		uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?q=%s,%s", 
    					URLEncoder.encode(String.valueOf(getLastGPSLatitude()), "utf-8"), 
    					URLEncoder.encode(String.valueOf(getLastGPSLongitude()), "utf-8"));
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                	context.startActivity(intent);
                }
                catch(ActivityNotFoundException innerEx) {
                	Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", innerEx);
                    Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
                }
        	}
		} 
		catch (UnsupportedEncodingException e) {
			Log.e(Utils.TAG, Utils.getLineNumber() + ": Error", e);
			Toast.makeText(this, "UnsupportedEncodingException: " + 
								e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private boolean isLoggedIn() {
		return(auth != null && auth.getRegisteredBackend());
	}
	
	private void logout() {
        if (checkPlayServices()) {
        	if(auth != null) {
        		Log.i(Utils.TAG, Utils.getLineNumber() + ": Logging out of Rip Runner.");
        		
	            runOnUiThread(new Runnable() {
	                public void run() {
	                	setupLoginUI();
	                	playSound(context,FireHallSoundPlayer.SOUND_DINGLING);
	               }
	            });       
        	}
        } 
        else {
            Log.i(Utils.TAG, Utils.getLineNumber() + ": No valid Google Play Services APK found.");
        }
	}
	
	private void clearUI() {
        mDisplay.setText("");
	}
	
	private void openSettings() {
		cancelGEOAlarm();
    	
		Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
    	intent.setClass(AppMainActivity.this, SettingsActivity.class);
    	startActivityForResult(intent, 0); 
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
        displayUserSettings();
    }
    
    private void displayUserSettings() {
	    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	
	    String host_url = sharedPrefs.getString(PROPERTY_WEBSITE_URL, "");
	    String sender_id = sharedPrefs.getString(PROPERTY_SENDER_ID, "");
	    Boolean tracking_enabled = sharedPrefs.getBoolean(PROPERTY_TRACKING_ENABLED, true);
	
	    storeConfigItem(context, PROPERTY_WEBSITE_URL, host_url);
	    storeConfigItem(context, PROPERTY_SENDER_ID, sender_id);
	    storeConfigItem(context, PROPERTY_TRACKING_ENABLED, tracking_enabled);
	    
	    startGEOAlarm();
    }

    private BroadcastReceiver getBroadCastReceiver() {
    	if(bReceiver == null) {
    		bReceiver = new AppMainBroadcastReceiver();
    		((AppMainBroadcastReceiver)bReceiver).setMainApp(this);
    	}
    	return bReceiver;
    }

    /** Populate the SoundPool*/
    @SuppressLint("UseSparseArrays")
	public static void initSounds(Context context) {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
	    soundPoolMap = new HashMap<Integer,Integer>();
	
	    soundPoolMap.put( FireHallSoundPlayer.SOUND_DINGLING, soundPool.load(context, R.raw.dingling, 2) );
	    soundPoolMap.put( FireHallSoundPlayer.SOUND_LOGIN, soundPool.load(context, R.raw.login, 2) );
	    soundPoolMap.put( FireHallSoundPlayer.SOUND_PAGE1, soundPool.load(context, R.raw.page1, 2) );
	    soundPoolMap.put( FireHallSoundPlayer.SOUND_PAGER_TONE_PG, soundPool.load(context, R.raw.pager_tone_pg, 2) );
    }    
    
    /** Play a given sound in the soundPool */
    public static void playSound(Context context, int soundID) {
	   if(soundPool == null || soundPoolMap == null){
	      initSounds(context);
	   }
       float volume = (float) 1.0; // whatever in the range = 0.0 to 1.0

       // play sound with same right and left volume, with a priority of 1, 
       // zero repeats (i.e play once), and a playback rate of 1f
       soundPool.play(soundPoolMap.get(soundID), volume, volume, 1, 0, 1f);
    }

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner GPS Connection FAILED!");
		Toast.makeText(this, "GPS Connection FAILED!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner GPS Connection SUSPENDED!");
		Toast.makeText(this, "GPS Connection SUSPENDED!", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		// Display the connection status
		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner GPS Connected: " + (arg0 == null ? "null" : arg0));
        Toast.makeText(this, "GPS Connected", Toast.LENGTH_SHORT).show();

        // If already requested, start periodic updates
//        if (mUpdatesRequested && mLocationClient != null) {
//            mLocationClient.requestLocationUpdates(mLocationRequest, this);
//        }
        
        if (mUpdatesRequested) {
	        mLocationRequest = LocationRequest.create();
	        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	        mLocationRequest.setInterval(20000); // Update location every 20 seconds
	
	        LocationServices.FusedLocationApi.requestLocationUpdates(
	                mGoogleApiClient, mLocationRequest, this);
        }
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner GPS Disconnected.");
        Toast.makeText(this, "GPS Disconnected.",Toast.LENGTH_SHORT).show();
	}

	@Override
    public void onLocationChanged(Location location) {
		//this.location = location;
		// Report to the UI that the location was updated
		//Log.i(Utils.TAG, Utils.getLineNumber() + ": Rip Runner onLocationChanged.");
		
		// Debug GPS values
//        String msg = "Updated GPS Location: " +
//                Double.toString(location.getLatitude()) + "," +
//                Double.toString(location.getLongitude());
//        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		
		lastTrackedGEOLocation = location;
	}

	double getLastGPSLatitude() {
		if(lastTrackedGEOLocation == null) {
			return 0;
		}
        double lat = lastTrackedGEOLocation.getLatitude();
        return lat;
	}
	double getLastGPSLongitude() {
		if(lastTrackedGEOLocation == null) {
			return 0;
		}

        double lng = lastTrackedGEOLocation.getLongitude();
        return lng;
	}
	
}
