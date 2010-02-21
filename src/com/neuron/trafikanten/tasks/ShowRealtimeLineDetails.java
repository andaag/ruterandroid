package com.neuron.trafikanten.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.text.method.ScrollingMovementMethod;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.RealtimeData.NextDeparture;

public class ShowRealtimeLineDetails implements GenericTask {
    //private static final String TAG = "Trafikanten-SelectDeviTask";
    private Activity activity;
    private RealtimeData data;
    private Dialog dialog;
    
    public ShowRealtimeLineDetails(Activity activity, RealtimeData data) 
    {
        this.activity = activity;
        this.data = data;
        showDialog();
    }
    
    private void renderDeparture(LinearLayout body, long expectedDeparture, boolean realtime, String stopVisitNote) {
    	StringBuffer info = new StringBuffer();
    	if (!realtime) {
    		info.append(activity.getText(R.string.ca));
    		info.append(" ");
    	}
    	info.append(HelperFunctions.renderTime(activity, expectedDeparture));
    	if (stopVisitNote != null) {
    		info.append(" - " + stopVisitNote);
    	}
    	
    	TextView infoLine = new TextView(activity);
    	infoLine.setMovementMethod(ScrollingMovementMethod.getInstance());
    	infoLine.setHorizontallyScrolling(true);
    	infoLine.setSingleLine();
    	infoLine.setText(info);
    	body.addView(infoLine);
    }
    
    private void showDialog() {
		//dialog = new Dialog(activity, android.R.style.Theme);
    	dialog = new Dialog(activity);
    	dialog.setTitle(data.line + " " + data.destination);
		dialog.setContentView(R.layout.dialog_showrealtimeline);
		
		final LinearLayout body = (LinearLayout) dialog.findViewById(R.id.body); 
		renderDeparture(body, data.expectedDeparture, data.realtime, data.stopVisitNote);
		for (NextDeparture departure : data.nextDepartures) {
			renderDeparture(body, departure.expectedDeparture, departure.realtime, departure.stopVisitNote);
		}
		
		/*final TextView validPeriod = (TextView) dialog.findViewById(R.id.validPeriod);
		final TextView title = (TextView) dialog.findViewById(R.id.title);
		final TextView description = (TextView) dialog.findViewById(R.id.description);
		final TextView body = (TextView) dialog.findViewById(R.id.devibody);
		
		validPeriod.setText(activity.getText(R.string.validFrom) + " " + dateFormater.format(data.validFrom).toString());
		title.setText(stripCode(data.title));
		description.setText(stripCode(data.description));
		
		final CharSequence bodyText = stripCode(data.body);
		if (bodyText.length() < 3) {
			body.setVisibility(View.GONE);
		}
		else {
			body.setText(stripCode(data.body));
		}*/
		
		dialog.show();
    }

	@Override
	public void stop() {
		dialog.dismiss();
	}
}
