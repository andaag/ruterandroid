package com.neuron.trafikanten;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.hacks.GoogleAnalyticsCleanup;


// http://www.anddev.org/simple_splash_screen_-_alternative-t815.html
public class SplashScreen extends Activity {
	public final static String KEY_ANALYTICSERRORS = "analyticserrors";
	public final static String KEY_ANALYTICSERRORS_VER = "analyticserrorsversion";

    // =========================================================== 
    // Fields 
    // =========================================================== 
     
    private final int SPLASH_DISPLAY_LENGHT = 1000; 

    // =========================================================== 
    // "Constructors" 
    // =========================================================== 

    /** Called when the activity is first created. */ 
    @Override 
    public void onCreate(Bundle savedInstanceState) { 
         super.onCreate(savedInstanceState);
         
         if (savedInstanceState == null) {
	         requestWindowFeature(Window.FEATURE_NO_TITLE);
	         setContentView(R.layout.splashscreen); 
	          
	         /* New Handler to start the Menu-Activity 
	          * and close this Splash-Screen after some seconds.*/ 
	         new Handler().postDelayed(new Runnable(){ 
	              @Override 
	              public void run() { 
	                   /* Create an Intent that will start the Menu-Activity. */ 
	                   Intent mainIntent = new Intent(SplashScreen.this, Trafikanten.class);
	                   mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	                   SplashScreen.this.startActivity(mainIntent); 
	                   SplashScreen.this.finish(); 
	              } 
	         }, SPLASH_DISPLAY_LENGHT); 
	         
	         
         	GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
    		tracker.start("UA-16690738-3", this);
			try {
	         	PackageInfo packageInfo = getPackageManager().getPackageInfo("com.neuron.trafikanten", PackageManager.GET_META_DATA);
				//tracker.trackEvent("Version", "Application", URLEncoder.encode(packageInfo.versionName,"UTF-8"), packageInfo.versionCode);
	         	tracker.trackEvent("Version", "Application", URLEncoder.encode((String) getText(R.string.app_version),"UTF-8"), packageInfo.versionCode);
			} catch (NameNotFoundException e) {
         	} catch (UnsupportedEncodingException e) {}
         	
	         /*
	          * Cleanup broken analytics database, this is a HACK
	          */
	        GoogleAnalyticsCleanup myDbHelper = new GoogleAnalyticsCleanup(this);
            try {
                myDbHelper.openDataBase();
                int deleted = myDbHelper.deleteEvents();
                myDbHelper.close();
                if (deleted > 0) {
                	Log.e("Trafikanten-SplashScreen","Deleted " + deleted + " invalid google analyics database entries");
					try {
						int packageVersion = getPackageManager().getPackageInfo("com.neuron.trafikanten", PackageManager.GET_META_DATA).versionCode;
	            		tracker.trackEvent("Error", "GoogleAnalytics", "Version:" + packageVersion, deleted);
	            		tracker.dispatch();
					} catch (NameNotFoundException e) {}
                }

            }
            catch (SQLException sqle) {
                //throw sqle;
            }
            finally{
                myDbHelper.close();
            }
	         
         } else {
        	 // No splash screen needed.
             Intent mainIntent = new Intent(this, Trafikanten.class);
             mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
             startActivity(mainIntent); 
             finish(); 
         }
    } 
}