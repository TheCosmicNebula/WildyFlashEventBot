package com.tcn.wilderness.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tcn.wilderness.helper.JavaHelper;
import com.tcn.wilderness.helper.JavaHelper.LVL;

public class Settings {
	
	public enum S {
		SERVER_ID(0, 0, "server_id", String.class),
		ADMIN_ROLE(1, 0, "admin_role_id", String.class),
		PING_ROLE(2, 0, "ping_role_id", String.class),
		BOT_MEMBER_ID(3, 0, "bot_member_id", String.class),
		
		COMMANDS_ID(4, 1, "commands_id", String.class),
		ANNOUNCE_ID(5, 1, "announce_id", String.class),
		
		PREFIX(6, 2, "command_prefix", String.class),
		
		ACTIVITY_TYPE(7, 3, "activity_type", int.class),
		ACTIVITY_DESC(8, 3, "activity_desc", String.class),
		
		TIMER_INTERVAL(9, 4, "timer_interval_mins", int.class),

		PRE_TICK(12, 5, "pretick", Boolean.class),
		SCHEDULED_EVENT(10, 5, "scheduled_event", String.class),
		
		PRINT_PERMS(13, 6, "print_perms", Boolean.class),
		
		BOT_OWNER(14, 7, "bot_owner", String.class);
				
		int index;
		int id;
		String ident;
		Class<?> objectType;
		
		S(int indexIn, int idIn, String identIn, Class<?> objectTypeIn) {
			index = indexIn;
			id = idIn;
			ident = identIn;
			objectType = objectTypeIn;
		}
		
		public int getIndex() {
			return index;
		}
		
		public int getId() {
			return id;
		}
		
		public String getIdent() {
			return ident;
		}
		
		public Class<?> getClazz() {
			return objectType;
		}
	}

	public static String SERVER_ID = "";
	public static String ADMIN_ROLE = "";
	public static String PING_ROLE = "";
	public static String BOT_MEMBER_ID = "";
	
	public static String ANNOUNCE_ID = "";
	public static String COMMANDS_ID = "";
	
	public static String PREFIX = "";
	
	public static int ACTIVITY_TYPE = 0;
	public static String ACTIVITY_DESC = "";
	
	public static int TIMER_INTERVAL = 5;
	
	public static String SCHEDULED_EVENT = "";

	public static boolean PRETICK = false;
	
	public static boolean PRINT_PERMS = false;
	
	public static String BOT_OWNER = "";
	
	public static JSONArray ARRAY = new JSONArray();
	public static JSONArray EVENTS = new JSONArray();
	
	public static void initiateSettings(DiscordApi apiIn, Server serverIn) {
		loadSettings();
		loadEvents();

		SERVER_ID = (String) get(S.SERVER_ID);
		ADMIN_ROLE = (String) get(S.ADMIN_ROLE);
		PING_ROLE = (String) get(S.PING_ROLE);
		BOT_MEMBER_ID = (String) get(S.BOT_MEMBER_ID);
		
		COMMANDS_ID = (String) get(S.COMMANDS_ID);
		ANNOUNCE_ID = (String) get(S.ANNOUNCE_ID);
		
		PREFIX = (String) get(S.PREFIX);
		
		ACTIVITY_TYPE = (int) get(S.ACTIVITY_TYPE);
		ACTIVITY_DESC = (String) get(S.ACTIVITY_DESC);
		
		TIMER_INTERVAL = (int) get(S.TIMER_INTERVAL);
		
		PRETICK = (Boolean) get(S.PRE_TICK);
		
		SCHEDULED_EVENT = (String) get(S.SCHEDULED_EVENT);
		
		PRINT_PERMS = (Boolean) get(S.PRINT_PERMS);
		
		BOT_OWNER = (String) get(S.BOT_OWNER);
		
		//set(S.PRE_TICK, false);
		setCurrentEvent(apiIn, serverIn, true);
	}
	
