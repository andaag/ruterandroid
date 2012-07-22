package com.neuron.trafikanten;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;

import com.google.android.AnalyticsUtils;
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

         boolean splashscreenIsDisabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("splashscreen_disabled", false);
         if (savedInstanceState == null && !splashscreenIsDisabled) {
	         requestWindowFeature(Window.FEATURE_NO_TITLE);
	         setContentView(R.layout.splashscreen); 
	          
	         /* New Handler to start the Menu-Activity 
	          * and close this Splash-Screen after some seconds.*/ 
	         new Handler().postDelayed(new Runnable(){ 
	              @Override 
	              public void run() {
                      removeSplashscreen();

                  }
	         }, SPLASH_DISPLAY_LENGHT); 
	         
	        
	         /*
	          * Track version and log to analytics
	          */
			try {
	         	PackageInfo packageInfo = getPackageManager().getPackageInfo("com.neuron.trafikanten", PackageManager.GET_META_DATA);
				//tracker.trackEvent("Version", "Application", URLEncoder.encode(packageInfo.versionName,"UTF-8"), packageInfo.versionCode);
	         	AnalyticsUtils.getInstance(this).trackEvent("Version", "Application", URLEncoder.encode(HelperFunctions.GetApplicationVersion(this),"UTF-8"), packageInfo.versionCode);
			} catch (NameNotFoundException e) {
         	} catch (UnsupportedEncodingException e) {}
			
			
			/*
			 * Track first start of application and log to analytics
			 */
			{
				SharedPreferences settings = getSharedPreferences("trafikanten", Activity.MODE_PRIVATE);
				if (settings.getBoolean("firststart", true)) {
					try {
						AnalyticsUtils.getInstance(this).trackEvent("FirstStart", "Application", URLEncoder.encode(HelperFunctions.GetApplicationVersion(this),"UTF-8"), 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean("firststart", false);
						editor.commit();
					} catch (UnsupportedEncodingException e) {}
				}
			}
         	
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
	            		AnalyticsUtils.getInstance(this).trackEvent("Error", "GoogleAnalytics", "Version:" + packageVersion, deleted);
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
             removeSplashscreen();
         }
    }

    private void removeSplashscreen() {
        /* Create an Intent that will start the Menu-Activity. */
        Intent mainIntent = new Intent(this, Trafikanten.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(mainIntent);
        this.finish();
    }
}