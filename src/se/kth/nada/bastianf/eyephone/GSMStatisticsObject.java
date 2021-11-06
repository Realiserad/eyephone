package se.kth.nada.bastianf.eyephone;

import android.os.Parcel;
import android.os.Parcelable;

public class GSMStatisticsObject implements Parcelable {
	public int count;
	public String description;
	
	public GSMStatisticsObject(String description, int count) {
		this.count = count;
		this.description = description;
	}
	
	private GSMStatisticsObject(Parcel in) {
		this.count = in.readInt();
		this.description = in.readString();
	}

	public static final Parcelable.Creator<GSMStatisticsObject> CREATOR = new Parcelable.Creator<GSMStatisticsObject>() {
		public GSMStatisticsObject createFromParcel(Parcel in) {
			return new GSMStatisticsObject(in);
		}

		public GSMStatisticsObject[] newArray(int size) {
			return new GSMStatisticsObject[size];
		}
	};

	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(count);
		out.writeString(description);
	}
}
