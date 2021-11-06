package se.kth.nada.bastianf.eyephone;

import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;
import se.kth.nada.bastianf.eyephone.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * SubscriberActivity displays a screen with detailed information about a
 * subscriber on the GSM network.
 * 
 * @author Bastian Fredriksson and Peter Caprioli
 * @version
 */
public class SubscriberActivity extends Activity {
	private Subscriber subscriber;
	private boolean isEditing = false;

	private class SubscriberUpdater extends AsyncTask<Void, Void, Void> {
		private Exception exception;
		private String newName;
		private int newExtension;
		private AlertDialog.Builder dialog;

		public SubscriberUpdater(String newName, int newExtension,
				Context context) {
			this.newName = newName;
			this.newExtension = newExtension;
			this.dialog = new AlertDialog.Builder(context);
		}

		@Override
		protected void onPreExecute() {
			if (!connected()) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						SubscriberActivity.this);
				dialog.setTitle(R.string.io_exception_title);
				dialog.setMessage(R.string.no_internet_connection);
				dialog.setPositiveButton(R.string.ok, null);
				dialog.create().show();
				this.cancel(true);
				return;
			}
			hideEditView();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.view_update,
					(ViewGroup) findViewById(R.id.edit_view_holder));
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				subscriber.changeName(newName);
				subscriber.changeExtension(newExtension);
			} catch (Exception e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			LinearLayout updateHolder = (LinearLayout) findViewById(R.id.update_holder);
			((ViewGroup) updateHolder.getParent()).removeView(updateHolder);
			if (exception == null) {
				// Update fields
				TextView number = (TextView) findViewById(R.id.extension);
				number.setText(String.valueOf(subscriber.getExtension()));
				String name = subscriber.getName();
				if (name.equals("")) {
					name = String.valueOf(subscriber.getExtension());
				}
				getActionBar().setTitle(name);
				// Display toast message telling user that the operation was successful
				Toast.makeText(SubscriberActivity.this,
						R.string.subscriber_updated, Toast.LENGTH_SHORT).show();
				return;
			}
			if (exception instanceof IOException) {
				Toast.makeText(dialog.getContext(), R.string.io_exception,
						Toast.LENGTH_SHORT).show();
			} else if (exception instanceof IllegalPasswordException) {
				dialog.setTitle(R.string.client_config);
				dialog.setMessage(R.string.illegal_password_exception);
				dialog.setPositiveButton(R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Intent intent = new Intent(dialog.getContext(),
								LoginActivity.class);
						startActivity(intent);
					}

				});
				dialog.create().show();
			} else if (exception instanceof SSLHandshakeException) {
				dialog.setTitle(R.string.handshake_exception_title);
				dialog.setMessage(R.string.handshake_exception);
				dialog.setPositiveButton(R.string.ok, null);
				dialog.create().show();
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_subscriber);

		// Get the subscriber selected by the user on the previous screen.
		Intent intent = getIntent();
		subscriber = (Subscriber) intent.getParcelableExtra("subscriber");

		// Set the action bar title to the name of the current subscriber, 
		// or the extension if the subscriber has no name
		String name = subscriber.getName();
		if (name.equals("")) {
			name = "" + subscriber.getExtension();
		}
		getActionBar().setTitle(name);

		// Set subscriber details
		LinearLayout root = (LinearLayout) findViewById(R.id.subscriber_holder);
		TextView extension = (TextView) root.findViewById(R.id.extension);
		TextView id = (TextView) root.findViewById(R.id.id);
		TextView station = (TextView) root.findViewById(R.id.station);
		TextView imei = (TextView) root.findViewById(R.id.imei);
		TextView imsi = (TextView) root.findViewById(R.id.imsi);
		TextView tmsi = (TextView) root.findViewById(R.id.tmsi);
		extension.setText(Integer.toString(subscriber.getExtension()));
		id.setText(Integer.toString(subscriber.getID()));
		station.setText(Integer.toString(subscriber.getStation()));
		imei.setText(subscriber.getIMEI());
		imsi.setText(subscriber.getIMSI());
		tmsi.setText(subscriber.getTMSI());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.subscriber, menu);

		// Create edit button
		MenuItem edit = (MenuItem) menu.findItem(R.id.edit);
		edit.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (!isEditing) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View editView = inflater.inflate(R.layout.view_edit,
							(ViewGroup) findViewById(R.id.edit_view_holder));
					EditText setName = (EditText) editView
							.findViewById(R.id.edit_name_field);
					EditText setExtension = (EditText) editView
							.findViewById(R.id.edit_extension_field);
					setName.setText(subscriber.getName());
					setExtension.setText("" + subscriber.getExtension());
					isEditing = true;
				} else {
					hideEditView();
				}
				return true;
			}
		});

		// Show about box
		MenuItem about = (MenuItem) menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						SubscriberActivity.this);
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
		// Call
		MenuItem call = (MenuItem) menu.findItem(R.id.call);
		call.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				makeCall(null);
				return true;
			}
		});
		// Send SMS
		MenuItem sendSMS = (MenuItem) menu.findItem(R.id.sendSMS);
		sendSMS.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sendSMS(null);
				return true;
			}
		});
		return true;
	}

	@Override
	public void onBackPressed() {
		// We're going back to MainActivity, submit any changes
		Intent returnIntent = new Intent();
		returnIntent.putExtra("subscriber", subscriber);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	private void hideEditView() {
		LinearLayout editView = (LinearLayout) findViewById(R.id.edit_subscriber);
		((ViewGroup) editView.getParent()).removeView(editView);
		isEditing = false;
	}

	/**
	 * Make a call to the subscriber.
	 * 
	 * @param v
	 *            The view pressed.
	 */
	public void makeCall(View v) {
		String uri = "tel:" + subscriber.getExtension();
		Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
		startActivity(callIntent);
	}

	/**
	 * Send an SMS to the subscriber.
	 * 
	 * @param v
	 *            The view pressed.
	 */
	public void sendSMS(View v) {
		Intent intent = new Intent(this, SMSActivity.class);
		intent.putExtra("subscriber", subscriber);
		intent.putExtra("recipients", getIntent().getSerializableExtra("recipients"));
		startActivity(intent);
	}

	/**
	 * Sync the subscriber's name and extension with the server.
	 * 
	 * @param v
	 *            The view pressed.
	 */
	public void saveSubscriber(View v) {
		EditText nameField = (EditText) findViewById(R.id.edit_name_field);
		EditText extensionField = (EditText) findViewById(R.id.edit_extension_field);
		String newName = nameField.getText().toString();
		try {
			int newExtension = Integer.parseInt(extensionField.getText()
					.toString()); // May throw NumberFormatException
			SubscriberUpdater updater = new SubscriberUpdater(newName,
					newExtension, this);
			updater.execute();
		} catch (NumberFormatException e) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.parsing_error_title);
			dialog.setMessage(R.string.extension_error);
			dialog.setPositiveButton(R.string.ok, null);
			dialog.create().show();
		}
	}

	private boolean connected() {
		NetworkInfo ni = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}
}
