package de.nicidienase.chaosflix.entities.streaming;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 23.03.17.
 */

public class Room implements Parcelable {
	String slug;
	String shedulename;
	String thumb;
	String link;
	String display;
	List<Stream> streams;

	public Room() {}

	protected Room(Parcel in) {
		slug = in.readString();
		shedulename = in.readString();
		thumb = in.readString();
		link = in.readString();
		display = in.readString();
		streams = in.createTypedArrayList(Stream.CREATOR);
	}

	public static final Creator<Room> CREATOR = new Creator<Room>() {
		@Override
		public Room createFromParcel(Parcel in) {
			return new Room(in);
		}

		@Override
		public Room[] newArray(int size) {
			return new Room[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(slug);
		dest.writeString(shedulename);
		dest.writeString(thumb);
		dest.writeString(link);
		dest.writeString(display);
		dest.writeTypedList(streams);
	}


	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getShedulename() {
		return shedulename;
	}

	public void setShedulename(String shedulename) {
		this.shedulename = shedulename;
	}

	public String getThumb() {
		return thumb;
	}

	public void setThumb(String thumb) {
		this.thumb = thumb;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public List<Stream> getStreams() {
		return streams;
	}

	public void setStreams(List<Stream> streams) {
		this.streams = streams;
	}

	public static Room getDummyObject(){
		Room dummy = new Room();
		dummy.setSlug("dummy_room");
		dummy.setShedulename("Dummy Room");
		dummy.setThumb("https://static.media.ccc.de/media/unknown.png");
		dummy.setLink("");
		dummy.setDisplay("Dummy Room");
		dummy.setStreams(new ArrayList<>());
		dummy.getStreams().add(Stream.getDummyObject());
		return dummy;
	}
}
