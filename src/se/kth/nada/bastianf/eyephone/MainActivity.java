package se.kth.nada.bastianf.eyephone;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashSet;
import javax.net.ssl.SSLHandshakeException;
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
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// Identifier used for callback
	public static final int MAIN_ACTIVITY = 0;

	// Name of the favorites file
	private final String FAVORITES = "favorites";

	// The ListView that displays the subscribers registered on the network.
	private ListView list;

	// The data source used. Any changes in this array will be available to the
	// adapter.
	private ArrayList<Subscriber> data;

	// An adapter acts as a bridge between the list and the underlying data
	// source.
	private SubscriberAdapter adapter;

	private class Updater extends AsyncTask<Void, Void, ArrayList<Subscriber>> {
		AlertDialog.Builder dialog;
		Context context;
		Exception exception;

		public Updater(Context context) {
			this.context = context;
			dialog = new AlertDialog.Builder(context);
		}

		@Override
		protected void onPreExecute() {
			if (!connected()) {
				dialog.setTitle(R.string.io_exception_title);
				dialog.setMessage(R.string.no_internet_connection);
				dialog.setPositiveButton(R.string.ok, null);
				dialog.create().show();
				this.cancel(true);
				return;
			}
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.view_update,
					(ViewGroup) findViewById(R.id.notifications_holder));
		}

		@Override
		protected ArrayList<Subscriber> doInBackground(Void... arg0) {
			try {
				return NetworkRequester.getSubscribers();
			} catch (Exception e) {
				exception = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<Subscriber> result) {
			LinearLayout updateHolder = (LinearLayout) findViewById(R.id.update_holder);
			((ViewGroup) updateHolder.getParent()).removeView(updateHolder);
			if (exception != null) {
				if (exception instanceof IllegalPasswordException) {
					dialog.setTitle(R.string.client_config);
					dialog.setMessage(R.string.illegal_password_exception);
					dialog.setPositiveButton(R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									Intent intent = new Intent(context,
											LoginActivity.class);
									startActivity(intent);
								}

							});
					dialog.create().show();
				} else if (exception instanceof IOException) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					inflater.inflate(R.layout.view_retry,
							(ViewGroup) findViewById(R.id.notifications_holder));
				} else if (exception instanceof SSLHandshakeException) {
					dialog.setTitle(R.string.handshake_exception_title);
					dialog.setMessage(R.string.handshake_exception);
					dialog.setPositiveButton(R.string.ok, null);
					dialog.create().show();
				}
				return;
			}
			data.clear();
			data.addAll(result);
			adapter.notifyDataSetChanged();
			Toast.makeText(context, R.string.sync_successful,
					Toast.LENGTH_SHORT).show();
		}
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Do not trigger listeners
		SubscriberAdapter.listenersEnabled = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SubscriberAdapter.listenersEnabled = true;
		list = (ListView) findViewById(R.id.subscribers);
		// Load favorites from disk
		HashSet<Integer> favorites = new HashSet<Integer>();
		try {
			FileInputStream fis = openFileInput(FAVORITES);
			ObjectInputStream ois = new ObjectInputStream(fis);
			favorites = (HashSet<Integer>) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			// File or directory could not be found or cannot be accessed
		} catch (StreamCorruptedException e) {
			// The object is corrupt
		} catch (IOException e) {
			// Error while reading data
		} catch (ClassNotFoundException e) {
			// The class does not exist
		}
		// Load data from bundle
		ArrayList<Subscriber> filteredData = new ArrayList<Subscriber>();
		if (savedInstanceState == null) {
			data = new ArrayList<Subscriber>();
		} else {
			data = savedInstanceState.getParcelableArrayList("source");
			filteredData = savedInstanceState
					.getParcelableArrayList("filteredSource");
		}
		adapter = new SubscriberAdapter(this, data, filteredData);
		adapter.setFavorites(favorites);
		list.setAdapter(adapter);
		if (savedInstanceState == null) {
			Updater updater = new Updater(MainActivity.this);
			updater.execute();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		// Add sort menu
		MenuItem sortByName = menu.findItem(R.id.sort_by_name);
		sortByName.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				adapter.sort(Subscriber.getNameComparator());
				adapter.notifyDataSetChanged();
				return true;
			}
		});
		MenuItem sortByExtension = menu.findItem(R.id.sort_by_extension);
		sortByExtension
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						adapter.sort(Subscriber.getExtensionComparator());
						adapter.notifyDataSetChanged();
						return true;
					}
				});
		MenuItem sortById = menu.findItem(R.id.sort_by_id);
		sortById.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				adapter.sort(Subscriber.getIdComparator());
				adapter.notifyDataSetChanged();
				return true;
			}
		});
		MenuItem favoritesFirst = menu.findItem(R.id.favorites_first);
		favoritesFirst
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						adapter.favoritesFirst();
						return true;
					}
				});
		// Filter the list when the user enters text into the search view
		SearchView search = (SearchView) menu.findItem(R.id.search)
				.getActionView();
		search.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextChange(String newText) {
				adapter.getFilter().filter(newText);
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
		});
		// Update subscribers from server when users presses refresh button
		MenuItem refresh = (MenuItem) menu.findItem(R.id.refresh);
		refresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Updater updater = new Updater(MainActivity.this);
				updater.execute();
				return true;
			}
		});

		// Show about box
		MenuItem about = (MenuItem) menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						MainActivity.this);
				dialog.setTitle(R.string.about);
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View about = inflater.inflate(R.layout.view_about, null);
				dialog.setView(about);
				dialog.setNegativeButton(R.string.ok, null);
				dialog.setPositiveButton(R.string.email_title,
						new OnClickListener() {

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
		// Show statistics
		MenuItem statistics = (MenuItem) menu.findItem(R.id.statistics);
		statistics.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(MainActivity.this,
						StatisticsActivity.class);
				startActivity(intent);
				return true;
			}
		});
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == MAIN_ACTIVITY) {
				Subscriber returnData = (Subscriber) intent
						.getParcelableExtra("subscriber");
				data.remove(adapter.getCurrentPos());
				data.add(returnData);
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Do not trigger listeners
		SubscriberAdapter.listenersEnabled = false;
		super.onSaveInstanceState(outState);
		SubscriberAdapter.listenersEnabled = true;
		// Save the data source, so we can reinitialize it later
		outState.putParcelableArrayList("source", data);
		outState.putParcelableArrayList("filteredSource",
				adapter.getFilteredData());
		// Save selected favorites to disk
		saveFavorites();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		saveFavorites();
	}

	private void saveFavorites() {
		try {
			FileOutputStream fos = openFileOutput(FAVORITES,
					Context.MODE_PRIVATE);
			ObjectOutputStream ous = new ObjectOutputStream(fos);
			ous.writeObject(adapter.getFavorites());
			ous.close();
		} catch (FileNotFoundException e) {
			// File cannot be accessed or the directory does not exist
		} catch (IOException e) {
			// An error occurred while writing the object
		}
	}

	/**
	 * Try to reconnect to server. Called after IOException failure.
	 * 
	 * @param v
	 *            The view pressed.
	 */
	public void tryAgain(View v) {
		LinearLayout retryHolder = (LinearLayout) findViewById(R.id.retry_holder);
		((ViewGroup) retryHolder.getParent()).removeView(retryHolder);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_update,
				(ViewGroup) findViewById(R.id.notifications_holder));
		Updater updater = new Updater(this);
		updater.execute();
	}

	private boolean connected() {
		NetworkInfo ni = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}
}