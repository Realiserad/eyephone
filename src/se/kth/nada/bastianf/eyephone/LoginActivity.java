package se.kth.nada.bastianf.eyephone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.net.ssl.SSLHandshakeException;
import se.kth.nada.bastianf.eyephone.R;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class LoginActivity extends Activity {
	// TextView where user can enter a password
	private TextView password;

	// Submit button
	private Button submit;

	private class CheckLogin extends AsyncTask<String, Void, Boolean> {
		private Exception exception;
		private Context context;
		private AlertDialog.Builder errorBuilder;
		private Dialog waitDialog;

		public CheckLogin(Context context) {
			this.context = context;
			errorBuilder = new AlertDialog.Builder(context);
		}
		
		@Override
		protected void onPreExecute() {
			// Create wait dialog
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.view_progress, null, false);
			TextView progressMessage = (TextView) view.findViewById(R.id.progress_message);
			progressMessage.setText(R.string.logging_in);
			AlertDialog.Builder waitBuilder = new AlertDialog.Builder(context);
			waitBuilder.setView(view);
			waitDialog = waitBuilder.create();
		}

		@Override
		protected Boolean doInBackground(String... args) {
			try {
				return NetworkRequester.checkLogin(args[0]);
			} catch (Exception e) {
				exception = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			waitDialog.dismiss();
			if (result == null) {
				// NetworkRequester threw an exception
				if (exception instanceof IOException) {
					errorBuilder.setTitle(R.string.io_exception_title);
					errorBuilder.setMessage(R.string.io_exception);
					errorBuilder.setPositiveButton(R.string.ok, null);
					errorBuilder.create().show();
				} else if (exception instanceof SSLHandshakeException) {
					errorBuilder.setTitle(R.string.handshake_exception_title);
					errorBuilder.setMessage(R.string.handshake_exception);
					errorBuilder.setPositiveButton(R.string.ok, null);
					errorBuilder.create().show();
				} else if (exception instanceof UnsupportedEncodingException) {
					errorBuilder.setTitle(R.string.encoding_failed);
					errorBuilder.setMessage(R.string.encoding_not_supported);
					errorBuilder.setPositiveButton(R.string.ok, null);
					errorBuilder.create().show();
				}
				return;
			}
			
			// We don't care if the login succeeded or not, clear out the password once it has been submitted
			password.setText("");
			
			if (result) {
				// Password is valid, redirect to main activity
				Intent intent = new Intent(context, MainActivity.class);
				startActivity(intent);
			} else {
				// Invalid password, display error message
				errorBuilder.setTitle(R.string.client_config);
				errorBuilder.setMessage(R.string.illegal_password_exception);
				errorBuilder.setPositiveButton(R.string.ok, null);
				errorBuilder.create().show();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		password = (TextView) findViewById(R.id.password);
		submit = (Button) findViewById(R.id.submit);

		// Create a listener triggered when user want to log in
		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (connected()) {
					String currentPassword = password.getText().toString();
					// Send currentPassword to server and check if it's valid
					CheckLogin task = new CheckLogin(LoginActivity.this);
					task.execute(currentPassword);
				} else {
					AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
					dialog.setTitle(R.string.io_exception_title);
					dialog.setMessage(R.string.no_internet_connection);
					dialog.setPositiveButton(R.string.ok, null);
					dialog.create().show();
				}
			}
		});
		// Warn the user if (s)he is not connected
		if (!connected()) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View root = inflater.inflate(R.layout.view_reconnect,
					(ViewGroup) findViewById(R.id.notifications_holder_login));
			root.findViewById(R.id.close_msg).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							ViewGroup reconnectHolder = (ViewGroup) v
									.getParent();
							ViewGroup notHolder = (ViewGroup) reconnectHolder
									.getParent();
							notHolder.removeView(reconnectHolder);
						}
					});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// Clear the password, if there was one before
		NetworkRequester.resetGlobalPassword();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);

		// Show about box
		MenuItem about = (MenuItem) menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						LoginActivity.this);
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
	public void onBackPressed() {
		finish();
	}

	private boolean connected() {
		NetworkInfo ni = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}
}
