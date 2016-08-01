package com.google.devrel.training.conference.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class Profile {
	String displayName;
	String mainEmail;
	TeeShirtSize teeShirtSize;

	private List<String> conferenceKeysToAttend;

	public List<String> getConferenceKeysToAttend() {
		return (this.conferenceKeysToAttend == null ? new ArrayList<String>()
				: ImmutableList.copyOf(this.conferenceKeysToAttend));
	}

	public void addToconferenceKeysToAttend(String conferenceKey) {
		if (this.conferenceKeysToAttend == null) {
			this.conferenceKeysToAttend = new ArrayList<String>();
		}
		conferenceKeysToAttend.add(conferenceKey);
	}

	public void removeToconferenceKeysToAttend(String conferenceKey) {
		if (this.conferenceKeysToAttend != null) {
			conferenceKeysToAttend.remove(conferenceKey);
		}
	}
	
	public void setConferenceKeysToAttend(List<String> conferenceKeysToAttend) {
		this.conferenceKeysToAttend = conferenceKeysToAttend;
	}

	@Id
	String userId;

	/**
	 * Public constructor for Profile.
	 * 
	 * @param userId
	 *            The user id, obtained from the email
	 * @param displayName
	 *            Any string user wants us to display him/her on this system.
	 * @param mainEmail
	 *            User's main e-mail address.
	 * @param teeShirtSize
	 *            The User's tee shirt size
	 * 
	 */
	public Profile(String userId, String displayName, String mainEmail,
			TeeShirtSize teeShirtSize) {
		this.userId = userId;
		this.displayName = displayName;
		this.mainEmail = mainEmail;
		this.teeShirtSize = teeShirtSize;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getMainEmail() {
		return mainEmail;
	}

	public TeeShirtSize getTeeShirtSize() {
		return teeShirtSize;
	}

	public String getUserId() {
		return userId;
	}

	/**
	 * Just making the default constructor private.
	 */
	private Profile() {
	}

	/**
	 * Update the Profile with the given displayName and teeShirtSize
	 *
	 * @param displayName
	 * @param teeShirtSize
	 */
	public void update(String displayName, TeeShirtSize teeShirtSize) {
		if (displayName != null) {
			this.displayName = displayName;
		}
		if (teeShirtSize != null) {
			this.teeShirtSize = teeShirtSize;
		}
	}

}
