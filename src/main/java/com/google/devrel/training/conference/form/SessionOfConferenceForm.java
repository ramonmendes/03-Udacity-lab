package com.google.devrel.training.conference.form;


public class SessionOfConferenceForm {
	private SessionForm sessionForm;
	
	private String conference;
	
	public SessionForm getSessionForm() {
		return sessionForm;
	}
	public void setSessionForm(SessionForm sessionForm) {
		this.sessionForm = sessionForm;
	}
	public String getConference() {
		return conference;
	}
	public void setConference(String conference) {
		this.conference = conference;
	}

}
