package com.neuron.trafikanten;

import android.app.Activity;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.neuron.trafikanten.hacks.GoogleAnalyticsCleanup;


// http://www.anddev.org/simple_splash_screen_-_alternative-t815.html
public class SplashScreen extends Activity { 

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
	         
	         
	         /*
	          * Cleanup broken analytics database, this is a HACK
	          */
	        GoogleAnalyticsCleanup myDbHelper = new GoogleAnalyticsCleanup(this);
            try {
                myDbHelper.openDataBase();
                int deleted = myDbHelper.deleteEvents();
                if (deleted > 0) {
                	Toast.makeText(this, "Deleted " + deleted + " invalid google analytics database entries, report this!", Toast.LENGTH_LONG);
                	Log.i("Trafikanten-SplashScreen","Deleted " + deleted + " invalid google analyics database entries");
                }
                myDbHelper.close();
            }
            catch (SQLException sqle) {
                throw sqle;
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