	public static void setCurrentEvent(DiscordApi apiIn, Server serverIn, boolean startup) {
		int currentHour = Integer.parseInt(JavaHelper.getTime(1, 0, false));
		int currentMinute = Integer.parseInt(JavaHelper.getTime(2, 0, false));
		
		boolean foundEvent = false;
		
		for (int i = 0; i < EVENTS.length(); i++) {
			JSONObject testEvent = EVENTS.getJSONObject(i);
			String nextEvent = testEvent.getString("next_event");
			String eventID = testEvent.getString("id");
			
			int eventHour = testEvent.getInt("time_hh");
			int eventMint = testEvent.getInt("time_mm");
			
			if (!foundEvent) {
				
				//Find next event based on current hour
				if (eventHour == currentHour) {
					if (currentMinute >= eventMint && currentMinute <= (eventMint + 5)) {
						set(S.SCHEDULED_EVENT, eventID);
						foundEvent = true;
					} else {
						set(S.SCHEDULED_EVENT, nextEvent);
						foundEvent = true;
					}
					
					JavaHelper.sendSystemMessage(LVL.INFO, "<currentEvent> Current event" + (startup ? " at startup" : "") + ": [ " + SCHEDULED_EVENT + " ]");
					WildernessBot.saveSystem();
					break;
				}
				
				//Find next event IF no events happen on startup hour
				else {					
					for (int j = 1; j <= 12; j++) {
						int forward = JavaHelper.addHourFromHour(false, currentHour, j);
												
						if (eventHour == forward) {
							set(S.SCHEDULED_EVENT, eventID);

							JavaHelper.sendSystemMessage(LVL.INFO, "<currentEvent> :SEARCHED: Current event" + (startup ? " at startup" : "") + ": [ " + SCHEDULED_EVENT + " ]");
							WildernessBot.saveSystem();
							foundEvent = true;
						} else {
							break;
						}
					}
				}
			}
		}
		
		if (apiIn != null && serverIn != null) {
			WildernessBot.startEventClockTask(apiIn, serverIn);
		}
		
		WildernessBot.saveSystem();
	}

	public static Object get(S setting) {
		return Settings.ARRAY.getJSONObject(setting.getId()).get(setting.getIdent());
	}
	
	public static void set(S setting, Object object) {
		if (object.getClass() == setting.getClazz()) {
			switch (setting.index) {
				case 0:
					SERVER_ID = (String) object;
					break;
				case 1:
					ADMIN_ROLE = (String) object;
					break;
				case 2:
					PING_ROLE = (String) object;
					break;
				
				case 3:
					COMMANDS_ID = (String) object;
					break;
				case 4:
					ANNOUNCE_ID = (String) object;
					break;
				case 5:
					BOT_MEMBER_ID = (String) object;
					break;
					
				case 6:
					PREFIX = (String) object;
					break;
				
				case 7:
					ACTIVITY_TYPE = (int) object;
					break;
				case 8:
					ACTIVITY_DESC = (String) object;
					break;
					
				case 9:
					TIMER_INTERVAL = (int) object;
					break;
					
				case 10:
					SCHEDULED_EVENT = (String) object;
					break;
				case 12:
					PRETICK = (Boolean) object;
					break;
				case 13:
					PRINT_PERMS = (Boolean) object;
					break;
				default:
					break;
			}
			
			ARRAY.getJSONObject(setting.getId()).put(setting.getIdent(), object);
			
			WildernessBot.saveSystem();
		}
	}
	
	public static void saveSettings() {
		File users = Reference.SETTINGS;
		JSONArray a = ARRAY;
		String jsonstr = a.toString(2);
		try {
			writeFile(jsonstr, users, StandardCharsets.UTF_8);
		} catch (IOException e) { }
	}

	public static void loadSettings() {
		File users = Reference.SETTINGS;
		String usersInfo = null;
		try {
			usersInfo = readFile(users, StandardCharsets.UTF_8);
		} catch (IOException e) { }
		
		ARRAY = new JSONArray();
		
		JSONArray a = new JSONArray(usersInfo);
		for (int i = 0; i < a.length(); i++) {
			JSONObject item = a.getJSONObject(i);

			ARRAY.put(item);
		}
	}

	public static void saveEvents() {
		File users = Reference.EVENTS;
		JSONArray a = EVENTS;
		String jsonstr = a.toString(2);
		try {
			writeFile(jsonstr, users, StandardCharsets.UTF_8);
		} catch (IOException e) { }
	}

	public static void loadEvents() {
		File users = Reference.EVENTS;
		String usersInfo = null;
		try {
			usersInfo = readFile(users, StandardCharsets.UTF_8);
		} catch (IOException e) { }
		
		EVENTS = new JSONArray();
		
		JSONArray a = new JSONArray(usersInfo);
		for (int i = 0; i < a.length(); i++) {
			JSONObject item = a.getJSONObject(i);

			EVENTS.put(item);
		}
	}

	public static String readFile(File path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(path.toPath());
		return new String(encoded, encoding);
	}

	public static void writeFile(String content, File path, Charset encoding) throws IOException {
		Files.write(path.toPath(), content.getBytes(encoding));
	}

	public static String loadToken() throws IOException {
		return readFile(Reference.token, StandardCharsets.UTF_8);
	}
}