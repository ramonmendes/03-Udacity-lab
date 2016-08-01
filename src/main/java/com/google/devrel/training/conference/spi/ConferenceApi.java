package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.repackaged.com.google.common.base.Joiner;
import com.google.devrel.training.conference.Announcement;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.WrappedBoolean;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Session;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.form.SessionOfConferenceForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
		Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

	private static final String END_OF_LINE = "\n";

	/*
	 * Get the display name from the user's email. For example, if the email is
	 * lemoncake@example.com, then the display name becomes "lemoncake."
	 */
	private static String extractDefaultDisplayNameFromEmail(String email) {
		return email == null ? null : email.substring(0, email.indexOf("@"));
	}

	/**
	 * Creates a new Conference object and stores it to the datastore.
	 *
	 * @param user
	 *            A user who invokes this method, null when the user is not
	 *            signed in.
	 * @param conferenceForm
	 *            A ConferenceForm object representing user's inputs.
	 * @return A newly created Conference Object.
	 * @throws UnauthorizedException
	 *             when the user is not signed in.
	 */
	@SuppressWarnings("rawtypes")
	@ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
	public Conference createConference(final User user,
			final ConferenceForm conferenceForm) throws UnauthorizedException {
		validateUser(user);

		// Get the userId of the logged in User
		String userId = user.getUserId();

		// Get the key for the User's Profile
		Key profileKey = getKey(user);

		// Allocate a key for the conference -- let App Engine allocate the ID
		// Don't forget to include the parent Profile in the allocated ID
		final Key conferenceKey = ofy().factory().allocateId(profileKey,
				Conference.class);

		// Get the Conference Id from the Key
		final long conferenceId = conferenceKey.getId();

		// Get the existing Profile entity for the current user if there is one
		// Otherwise create a new Profile entity with default values
		Profile profile = getProfile(profileKey);

		if (profile != null) {
			ofy().delete().entity(profile);
		}

		profile = buildProfile(user, null);

		// Create a new Conference Entity, specifying the user's Profile entity
		// as the parent of the conference
		Conference conference = new Conference(conferenceId, userId,
				conferenceForm);

		// Save Conference and Profile Entities
		ofy().save().entities(profile, conference).now();
		
		
		//Add mail in queue
		Joiner joiner = Joiner.on(", ").skipNulls();
		StringBuilder builder = new StringBuilder();
		builder.append("Id: " + conferenceId + END_OF_LINE)
		.append("Name: " + conference.getName() + END_OF_LINE)
		.append("City: " + conference.getCity() + END_OF_LINE )
		.append("Topics: " + joiner.join(conference.getTopics()) + END_OF_LINE)
		.append("Start Date: " + conference.getStartDate() + END_OF_LINE)
		.append("End Date: " + conference.getEndDate() + END_OF_LINE)
		.append("Max Attendees: " + conference.getSeatsAvailable());

		Queue queue = QueueFactory.getDefaultQueue();
		
		queue.add(TaskOptions.Builder
				.withUrl("/cron/sendconfirmationemail")
				.param("email", user.getEmail())
				.param("conferenceInfo", builder.toString()));

		return conference;

	}

	/**
	 * Creates or updates a Profile object associated with the given user
	 * object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @param profileForm
	 *            A ProfileForm object sent from the client form.
	 * @return Profile object just created.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */

	// Declare this method as a method available externally through Endpoints
	@ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
	// The request that invokes this method should provide data that
	// conforms to the fields defined in ProfileForm
	public Profile saveProfile(final User user, ProfileForm profileForm)
			throws UnauthorizedException {

		// If the user is not logged in, throw an UnauthorizedException
		validateUser(user);

		Profile profile = buildProfile(user, profileForm);
		// Save the entity in the datastore
		ofy().save().entity(profile).now();
		// Return the profile
		return profile;
	}

	private Profile buildProfile(final User user, ProfileForm profileForm) {
		// Get the userId and mainEmail
		String mainEmail = user.getEmail();
		String userId = user.getUserId();
		// Get the displayName and teeShirtSize sent by the request.
		String displayName = null;
		TeeShirtSize teeShirtSize = null;

		if (profileForm != null) {
			displayName = profileForm.getDisplayName();
			teeShirtSize = profileForm.getTeeShirtSize();
		}

		// Get the Profile from the datastore if it exists
		// otherwise create a new one
		Profile profile = getProfile(Key.create(Profile.class, userId));

		if (profile == null) {
			// Populate the displayName and teeShirtSize with default values
			// if not sent in the request
			if (displayName == null) {
				displayName = extractDefaultDisplayNameFromEmail(user
						.getEmail());
			}
			if (teeShirtSize == null) {
				teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
			}
			// Now create a new Profile entity
			profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
		} else {
			// The Profile entity already exists
			// Update the Profile entity
			profile.update(displayName, teeShirtSize);
		}

		return profile;
	}

	/**
	 * Returns a Profile object associated with the given user object. The cloud
	 * endpoints system automatically inject the User object.
	 *
	 * @param user
	 *            A User object injected by the cloud endpoints.
	 * @return Profile object.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */
	@ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
	public Profile getProfile(final User user) throws UnauthorizedException {
		validateUser(user);

		// load the Profile Entity
		String userId = user.getUserId();
		Key<Profile> key = Key.create(Profile.class, userId);
		Profile profile = ofy().load().key(key).now();
		return profile;
	}

	/**
	 * Returns a Conference object with the given conferenceId.
	 *
	 * @param websafeConferenceKey
	 *            The String representation of the Conference Key.
	 * @return a Conference object with the given conferenceId.
	 * @throws NotFoundException
	 *             when there is no Conference with the given conferenceId.
	 */
	@ApiMethod(name = "getConference", path = "conference/{websafeConferenceKey}", httpMethod = HttpMethod.GET)
	public Conference getConference(
			@Named("websafeConferenceKey") final String websafeConferenceKey)
			throws NotFoundException {
		Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
		Conference conference = ofy().load().key(conferenceKey).now();
		if (conference == null) {
			throw new NotFoundException("No Conference found with key: "
					+ websafeConferenceKey);
		}
		return conference;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@ApiMethod(name = "queryConferences", path = "queryConferences", httpMethod = HttpMethod.POST)
	public List<Conference> queryConferences(ConferenceQueryForm query)
			throws UnauthorizedException {
		Iterable<Conference> conferenceIterable = query.getQuery();
		List<Conference> result = new ArrayList<>(0);
		List organizersKeyList = new ArrayList<>(0);

		for (Conference conference : conferenceIterable) {
			organizersKeyList.add(Key.create(Profile.class,
					conference.getOrganizerUserId()));
			result.add(conference);
		}

		ofy().load().keys(organizersKeyList);
		return result;
	}

	@SuppressWarnings({ "rawtypes" })
	@ApiMethod(name = "getConferencesCreated", path = "getConferencesCreated", httpMethod = HttpMethod.POST)
	public List<Conference> getConferencesCreated(User user)
			throws UnauthorizedException {
		validateUser(user);
		Key userKey = getKey(user);
		Query<Conference> order = ofy().load().type(Conference.class)
				.ancestor(userKey).order("name");

		return order.list();
	}

	private static String temp = null;

	/**
	 * Register to attend the specified Conference.
	 *
	 * @param user
	 *            An user who invokes this method, null when the user is not
	 *            signed in.
	 * @param websafeConferenceKey
	 *            The String representation of the Conference Key.
	 * @return Boolean true when success, otherwise false
	 * @throws UnauthorizedException
	 *             when the user is not signed in.
	 * @throws NotFoundException
	 *             when there is no Conference with the given conferenceId.
	 */
	@ApiMethod(name = "registerForConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.POST)
	public WrappedBoolean registerForConference(final User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey)
			throws UnauthorizedException, NotFoundException,
			ForbiddenException, ConflictException {
		// If not signed in, throw a 401 error.
		validateUser(user);

		temp = websafeConferenceKey;

		WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
			@Override
			public WrappedBoolean run() {
				try {
					// Get the conference key
					Key<Conference> conferenceKey = Key
							.create(ConferenceApi.temp);
					// Get the Conference entity from the datastore
					Conference conference = ofy().load().key(conferenceKey)
							.now();

					// 404 when there is no Conference with the given
					// conferenceId.
					if (conference == null) {
						return new WrappedBoolean(false,
								"No Conference found with key: "
										+ ConferenceApi.temp);
					}

					// Get the user's Profile entity
					Profile profile = getProfile(user);

					// Has the user already registered to attend this
					// conference?
					if (profile.getConferenceKeysToAttend().contains(
							ConferenceApi.temp)) {
						return new WrappedBoolean(false, "Already registered");
					} else if (conference.getSeatsAvailable() <= 0) {
						return new WrappedBoolean(false, "No seats available");
					} else {
						// All looks good, go ahead and book the seat
						profile.addToconferenceKeysToAttend(ConferenceApi.temp);
						conference.bookSeats(1);

						// Save the Conference and Profile entities
						ofy().save().entities(profile, conference).now();
						// We are booked!
						return new WrappedBoolean(true);
					}

				} catch (Exception e) {
					return new WrappedBoolean(false, "Unknown exception");

				}
			}
		});
		// if result is false
		if (!result.getResult()) {
			if (result.getReason() == "Already registered") {
				throw new ConflictException("You have already registered");
			} else if (result.getReason() == "No seats available") {
				throw new ConflictException("There are no seats available");
			} else {
				throw new ForbiddenException("Unknown exception");
			}
		}
		return result;
	}

	/**
	 * Returns a collection of Conference Object that the user is going to
	 * attend.
	 *
	 * @param user
	 *            An user who invokes this method, null when the user is not
	 *            signed in.
	 * @return a Collection of Conferences that the user is going to attend.
	 * @throws UnauthorizedException
	 *             when the User object is null.
	 */
	@ApiMethod(name = "getConferencesToAttend", path = "getConferencesToAttend", httpMethod = HttpMethod.GET)
	public Collection<Conference> getConferencesToAttend(final User user)
			throws UnauthorizedException, NotFoundException {
		Profile profile = verifyUserLogged(user);
		List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();
		List<Key<Conference>> keysToAttend = new ArrayList<>();
		for (String keyString : keyStringsToAttend) {
			keysToAttend.add(Key.<Conference> create(keyString));
		}
		return ofy().load().keys(keysToAttend).values();
	}

	private Profile verifyUserLogged(final User user)
			throws UnauthorizedException, NotFoundException {
		// If not signed in, throw a 401 error.
		validateUser(user);
		Profile profile = ofy().load()
				.key(Key.create(Profile.class, getUserId(user))).now();
		if (profile == null) {
			throw new NotFoundException("Profile doesn't exist.");
		}
		return profile;
	}

	/**
	 * Unregister from the specified Conference.
	 *
	 * @param user
	 *            An user who invokes this method, null when the user is not
	 *            signed in.
	 * @param websafeConferenceKey
	 *            The String representation of the Conference Key to unregister
	 *            from.
	 * @return Boolean true when success, otherwise false.
	 * @throws UnauthorizedException
	 *             when the user is not signed in.
	 * @throws NotFoundException
	 *             when there is no Conference with the given conferenceId.
	 */
	@ApiMethod(name = "unregisterFromConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.DELETE)
	public WrappedBoolean unregisterFromConference(final User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey)
			throws UnauthorizedException, NotFoundException,
			ForbiddenException, ConflictException {

		temp = websafeConferenceKey;

		WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
			@Override
			public WrappedBoolean run() {
				try {
					// Get the conference key
					Key<Conference> conferenceKey = Key
							.create(ConferenceApi.temp);
					// Get the Conference entity from the datastore
					Conference conference = ofy().load().key(conferenceKey)
							.now();

					// 404 when there is no Conference with the given
					// conferenceId.
					if (conference == null) {
						return new WrappedBoolean(false,
								"No Conference found with key: "
										+ ConferenceApi.temp);
					}

					// remove conference from list
					Profile profile = verifyUserLogged(user);
					profile.removeToconferenceKeysToAttend(websafeConferenceKey);

					// Update seat
					conference.bookSeatsAble(1);

					// Save the Conference and Profile entities
					ofy().save().entities(profile, conference).now();

				} catch (Exception e) {

				}
				return new WrappedBoolean(true);
			}
		});

		return result;

	}

	@ApiMethod(name = "getAnnouncement", path = "announcement", httpMethod = HttpMethod.GET)
	public Announcement getAnnouncement() {
		MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
		Object object = memcacheService.get(Constants.MEMCACHE_ANNOUNCEMENTS_KEY);
		if(object != null){
			return new Announcement(object.toString());
		}
		return null;
	}

	private String getUserId(User user) {
		return user.getUserId();
	}

	public List<Conference> findConferencePlayGround() {
		Query<Conference> order = ofy().load().type(Conference.class)
				.filter("city =", "Tokyo").filter("seatsAvailable >", 0)
				.filter("seatsAvailable <", 10).order("seatsAvailable")
				.order("name").order("month");
		return order.list();
	}

	@SuppressWarnings("rawtypes")
	private Profile getProfile(Key key) {
		@SuppressWarnings("unchecked")
		Profile profile = (Profile) ofy().load().key(key).now();
		return profile;
	}

	@SuppressWarnings("rawtypes")
	private Key getKey(final User user) {
		String userId = user.getUserId();
		Key key = Key.create(Profile.class, userId);
		return key;
	}
	
	@ApiMethod(name = "createSession", path = "createSession", httpMethod = HttpMethod.POST)
	public WrappedBoolean createSession(final User user, SessionOfConferenceForm form) throws ParseException, UnauthorizedException {
		validateUser(user);
		
		Key<Conference> conferenceKey = Key.create(form.getConference());
		
		SessionForm sessionForm = form.getSessionForm();
		Conference conference = ofy().load().key(conferenceKey).now();

		final Key<Session> sessionKey = ofy().factory().allocateId(conferenceKey,Session.class);

		Date dateSession = new SimpleDateFormat("dd/MM/yyyy").parse(sessionForm.getDate());
		Session session = new Session(sessionForm.getSessionName(), 
				sessionForm.getHighlights(),
				sessionForm.getSpeaker(), 
				sessionForm.getDuration(),
				sessionForm.getTypeOfSession(), 
				dateSession,
				sessionForm.getStartTime(), 
				conferenceKey,
				sessionKey.getId());
		
		
		//Save session
		ofy().save().entity(session);

		conference.addToSessionKeys(String.valueOf(sessionKey.getString()));
		
		ofy().save().entity(conference);

		
		return new WrappedBoolean(true);
	}
	
	@ApiMethod(name = "getConferenceSessions", path = "getConferenceSessions", httpMethod = HttpMethod.GET)
	public List<Session> getConferenceSessions(User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey) throws UnauthorizedException {
		validateUser(user);
		return ofy()
				.load()
				.type(Session.class)
				.ancestor(Key.create(websafeConferenceKey))
				.list();
	}
	@ApiMethod(name = "getConferenceSessionsByType", path = "getConferenceSessionsByType", httpMethod = HttpMethod.GET)
	public List<Session> getConferenceSessionsByType(User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey, @Named("typeOfSession") String typeOfSession) throws UnauthorizedException {
		validateUser(user);
		return ofy()
				.load()
				.type(Session.class)
				.ancestor(Key.create(websafeConferenceKey))
				.filter("typeOfSession =",typeOfSession)
				.list();
	}
	
	@ApiMethod(name = "getSessionsBySpeaker", path = "getSessionsBySpeaker", httpMethod = HttpMethod.GET)
	public List<Session> getSessionsBySpeaker(User user,
			@Named("websafeConferenceKey") final String websafeConferenceKey, @Named("speaker") String speaker) throws UnauthorizedException {
		validateUser(user);
		return ofy()
				.load()
				.type(Session.class)
				.ancestor(Key.create(websafeConferenceKey))
				.filter("speaker =",speaker)
				.list();
	}
	
	@ApiMethod(name = "getSessionsInWishlist", path = "getSessionsInWishlist", httpMethod = HttpMethod.GET)
	public Collection<Session> getSessionsInWishlist(User user) throws UnauthorizedException {
		validateUser(user);
		
		Profile profile = getProfile(user);
		
		List<Key<Session>> keysSession = new ArrayList<>();
		for (String keyString : profile.getSessionsKey()) {
			keysSession.add(Key.<Session> create(keyString));
		}
		
		return ofy().load().keys(keysSession).values();
	}
	
	@ApiMethod(name = "deleteSessionInWishlist", path = "deleteSessionInWishlist", httpMethod = HttpMethod.DELETE)
	public WrappedBoolean deleteSessionInWishlist(User user,@Named("sessionKey") final String sessionKey) throws UnauthorizedException {
		validateUser(user);
		
		Profile profile = getProfile(user);
		profile.removeSessionKeys(sessionKey);
		ofy().save().entities(profile).now();
		
		return new WrappedBoolean(true);
	}
	
	@ApiMethod(name = "addSessionToWishlist", path = "addSessionToWishlist", httpMethod = HttpMethod.POST)
	public WrappedBoolean addSessionToWishlist(User user,@Named("sessionKey") final String sessionKey) throws UnauthorizedException{
		validateUser(user);
		
		Profile profile = getProfile(user);
		profile.addToSessionKeys(sessionKey);
		ofy().save().entity(profile).now();
		
		return new WrappedBoolean(true);
	}

	private void validateUser(User user) throws UnauthorizedException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
	}

}
