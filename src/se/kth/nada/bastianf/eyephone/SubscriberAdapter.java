package se.kth.nada.bastianf.eyephone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import javax.net.ssl.SSLHandshakeException;
import se.kth.nada.bastianf.eyephone.R;
import se.kth.nada.bastianf.eyephone.SlideLayout.Direction;
import se.kth.nada.bastianf.eyephone.SlideLayout.OnSlideCompleteListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A custom ArrayAdapter for subscribers with a name, telephone number and
 * access state. Uses an array of Subscriber objects as underlying data source.
 * 
 * @author Bastian Fredriksson and Peter Caprioli
 * @version
 */
public class SubscriberAdapter extends ArrayAdapter<Subscriber> implements
		Filterable {
	public static boolean listenersEnabled = true;

	private ArrayList<Subscriber> subscribers; // All subscribers displayed on
												// screen
	private ArrayList<Subscriber> unvisibleSubscribers; // Subscribers filtered
														// away by user
	private final Context context;

	private HashSet<Integer> favorites;

	// The position of the last selected subscriber
	private int currentPos;

	private class Authorizer extends AsyncTask<Void, Void, Void> {
		Subscriber subscriber;
		Boolean isChecked;
		Exception exception;
		AlertDialog.Builder dialog;
		CompoundButton buttonView;

		public Authorizer(Subscriber subscriber, Boolean isChecked,
				CompoundButton buttonView) {
			this.buttonView = buttonView;
			this.subscriber = subscriber;
			this.isChecked = isChecked;
			this.dialog = new AlertDialog.Builder(context);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				subscriber.authorize(isChecked);
			} catch (Exception e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			listenersEnabled = false;
			if (exception == null) {
				listenersEnabled = true;
				return;
			}
			if (exception instanceof SSLHandshakeException) {
				buttonView.toggle();
				Toast.makeText(context, R.string.handshake_exception,
						Toast.LENGTH_SHORT).show();
			} else if (exception instanceof IOException) {
				buttonView.toggle();
				Toast.makeText(context, R.string.io_exception,
						Toast.LENGTH_SHORT).show();
			} else if (exception instanceof IllegalPasswordException) {
				buttonView.toggle();
				dialog = new AlertDialog.Builder(context);
				dialog.setTitle(R.string.illegal_password_exception);
				dialog.setMessage(R.string.client_config);
				dialog.setPositiveButton(R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(context, LoginActivity.class);
						context.startActivity(intent);
					}
				});
				dialog.create().show();
			}
			listenersEnabled = true;
		}
	}

	public SubscriberAdapter(Context context, ArrayList<Subscriber> data,
			ArrayList<Subscriber> filteredData) {
		super(context, R.layout.view_subscriber, data);
		this.context = context;
		this.subscribers = data;
		this.favorites = new HashSet<Integer>();
		this.unvisibleSubscribers = filteredData;
	}

	@Override
	public Filter getFilter() {
		return new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				if (constraint == null) {
					subscribers.addAll(unvisibleSubscribers);
					unvisibleSubscribers.clear();
					return null;
				}
				int skip = 0; // Number of newly added elements to skip
				// Filter using the subscriber's extension and name
				constraint = constraint.toString().toLowerCase(Locale.US);
				for (int i = subscribers.size() - 1; i >= 0; i--) {
					String name = subscribers.get(i).getName();
					String extension = String.valueOf(subscribers.get(i)
							.getExtension());
					if (!name.toLowerCase(Locale.US).startsWith(
							constraint.toString())
							&& !extension.startsWith(constraint.toString())) {
						unvisibleSubscribers.add(subscribers.remove(i));
						skip++;
					}
				}
				for (int i = unvisibleSubscribers.size() - skip - 1; i >= 0; i--) {
					String name = unvisibleSubscribers.get(i).toString();
					String extension = String.valueOf(unvisibleSubscribers.get(
							i).getExtension());
					if (name.toLowerCase(Locale.US).startsWith(
							constraint.toString())
							|| extension.startsWith(constraint.toString())) {
						subscribers.add(unvisibleSubscribers.remove(i));
					}
				}
				// We edit the data source directly, so there is nothing to
				// return
				return null;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				notifyDataSetChanged();
			}
		};
	}

	@Override
	public int getCount() {
		return subscribers.size();
	}

	@Override
	public View getView(int pos, View oldView, ViewGroup parent) {
		SlideLayout subscriberView = (SlideLayout) oldView;
		if (subscriberView == null) {
			// Inflate a new subscriber view from XML
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			subscriberView = (SlideLayout) inflater.inflate(
					R.layout.view_subscriber, parent, false);
		}
		// Retrieve fields
		TextView name = (TextView) subscriberView
				.findViewById(R.id.subscriber_name);
		TextView number = (TextView) subscriberView
				.findViewById(R.id.subscriber_number);
		CheckBox authorized = (CheckBox) subscriberView
				.findViewById(R.id.authorized);
		View status = (View) subscriberView
				.findViewById(R.id.subscriber_status);
		CheckBox favorite = (CheckBox) subscriberView
				.findViewById(R.id.favorite);
		// Reset listeners (important!)
		authorized.setOnCheckedChangeListener(null);
		favorite.setOnCheckedChangeListener(null);
		// Set values
		String subscriberName = subscribers.get(pos).getName();
		if (subscriberName.equals("")) {
			subscriberName = "N/A";
			name.setTextColor(Color.GRAY);
		}
		name.setText(subscriberName);
		number.setText("" + subscribers.get(pos).getExtension());
		authorized.setChecked(subscribers.get(pos).isAuthorized());
		favorite.setChecked(favorites.contains(subscribers.get(pos).getID()));
		// If subscriber is online: set a green label (red by default)
		if (subscribers.get(pos).isOnline()) {
			status.setBackgroundColor(Color.GREEN);
		} else {
			status.setBackgroundColor(Color.RED);
		}
		// Add listeners, only triggered if listenersEnabled == true
		final int position = pos;
		authorized.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (listenersEnabled) {
					Authorizer authorizer = new Authorizer(subscribers
							.get(position), isChecked, buttonView);
					authorizer.execute();
				}
			}
		});
		favorite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (listenersEnabled) {
					if (isChecked) {
						// Add subscriber ID to favorites
						favorites.add(subscribers.get(position).getID());
					} else {
						favorites.remove(subscribers.get(position).getID());
					}
				}
			}
		});
		// Handle layout events
		subscriberView
				.setOnSlideCompleteListener(new OnSlideCompleteListener() {

					@Override
					public void onSlideComplete(Direction dir) {
						if (dir == Direction.LEFT) {
							String uri = "tel:"
									+ subscribers.get(position).getExtension();
							Intent callIntent = new Intent(Intent.ACTION_CALL,
									Uri.parse(uri));
							context.startActivity(callIntent);
						} else if (dir == Direction.RIGHT) {
							Intent intent = new Intent(context,
									SMSActivity.class);
							intent.putExtra("subscriber",
									subscribers.get(position));
							ArrayList<Subscriber> allSubscribers = new ArrayList<Subscriber>();
							allSubscribers.addAll(subscribers);
							allSubscribers.addAll(unvisibleSubscribers);
							intent.putExtra("recipients", allSubscribers);
							context.startActivity(intent);
						} else if (dir == Direction.NONE) {
							Subscriber currentSubscriber = subscribers
									.get(position);
							currentPos = position;
							Intent intent = new Intent(context,
									SubscriberActivity.class);
							// Pass the all subscribers to the new activity
							intent.putExtra("subscriber", currentSubscriber);
							ArrayList<Subscriber> allSubscribers = new ArrayList<Subscriber>();
							allSubscribers.addAll(subscribers);
							allSubscribers.addAll(unvisibleSubscribers);
							intent.putExtra("recipients", allSubscribers);
							((Activity) context).startActivityForResult(intent,
									MainActivity.MAIN_ACTIVITY);
						}
					}
				});
		return (View) subscriberView;
	}

	public int getCurrentPos() {
		return currentPos;
	}

	/**
	 * Put visible subscribers marked as 'favorite' in the beginning of the list
	 * and refresh the adapter.
	 */
	public void favoritesFirst() {
		ArrayList<Subscriber> favoriteSubscribers = new ArrayList<Subscriber>();
		for (int i = subscribers.size() - 1; i >= 0; i--) {
			if (favorites.contains(subscribers.get(i).getID())) {
				// Subscriber is a favorite
				favoriteSubscribers.add(subscribers.remove(i));
			}
		}
		// Merge lists
		subscribers.addAll(0, favoriteSubscribers);
		notifyDataSetChanged();
	}

	/**
	 * Get favorites.
	 * 
	 * @return A set of favorites.
	 */
	public HashSet<Integer> getFavorites() {
		return favorites;
	}

	/**
	 * Set favorites.
	 * 
	 * @param favorites
	 */
	public void setFavorites(HashSet<Integer> favorites) {
		this.favorites = favorites;
	}

	public ArrayList<Subscriber> getFilteredData() {
		return unvisibleSubscribers;
	}
}