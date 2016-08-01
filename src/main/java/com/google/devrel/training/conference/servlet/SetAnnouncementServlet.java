package com.google.devrel.training.conference.servlet;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.common.base.Joiner;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;

public class SetAnnouncementServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6125616790988037993L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		Iterable<Conference> filter = ofy().load().type(Conference.class)
		.filter("seatsAvailable <", 5)
		.filter("seatsAvailable >",0);
		
		List<String> conferenceName = new ArrayList<String>();
		
		for (Conference conference : filter) {
			conferenceName.add(conference.getName());
		}
		
		StringBuilder announcement = new StringBuilder("Oh Look! Last chance to attend!, The following conferences are nearly sould out:");
		Joiner joiner = Joiner.on(", ").skipNulls();
		announcement.append(joiner.join(conferenceName));
		
		MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
		
		memcacheService.put(Constants.MEMCACHE_ANNOUNCEMENTS_KEY, announcement.toString());
		
		resp.setStatus(204);
		
	}
}
