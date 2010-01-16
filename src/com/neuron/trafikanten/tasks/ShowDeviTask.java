package com.neuron.trafikanten.tasks;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;

public class ShowDeviTask implements GenericTask {
	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private Activity activity;
    private DeviData data;
    private Dialog dialog;
    
    public ShowDeviTask(Activity activity, DeviData data) 
    {
        this.activity = activity;
        this.data = data;
        showDialog();
    }
    
    private CharSequence stripCode(String data) {
    	String r = data.replaceAll("<br />", "\n").replaceAll("\n\n", "\n");
    	if (r.endsWith("\n")) {
    		return r.subSequence(0, r.length() - 1);
    	}
    	return r;
    }
    
    private void showDialog() {
		dialog = new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_showdevi);
		
		final TextView validPeriod = (TextView) dialog.findViewById(R.id.validPeriod);
		final TextView title = (TextView) dialog.findViewById(R.id.title);
		final TextView description = (TextView) dialog.findViewById(R.id.description);
		final TextView body = (TextView) dialog.findViewById(R.id.devibody);
		
		validPeriod.setText("Gyldig fra " + dateFormater.format(data.validFrom).toString());
		title.setText(stripCode(data.title));
		description.setText(stripCode(data.description));
		
		final CharSequence bodyText = stripCode(data.body);
		if (bodyText.length() < 3) {
			body.setVisibility(View.GONE);
		}
		else {
			body.setText(stripCode(data.body));
		}
		
		dialog.show();
    }
    
	@Override
	public void stop() {
		dialog.dismiss();
	}

}
