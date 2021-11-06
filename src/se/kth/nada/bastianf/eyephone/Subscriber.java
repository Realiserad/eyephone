package se.kth.nada.bastianf.eyephone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;

import javax.net.ssl.SSLHandshakeException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A subscriber in a GSM network. Since this class is parcelable, subscribers
 * can be passed between activities and saved to disk with ease.
 * 
 * @author Bastian Fredriksson and Peter Caprioli
 * @version 2.0
 */
public class Subscriber implements Parcelable {
	private String name;
	private int id;
	private int extension;
	private String imsi;
	private String imei;
	private String tmsi;
	private int station;
	private boolean isAuthorized;

	public Subscriber(String name, int id, int extension, String imsi,
			String imei, String tmsi, int station, boolean isAuthorized) {
		this.name = name;
		this.id = id;
		this.extension = extension;
		this.imsi = imsi;
		this.imei = imei;
		this.tmsi = tmsi;
		this.station = station;
		this.isAuthorized = isAuthorized;
	}

	private Subscriber(Parcel in) {
		this.name = in.readString();
		this.id = in.readInt();
		this.extension = in.readInt();
		this.imsi = in.readString();
		this.imei = in.readString();
		this.tmsi = in.readString();
		this.station = in.readInt();
		this.isAuthorized = in.readByte() == 1; // isAuthorized == true if byte == 1
	}
	
	public static Comparator<Subscriber> getNameComparator() {
		return new Comparator<Subscriber>() {

			@Override
			public int compare(Subscriber a, Subscriber b) {
				return a.getName().compareTo(b.getName());
			}
		};
	}
	
	public static Comparator<Subscriber> getExtensionComparator() {
		return new Comparator<Subscriber>() {
			
			@Override
			public int compare(Subscriber a, Subscriber b) {
				return a.getExtension() - b.getExtension();
			}
		};
	}
	
	public static Comparator<Subscriber> getIdComparator() {
		return new Comparator<Subscriber>() {

			@Override
			public int compare(Subscriber a, Subscriber b) {
				return a.getID() - b.getID();
			}
		};
	}

	/**
	 * Change the name of this subscriber.
	 * 
	 * @param name
	 *            The new name for this subscriber.
	 */
	public void changeName(String name) throws IOException,
			IllegalPasswordException {
		NetworkRequester.setName(this.id, name);
		this.name = name;
	}

	/**
	 * Change the extension (phone number) for this subscriber.
	 * 
	 * @param extension
	 *            The new extension (phone number) for this subscriber.
	 */
	public void changeExtension(int extension) throws IOException,
			IllegalPasswordException {
		NetworkRequester.setExtension(this.id, extension);
		this.extension = extension;
	}

	/**
	 * Get the name of this subscriber.
	 * 
	 * @return The name of this subscriber.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the extension (phone number) of this subscriber.
	 * 
	 * @return The extension (phone number) of this subscriber.
	 */
	public int getExtension() {
		return extension;
	}

	/**
	 * Get the IMSI of this subscriber.
	 * 
	 * @return The IMSI of this subscriber.
	 */
	public String getIMSI() {
		return imsi;
	}

	/**
	 * Get the IMEI of this subscriber.
	 * 
	 * @return The IMEI of this subscriber.
	 */
	public String getIMEI() {
		return imei;
	}

	/**
	 * Get the TMSI of this subscriber.
	 * 
	 * @return The TMSI of this subscriber.
	 */
	public String getTMSI() {
		return tmsi;
	}

	/**
	 * Get the ID of this subscriber.
	 * 
	 * @return The ID of this subscriber.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Get the station this subscriber is connected to.
	 * 
	 * @return The station of this subscriber.
	 */
	public int getStation() {
		return station;
	}

	/**
	 * Determine whether this subscriber is authorized on the network or not.
	 * 
	 * @return The access state for this subscriber.
	 */
	public boolean isAuthorized() {
		return isAuthorized;
	}

	/**
	 * Determine whether the subscriber is online on the network or not
	 * 
	 * @return True if the subscriber is on the network, otherwise false
	 */
	public boolean isOnline() {
		return (this.station == 0) ? false : true;
	}

	/**
	 * Authorizes the subscriber to access the network
	 * 
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws IllegalPasswordException
	 *             if the password has changed.
	 */
	public void authorize(boolean authorized) throws IOException,
			IllegalPasswordException, SSLHandshakeException {
		NetworkRequester.authorize(this.id, authorized);
		this.isAuthorized = authorized;
	}

	/**
	 * Sends a text message to the particular subscriber
	 * 
	 * @return true/false depending on if the text message was sent or not
	 * @throws IOException
	 *             if server failed to respond with a valid HTTP response.
	 * @throws UnsupportedEncodingException
	 *             if the named encoding is not supported.
	 */
	public boolean sendSMS(String message) throws UnsupportedEncodingException,
			IOException, SSLHandshakeException {
		return NetworkRequester.sendSMS(this.id, message);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<Subscriber> CREATOR = new Parcelable.Creator<Subscriber>() {
		public Subscriber createFromParcel(Parcel in) {
			return new Subscriber(in);
		}

		public Subscriber[] newArray(int size) {
			return new Subscriber[size];
		}
	};

	@Override
	public void writeToParcel(Parcel out, int flags) {
		/*
		 Save the values in the same order as they're read
		  
		 this.name = name;
		 this.id = id;
		 this.extension = extension;
		 this.imsi = imsi;
		 this.imei = imei;
		 this.tmsi = tmsi;
		 this.station = station;
		 this.isAuthorized = isAuthorized;
		 */
		out.writeString(name);
		out.writeInt(id);
		out.writeInt(extension);
		out.writeString(imsi);
		out.writeString(imei);
		out.writeString(tmsi);
		out.writeInt(station);
		out.writeByte((byte) (isAuthorized ? 1 : 0)); // if isAuthorized == true then byte == 1
	}
}
