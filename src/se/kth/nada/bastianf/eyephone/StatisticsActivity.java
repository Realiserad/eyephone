package se.kth.nada.bastianf.eyephone;

import java.io.IOException;
import java.util.ArrayList;

import javax.net.ssl.SSLHandshakeException;
import se.kth.nada.bastianf.eyephone.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class StatisticsActivity extends Activity {
	private ListView list;
	private ArrayList<GSMStatisticsObject> data;
	private StatsAdapter adapter;

	private class StatsAdapter extends ArrayAdapter<GSMStatisticsObject> {
		private final Context context;
		private ArrayList<GSMStatisticsObject> data;

		public StatsAdapter(Context context, ArrayList<GSMStatisticsObject> data) {
			super(context, R.layout.view_statistic, data);
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(int pos, View oldView, ViewGroup parent) {
			View statisticView = oldView;
			if (statisticView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				statisticView = inflater.inflate(R.layout.view_statistic,
						parent, false);
			}
			// Get fields
			TextView statName = (TextView) statisticView
					.findViewById(R.id.stat_name);
			TextView statValue = (TextView) statisticView
					.findViewById(R.id.stat_value);
			// Set fields
			statName.setText(data.get(pos).description);
			statValue.setText("" + data.get(pos).count);
			return statisticView;
		}
	}

	private class StatDownloader extends
			AsyncTask<Void, Void, ArrayList<GSMStatisticsObject>> {
		private AlertDialog.Builder dialog;
		private Exception exception;

		public StatDownloader(Context context) {
			dialog = new AlertDialog.Builder(context);
		}

		@Override
		protected void onPreExecute() {
			if(!connected()) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View root = inflater.inflate(
						R.layout.view_retry,
						(ViewGroup) findViewById(R.id.stats_notifications_holder));
				TextView errorMsg = (TextView) root
						.findViewById(R.id.error_message);
				errorMsg.setText(R.string.no_internet_connection_title);
				this.cancel(true);
				return;
			}
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.view_update,
					(ViewGroup) findViewById(R.id.stats_notifications_holder));
		}

		@Override
		protected ArrayList<GSMStatisticsObject> doInBackground(Void... arg0) {
			try {
				ArrayList<GSMStatisticsObject> result = NetworkRequester
						.getStatistics();
				return result;
			} catch (Exception e) {
				exception = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<GSMStatisticsObject> result) {
			LinearLayout updateHolder = (LinearLayout) findViewById(R.id.update_holder);
			((ViewGroup) updateHolder.getParent()).removeView(updateHolder);
			// Check for exceptions
			if (result == null) {
				if (exception instanceof IllegalPasswordException) {
					dialog.setTitle(R.string.server_config);
					dialog.setMessage(R.string.illegal_password_exception);
					dialog.setPositiveButton(R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// Open login prompt
									Intent intent = new Intent(
											StatisticsActivity.this,
											LoginActivity.class);
									startActivity(intent);
								}
							});
					dialog.create().show();
				} else if (exception instanceof IOException) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					inflater.inflate(
							R.layout.view_retry,
							(ViewGroup) findViewById(R.id.stats_notifications_holder));
				} else if (exception instanceof SSLHandshakeException) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View root = inflater
							.inflate(
									R.layout.view_retry,
									(ViewGroup) findViewById(R.id.stats_notifications_holder));
					TextView errorMsg = (TextView) root
							.findViewById(R.id.error_message);
					errorMsg.setText(R.string.handshake_exception_title);
				}
				return;
			}
			// Populate the list with downloaded stats
			data.addAll(result);
			adapter.notifyDataSetChanged();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_statistics);
		getActionBar().setTitle(R.string.statistics);
		list = (ListView) findViewById(R.id.stats);
		if (savedInstanceState == null) {
			data = new ArrayList<GSMStatisticsObject>();
		} else {
			data = savedInstanceState.getParcelableArrayList("stats");
		}
		adapter = new StatsAdapter(StatisticsActivity.this, data);
		list.setAdapter(adapter);
		if (savedInstanceState == null) {
			StatDownloader statDown = new StatDownloader(
					StatisticsActivity.this);
			statDown.execute();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.statistics, menu);

		// Show about box
		MenuItem about = (MenuItem) menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						StatisticsActivity.this);
				dialog.setTitle(R.string.about);
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View about = inflater.inflate(R.layout.view_about, null);
				dialog.setView(about);
				dialog.setNegativeButton(R.string.ok, null);
				dialog.setPositiveButton(R.string.email_title,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Send email
								Uri uri = Uri.parse("mailto:bastianf@kth.se");
								Intent intent = new Intent(
										Intent.ACTION_SENDTO, uri);
								startActivity(intent);
							}
						});
				dialog.create().show();
				return true;
			}
		});
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList("stats", data);
	}

	/**
	 * Retry to download statistics. Executed manually after an exception has
	 * been thrown.
	 * 
	 * @param v
	 *            The view pressed.
	 */
	public void tryAgain(View v) {
		LinearLayout retryHolder = (LinearLayout) findViewById(R.id.retry_holder);
		((ViewGroup) retryHolder.getParent()).removeView(retryHolder);
		StatDownloader statsDown = new StatDownloader(this);
		statsDown.execute();
	}
	
	private boolean connected() {
		NetworkInfo ni = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}
}
