package se.kth.nada.bastianf.eyephone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.net.ssl.SSLHandshakeException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

public class SMSActivity extends Activity {
	private AutoCompleteTextView recipients;
	private EditText message;
	private Switch broadcast;
	private ArrayList<Subscriber> subscribers;
	private RecipientsCompletionsAdapter adapter;
	private HashMap<Button, String> sendingList;

	private class sender extends AsyncTask<String, Integer, Boolean> {
		private Context context;
		private Exception exception;
		private int messagesNotSent;
		private Dialog dialog;

		public sender(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View progress = inflater.inflate(R.layout.view_progress, null);
			builder.setView(progress);
			dialog = builder.create();
			dialog.show();
		}

		/**
		 * Send an SMS in a separate thread to the subscribers defined in
		 * params.
		 * 
		 * @param params
		 *            The message to send, followed by the IDs of one or more
		 *            subscribers.
		 */
		@Override
		protected Boolean doInBackground(String... params) {
			messagesNotSent = params.length - 1;
			try {
				String msg = params[0];
				int[] ids = new int[params.length - 1];
				for (int i = 1; i < params.length; i++) {
					ids[i - 1] = Integer.parseInt(params[i]);
				}
				// Send msg to ids
				for (int i = 0; i < ids.length; i++) {
					if (NetworkRequester.sendSMS(ids[i], msg)) {
						messagesNotSent--;
					}
				}
			} catch (Exception e) {
				exception = e;
			}
			if (messagesNotSent == 0) {
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			dialog.dismiss();
			if (success) {
				// All messages was sent successfully, go back to previous
				// activity
				Toast.makeText(context, "Debug", Toast.LENGTH_LONG);
				finish();
				return;
			}
			if (exception == null) {
				Toast.makeText(
						context,
						"Sending to " + messagesNotSent + " recipients failed.",
						Toast.LENGTH_LONG).show();
				return;
			}
			if (exception instanceof UnsupportedEncodingException) {
				Toast.makeText(
						context,
						R.string.encoding_not_supported + " Sending to "
								+ messagesNotSent + " recipients failed.",
						Toast.LENGTH_LONG).show();
			} else if (exception instanceof IOException) {
				Toast.makeText(
						context,
						R.string.io_exception + " Sending to "
								+ messagesNotSent + " recipients failed.",
						Toast.LENGTH_LONG).show();
			} else if (exception instanceof NumberFormatException) {
				Toast.makeText(
						context,
						R.string.io_exception + " Sending to "
								+ messagesNotSent + " recipients failed.",
						Toast.LENGTH_LONG).show();
			} else if (exception instanceof SSLHandshakeException) {
				Toast.makeText(
						context,
						R.string.handshake_exception + " Sending to "
								+ messagesNotSent + " recipients failed.",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms);
		getActionBar().setTitle(R.string.sendSMS);
		// Get fields
		recipients = (AutoCompleteTextView) findViewById(R.id.recipients);
		message = (EditText) findViewById(R.id.message);
		broadcast = (Switch) findViewById(R.id.broadcast);
		// Get data from intent
		Intent intent = getIntent();
		Subscriber subscriber = intent.getParcelableExtra("subscriber");
		// This cast should be OK, unless we put something fishy in bundle
		subscribers = (ArrayList<Subscriber>) intent
				.getSerializableExtra("recipients");
		// Get data from bundle
		sendingList = new HashMap<Button, String>();
		ArrayList<Subscriber> filteredData = new ArrayList<Subscriber>();
		LinearLayout selectedRecipients = (LinearLayout) findViewById(R.id.selected_recipients);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (savedInstanceState != null) {
			ArrayList<String> names = (ArrayList<String>) savedInstanceState
					.getSerializable("recipientNames");
			String[] recipientIDs = (String[]) savedInstanceState
					.getStringArray("recipientIDs");
			for (int i = 0; i < recipientIDs.length; i++) {
				View view = inflater.inflate(R.layout.view_selected_recipient,
						selectedRecipients, false);
				Button button = (Button) view;
				button.setText(names.get(i));
				button.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						sendingList.remove(v);
						ViewGroup parent = (ViewGroup) v.getParent();
						parent.removeView(v);
					}
				});
				selectedRecipients.addView(view);
				sendingList.put(button, recipientIDs[i]);
			}
		} else {
			// Add recipient selected on previous screen
			View view = inflater.inflate(R.layout.view_selected_recipient,
					selectedRecipients, false);
			Button button = (Button) view;
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					sendingList.remove(v);
					ViewGroup parent = (ViewGroup) v.getParent();
					parent.removeView(v);
				}
			});
			button.setText(subscriber.getName());
			selectedRecipients.addView(view);
			sendingList.put(button, String.valueOf(subscriber.getID()));
		}
		adapter = new RecipientsCompletionsAdapter(this, subscribers,
				filteredData);
		recipients.setAdapter(adapter);
		broadcast.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				recipients.setEnabled(!isChecked);
				recipients.setText("");
				LinearLayout selectedRecipients = (LinearLayout) findViewById(R.id.selected_recipients);
				selectedRecipients.removeAllViews();
			}
		});
		recipients.setOnItemClickListener(new OnItemClickListener() {

			/**
			 * Add a recipient.
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Subscriber currentSubscriber = subscribers.get(position);
				String subscriberID = String.valueOf(currentSubscriber.getID());
				// Check if this recipient is present
				if (sendingList.values().contains(subscriberID)) {
					Toast.makeText(SMSActivity.this,
							R.string.recipient_already_added, Toast.LENGTH_LONG)
							.show();
					recipients.setText("");
					return;
				}
				String subscriberName = currentSubscriber.getName();
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View selectedRecipient = inflater.inflate(
						R.layout.view_selected_recipient, parent, false);
				Button button = (Button) selectedRecipient
						.findViewById(R.id.selected_recipient);
				button.setText(subscriberName);
				button.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						sendingList.remove(v);
						ViewGroup parent = (ViewGroup) v.getParent();
						parent.removeView(v);
					}
				});
				sendingList.put(button, subscriberID);
				LinearLayout selectedRecipients = (LinearLayout) findViewById(R.id.selected_recipients);
				selectedRecipients.addView(selectedRecipient);
				recipients.setText("");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sms, menu);
		// Show about box
		MenuItem about = (MenuItem) menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						SMSActivity.this);
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
		// Send SMS action item
		MenuItem sendAction = (MenuItem) menu.findItem(R.id.sendSMS_action);
		sendAction.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sendSMS();
				return true;
			}
		});
		// Send SMS menu item
		MenuItem send = (MenuItem) menu.findItem(R.id.sendSMS);
		send.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sendSMS();
				return true;
			}
		});
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList("filteredSource",
				adapter.getFilteredData());
		LinearLayout selectedRecipients = (LinearLayout) findViewById(R.id.selected_recipients);
		ArrayList<String> recipientNames = new ArrayList<String>();
		for (int i = 0; i < selectedRecipients.getChildCount(); i++) {
			recipientNames.add(((Button) selectedRecipients.getChildAt(i))
					.getText().toString());
		}
		String[] recipientIDs = sendingList.values().toArray(
				new String[sendingList.size()]);
		outState.putSerializable("recipientNames", recipientNames);
		outState.putStringArray("recipientIDs", recipientIDs);
	}

	/**
	 * Send an SMS.
	 * 
	 * @param v
	 *            The view that triggered this action.
	 */
	public void sendSMS() {
		String msg = message.getText().toString();
		String[] ids;
		if (broadcast.isChecked()) {
			ids = new String[] { "0" };
		} else {
			ids = sendingList.values().toArray(new String[sendingList.size()]);
			if (ids.length == 0) {
				// No recipients to send msg to
				Toast.makeText(this, R.string.no_recipients, Toast.LENGTH_SHORT)
						.show();
				return;
			}
		}
		ArrayList<String> params = new ArrayList<String>(Arrays.asList(ids));
		params.add(0, msg);
		new sender(this).execute(params.toArray(new String[params.size()]));
	}
}
