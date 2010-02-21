package com.neuron.trafikanten.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.HorizontalScrollView;
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
    
    private void addText(LinearLayout body, String text, int paddingLeft) {
    	TextView infoLine = new TextView(activity);
    	infoLine.setSingleLine();
    	infoLine.setText(text);
    	infoLine.setPadding(paddingLeft, 0, 0, 1);
    	infoLine.setClickable(false);
    	infoLine.setLongClickable(false);
    	body.addView(infoLine);

    }
    
    private void renderDeparture(LinearLayout body, long expectedDeparture, boolean realtime, String stopVisitNote) {
    	final LinearLayout container = new LinearLayout(activity);
    	final HorizontalScrollView scroll = new HorizontalScrollView(activity);
    	scroll.addView(container);
    	
    	
    	StringBuffer info = new StringBuffer();
    	info.append(data.line + " " + data.destination + " ");
    	if (!realtime) {
    		info.append(activity.getText(R.string.ca));
    		info.append(" ");
    	}
    	info.append(HelperFunctions.renderTime(activity, expectedDeparture));
    	addText(container, info.toString(), 0);
    	if (stopVisitNote != null) {
    		addText(container, stopVisitNote, 10);
    	}
    	
    	body.addView(scroll);
    }
    
    private void showDialog() {
		//dialog = new Dialog(activity, android.R.style.Theme);
    	dialog = new Dialog(activity);
    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_showrealtimeline);
		
		final LinearLayout body = (LinearLayout) dialog.findViewById(R.id.body); 
		renderDeparture(body, data.expectedDeparture, data.realtime, data.stopVisitNote);
		for (NextDeparture departure : data.nextDepartures) {
			renderDeparture(body, departure.expectedDeparture, departure.realtime, departure.stopVisitNote);
		}
		
		dialog.show();
    }

	@Override
	public void stop() {
		dialog.dismiss();
	}
}
