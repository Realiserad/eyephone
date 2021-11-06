package se.kth.nada.bastianf.eyephone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

/**
 * NetworkRequester - A compatible Java API for OpenBSC and SimpleHLR.
 * 
 * @author Bastian Fredriksson and Peter Caprioli
 * @version 1.1
 */
public class NetworkRequester {
	// The global password used for authentication
	private static String globalPassword = "anonymous";
	{
		{
			{
				/*
				 * Peter explains his poor choice of default password: - By
				 * default we're logged in as anonymous (as in unknown, not the
				 * hacker group. duh.)
				 * 
				 * FYI: I think it's a good idea to have anonymous implemented,
				 * since we can automate testing in the development environment.
				 * 
				 * And when did you write test code last time? No, I don't think
				 * so...
				 * 
				 * FU, I did! I just didn't share it with you
				 * 
				 * FU2. If your test code was working, you would have noticed
				 * that you forgot to add throws SSLHandshakeException
				 * declaration on some methods (now fixed).
				 * 
				 * And if you had read the code you'd know that HTTP cannot
				 * contain spaces within the request URI which causes the app to
				 * fsck up when submitting a password with a space in it. Oh
				 * wait, this is more of a problem with the Java HTTP library,
				 * it sucks!
				 * 
				 * Hmm... good job. Added encoding as well. Because Java like deprecated.
				 */
			}
		}
	}

	private NetworkRequester() {
		// Since Java fails to recognize the StartCom root certificate, we need
		// to import it
		// System.setProperty("javax.net.ssl.trustStore","");
	}

	/**
	 * Reset the global password to default.
	 */
	public static void resetGlobalPassword() {
		globalPassword = "anonymous";
	}

	/**
	 * Makes a HTTP request to the server and returns a BufferedReader.
	 * 
	 * @param data
	 *            The data to be sent to the server.
	 * @return BufferedReader.
	 * @throws IOException
	 *             When something fails
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed
	 */
	private static BufferedReader makeRequest(String data) throws IOException,
			SSLHandshakeException {
		URL url = new URL("https://kth.stormhub.org/__Api_Java.php?API_VER=13&"
				+ data);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		return new BufferedReader(new InputStreamReader(conn.getInputStream()));
	}

