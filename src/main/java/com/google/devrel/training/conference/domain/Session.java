package com.google.devrel.training.conference.domain;

import java.util.Date;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.devrel.training.enumeration.TypeOfSession;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
@Cache
public class Session {
	
    @Id
    private Long id;
	
	private String sessionName;
	
	private String highlights;
	
	private String speaker;
	
	private Long duration;
	
	@Index
	private TypeOfSession typeOfSession;
	
	private Date date;
	
	private String startTime;
	
	@Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
	private Key<Conference> conference;

	public Session() {
		// TODO Auto-generated constructor stub
	}
	
	public Session(String sessionName, String highlights, String speaker,
			Long duration, TypeOfSession typeOfSession, Date date,
			String startTime, Key<Conference> conference, long id) {
		super();
		this.sessionName = sessionName;
		this.highlights = highlights;
		this.speaker = speaker;
		this.duration = duration;
		this.typeOfSession = typeOfSession;
		this.date = date;
		this.startTime = startTime;
		this.conference = conference;
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Key<Conference> getConference() {
		return conference;
	}

	public void setConference(Key<Conference> conference) {
		this.conference = conference;
	}

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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

}