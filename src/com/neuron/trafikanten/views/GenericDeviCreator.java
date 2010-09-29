package com.neuron.trafikanten.views;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.tasks.ShowDeviTask;

public class GenericDeviCreator {
	
    private static Typeface mDeparturesTypeface = null;
    public static Typeface getDeviTypeface(Activity activity) {
    	if (mDeparturesTypeface == null) {
    		mDeparturesTypeface = Typeface.createFromAsset(activity.getAssets(), "fonts/DejaVuSans.ttf");
    	}
    	return mDeparturesTypeface;
    }

    
    /*
     * Function for creating the default devi text, used both for line data and station data
     */
    
    
    
    public static TextView createDefaultDeviText(final Activity activity, final GoogleAnalyticsTracker tracker , final String title, final DeviData deviData, boolean station) {
    	Typeface departuresTypeface = getDeviTypeface(activity);
    	TextView deviText = new TextView(activity);
		deviText.setText(title);
		
		deviText.setSingleLine();
		if (station) {
			deviText.setTextColor(Color.BLACK);
			deviText.setBackgroundResource(R.drawable.skin_stasjonsdevi);
			deviText.setPadding(8, 8, 30, 2);
		} else {
			deviText.setTextColor(Color.rgb(250, 244, 0));
			deviText.setBackgroundResource(R.drawable.skin_sanntiddevi);
			deviText.setPadding(4, 4, 30, 2);
		}
		deviText.setTypeface(departuresTypeface);
		
		deviText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	new ShowDeviTask(activity, tracker, deviData);
								
			}
        });
		return deviText;
    }
}
