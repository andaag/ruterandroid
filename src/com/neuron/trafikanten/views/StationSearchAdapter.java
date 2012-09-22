package com.neuron.trafikanten.views;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.Filterable;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.HelperFunctions.StreamWithTime;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.trafikanten.Trafikanten;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenSearch;
import com.neuron.trafikanten.dataSets.StationData;

//TODO PERFORMANCE :  LoaderManager/CursorLoader? (not needed when flags=0 with compat lib?)

public class StationSearchAdapter extends SimpleCursorAdapter implements
		Filterable {
	private static final String TAG = "Trafikanten-StationSearchAdapter";
	private boolean mRealtime = false;
	private Context mContext;

	public StationSearchAdapter(Context context) {
		super(context, R.layout.autocomplete_station, null, from, to, 0);
		mContext = context;
	}

	private final static String[] columns = { BaseColumns._ID,
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_ICON_1 };
	private final static String[] from = { SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_ICON_1 };
	private final static int[] to = { R.id.stopname, R.id.address, R.id.icon };

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		if (getFilterQueryProvider() != null) {
			return getFilterQueryProvider().runQuery(constraint);
		}
		if (constraint == null) {
			return null;
		}

		// Setup content provider cursor for data:
		MyMatrixCursor cursor = new MyMatrixCursor();

		String urlString;
		try {
			urlString = Trafikanten.getApiUrl()
					+ "/ReisRest/Place/Autocomplete/"
					+ HelperFunctions.properEncode(constraint.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return cursor;
		}
		/*
		 * if (mRealtime) { //FIXME : Filter on realtime during realtime search?
		 * urlString = urlString + "?autocompleteType=" + TODO; }
		 */

		try {
			Log.i(TAG, "Query url : " + urlString);
			final StreamWithTime streamWithTime = HelperFunctions
					.executeHttpRequest(mContext, new HttpGet(urlString), true);
			JSONArray jsonArray;
			jsonArray = new JSONArray(
					HelperFunctions.InputStreamToString(streamWithTime.stream));
			final int arraySize = jsonArray.length();
			for (int i = 0; i < arraySize; i++) {
				final JSONObject json = jsonArray.getJSONObject(i);

				final int id = json.getInt("ID");
				final String name = json.getString("Name");
				final String extra = json.getString("District");
				final int type = json.getInt("Type");

				StationData station = new StationData(name, extra, id, type);
				TrafikantenSearch.searchForAddress(station);

				cursor.addRow(station.stationId, station.stopName,
						station.extra, type);
			}

		} catch (JSONException e) {
			Log.e(TAG, "Exception during autocomplete");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "Exception during autocomplete");
			e.printStackTrace();
		}

		return cursor;
	}

	private static class MyMatrixCursor extends MatrixCursor {
		public MyMatrixCursor() {
			super(columns);
		}

		// ArrayList<Integer> mStationIds = new ArrayList<Integer>();

		public void addRow(int stationId, String stopName, String extra,
				int type) {
			// Prevents duplicates for when we add from history/favorites
			/*
			 * if (mStationIds.contains(stationId)) { return; }
			 * mStationIds.add(stationId);
			 */

			String id = Integer.toString(stationId);
			int icon = android.R.drawable.ic_menu_directions;
			switch (type) {
			case StationData.TYPE_REROUTE: // Reroute
				icon = android.R.drawable.ic_menu_revert;
				break;
			case StationData.TYPE_POI: // POI
				icon = android.R.drawable.ic_menu_view;
				break;
			case StationData.TYPE_ADDRESS: // Address
				icon = android.R.drawable.ic_menu_mapmode;
				break;
			}

			addRow(new String[] { id, stopName, extra, Integer.toString(icon) });
		}
	}
}
