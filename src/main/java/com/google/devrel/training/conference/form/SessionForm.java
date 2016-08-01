package com.google.devrel.training.conference.form;

import com.google.devrel.training.enumeration.TypeOfSession;

public class SessionForm {
	private String sessionName;

	private String highlights;

	private String speaker;

	private Long duration;

	private TypeOfSession typeOfSession;

	private String date;

	private String startTime;

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	public String getHighlights() {
		return highlights;
	}

	public void setHighlights(String highlights) {
		this.highlights = highlights;
	}

	public String getSpeaker() {
		return speaker;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public TypeOfSession getTypeOfSession() {
		return typeOfSession;
	}

	public void setTypeOfSession(TypeOfSession typeOfSession) {
		this.typeOfSession = typeOfSession;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
}