	/**
	 * Check if the password supplied as input is accepted by the server. Also
	 * stores the password in the object for later use.
	 * 
	 * @param password
	 *            The password to check.
	 * @return True if the password is valid, false otherwise.
	 * @throws IOException
	 *             If the HTTP request fails in any way
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed
	 */
	public static boolean checkLogin(String password) throws IOException,
			SSLHandshakeException, UnsupportedEncodingException {
		BufferedReader reader = makeRequest("challenge&password=" + URLEncoder.encode(password, "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("granted")) {
				globalPassword = password;
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an array of all subscribers.
	 * 
	 * @return All subscribers from the server.
	 * @throws IllegalPasswordException
	 *             if password is invalid.
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed.
	 */
	public static ArrayList<Subscriber> getSubscribers()
			throws IllegalPasswordException, IOException, SSLHandshakeException {
		ArrayList<Subscriber> list = new ArrayList<Subscriber>(); // The
																	// ArrayList
																	// of our
																	// subscribers
		BufferedReader reader = makeRequest("getSubscribers&password="
				+ globalPassword); // Initialize a connection to the server
		String line; // Buffer each line
		while ((line = reader.readLine()) != null) {
			// At least one line contains a failure, indicating that
			// authentication has failed
			if (line.equals("failure")) {
				throw new IllegalPasswordException();
			}

			// Split every line with our magical delimiter
			String currentSubscriber[] = line.split(";");

			// This should never happen and would indicate a protocol error
			if (currentSubscriber.length != 8) {
				throw new IOException();
			}
			list.add(new Subscriber(currentSubscriber[0], // Name, always has a
															// value (may be an
															// empty string)
					Integer.parseInt(currentSubscriber[1]), // Database ID
					Integer.parseInt(currentSubscriber[2]), // Extension (phone
					// number)
					currentSubscriber[3], // IMSI
					currentSubscriber[4], // IMEI
					currentSubscriber[5], // TMSI
					Integer.parseInt(currentSubscriber[6]), // LAC (base station
					// ID)
					(currentSubscriber[7].equals("1") ? true : false) // Authorized
			));
		}

		return list;
	}

	/**
	 * Returns an array with statistics.
	 * 
	 * @return Statistics from the server.
	 * @throws IllegalPasswordException
	 *             if password is invalid.
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed
	 */
	public static ArrayList<GSMStatisticsObject> getStatistics()
			throws IllegalPasswordException, IOException, SSLHandshakeException {

		ArrayList<GSMStatisticsObject> stat = new ArrayList<GSMStatisticsObject>();
		BufferedReader reader = makeRequest("getStatistics&password="
				+ URLEncoder.encode(globalPassword, "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("failure")) {
				throw new IllegalPasswordException();
			}
			String currentStat[] = line.split(";");
			stat.add(new GSMStatisticsObject(currentStat[0], Integer
					.parseInt(currentStat[1])));
		}

		return stat;
	}

	/**
	 * Makes a request to the SMSC causing a text message to be sent from one
	 * subscriber to another. If ID is equal to zero, then the message is
	 * broadcasted.
	 * 
	 * @return True if the message was sent, false otherwise.
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws UnsupportedEncodingException
	 *             if the named encoding is not supported.
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed.
	 */
	public static boolean sendSMS(int toID, String message) throws IOException,
			UnsupportedEncodingException, SSLHandshakeException {
		if (message.length() > 160) {
			return false;
		}

		BufferedReader reader = makeRequest("password=" + URLEncoder.encode(globalPassword, "UTF-8")
				+ "&sendSMS=" + toID + // Send the message to this ID
				"&message=" + URLEncoder.encode(message, "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("done")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Makes a request to the HLR, causing a subscriber to be [de]authorized to
	 * access the network. The update happens live, causing the network to drop
	 * the subscriber if [s]he is online.
	 * 
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws IllegalPasswordException
	 *             if the password has changed.
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed
	 */
	public static void authorize(int ID, boolean isAuthorized)
			throws IOException, IllegalPasswordException, SSLHandshakeException {
		BufferedReader reader = makeRequest("password=" + URLEncoder.encode(globalPassword, "UTF-8")
				+ "&id=" + ID + "&authorize=" + (isAuthorized ? "yes" : "no"));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("failure")) {
				throw new IllegalPasswordException();
			}

			// Throw an IOException in case something went wrong on the server
			if (line.equals("error")) {
				throw new IOException();
			}
		}
	}

	/**
	 * Makes a request to the HLR which changes the extension (phone number) of
	 * a subscriber The new extension is applied at once.
	 * 
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws IllegalPasswordException
	 *             if the password has changed.
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed
	 */
	public static void setExtension(int ID, int newNumber) throws IOException,
			IllegalPasswordException, SSLHandshakeException {
		BufferedReader reader = makeRequest("password=" + URLEncoder.encode(globalPassword, "UTF-8")
				+ "&id=" + ID + "&number=" + newNumber);
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("failure")) {
				throw new IllegalPasswordException();
			}

			// Throw an IOException in case something went wrong on the server
			if (line.equals("error")) {
				throw new IOException();
			}
		}
	}

	/**
	 * Makes a request to the HLR which changes the database name of a
	 * subscriber. The new extension is applied at once. This parameter is sent
	 * out in the GSM 08.04 MM INFO and is taken in effect during the next
	 * periodical update made from the phone.
	 * 
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws IllegalPasswordException
	 *             if the password has changed.
	 * @throws SSLHandshakeException
	 *             if establishing a secure connection failed.
	 */
	public static void setName(int ID, String newName) throws IOException,
			IllegalPasswordException, SSLHandshakeException {
		BufferedReader reader = makeRequest("password=" + URLEncoder.encode(globalPassword, "UTF-8")
				+ "&id=" + ID + "&name=" + URLEncoder.encode(newName, "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {

			// Trigger the login window?
			if (line.equals("failure")) {
				throw new IllegalPasswordException();
			}

			// Throw an IOException in case something went wrong on the server
			if (line.equals("error")) {
				throw new IOException();
			}
		}
	}
}