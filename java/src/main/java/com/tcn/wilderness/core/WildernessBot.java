package com.tcn.wilderness.core;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.json.JSONObject;

import com.tcn.wilderness.core.Settings.S;
import com.tcn.wilderness.helper.DiscordHelper;
import com.tcn.wilderness.helper.JavaHelper;
import com.tcn.wilderness.helper.JavaHelper.LVL;

@SuppressWarnings("unused")
public class WildernessBot {
	
	static ArrayList<Timer> timers = new ArrayList<Timer>();
	
	static File ICON = new File("");
	
	public static void main(String[] args) throws IOException {
		JavaHelper.setupLogger("WildernessBot");

		ICON = new File("icon.png");
		
		Settings.initiateSettings(null, null);
		
		JavaHelper.sendSystemMessage(LVL.INFO, "Beginning System Load");
		JavaHelper.sendSystemMessage(LVL.INFO, "System version [" + Reference.VERSION + ", " + Reference.CHANNEL + "]");
		JavaHelper.sendSystemMessage(LVL.INFO, "Attempting Discord API Connection");
		
		DiscordApi api = new DiscordApiBuilder().setToken(Settings.loadToken()).setAllNonPrivilegedIntentsAnd(Intent.MESSAGE_CONTENT).login().join();
		JavaHelper.sendSystemMessage(LVL.INFO, "Discord API Connection Successful");
		
		JavaHelper.sendSystemMessage(LVL.DEBUG, "Begin load system settings");
		for (int i = 0; i < Settings.ARRAY.length(); i++) {
			JavaHelper.sendSystemMessage(LVL.DEBUG, "--> " + Settings.ARRAY.get(i));
		}
		JavaHelper.sendSystemMessage(LVL.INFO, "Finish load system settings");
		
		JavaHelper.sendSystemMessage(LVL.DEBUG, "Begin load resources");
		for (int i = 0; i < Settings.EVENTS.length(); i++) {
			JavaHelper.sendSystemMessage(LVL.DEBUG, "--> " + Settings.EVENTS.get(i));
		}
		JavaHelper.sendSystemMessage(LVL.INFO, "Finish load resources");
		
		ActivityType activityType = ActivityType.getActivityTypeById(Settings.ACTIVITY_TYPE);
		JavaHelper.sendSystemMessage(LVL.INFO, "Setting activity to: { " + activityType + " } [ " + Settings.ACTIVITY_DESC + " ]");
		api.updateActivity(activityType, Settings.ACTIVITY_DESC);
		
		ArrayList<Server> servers = new ArrayList<Server>();
		
		api.getServers().stream().forEach((server) -> {
			if (server.getIdAsString().equals(Settings.SERVER_ID)) {
				servers.add(server);
			}
		});

		if (servers.size() == 0) {
			JavaHelper.sendSystemMessage(LVL.CRITICAL, "ERROR! Defined server not present! Please add the bot to your server, and check the provided server ID!");
			shutdown(api);
		}
		
		Server specifiedServer = servers.get(0);
		JavaHelper.sendSystemMessage(LVL.INFO, "Verified Server: [ name: '" + specifiedServer.getName() + "', id: '" + specifiedServer.getIdAsString() + "' ]");
		
		if (Settings.PRINT_PERMS) {
			printPermissions(specifiedServer);
		}
		
		startEventClockTask(api, specifiedServer);
		
		api.addListener(new MessageCreateListener() {
			@Override
			public void onMessageCreate(MessageCreateEvent eventIn) {
				Message message = eventIn.getMessage();
				User messageUser = message.getUserAuthor().get();
				String messageContentRaw = message.getContent();
				String messageContent = messageContentRaw.toLowerCase();
				
				if (message.isPrivateMessage()) {
					if (!message.getUserAuthor().get().isBot()) {

						if (messageUser.getIdAsString().equals(Settings.BOT_OWNER)) {
							if (messageContent.contains("!ip")) {
								if (messageContent.contains(" ")) {
									String[] messageSplit = messageContent.split(" ");
									
									try {
										int site = Integer.parseInt(messageSplit[1]);
										
										switch (site) {
											case 0:
												JavaHelper.getIPAddress(message, "http://checkip.amazonaws.com/");
												break;
											case 1:
												JavaHelper.getIPAddress(message, "https://ipv4.icanhazip.com/");
												break;
											case 2:
												JavaHelper.getIPAddress(message, "http://myexternalip.com/raw");
												break;
											case 3:
												JavaHelper.getIPAddress(message, "http://ipecho.net/plain");
												break;
											case 4:
												JavaHelper.getIPAddress(message, "http://www.trackip.net/ip");
												break;
											default:
												JavaHelper.getIPAddress(message, "http://checkip.amazonaws.com/");
												break;
										}
									} catch (NumberFormatException e) {
										message.reply("`Check site must be a number.``");
									}
								} else {
									JavaHelper.getIPAddress(message, "http://checkip.amazonaws.com/");
								}
							}

							else if (message.getContent().equals(Settings.PREFIX + "shutdown")) {
								message.reply("`Shutting down`");
								
								JavaHelper.sleep(5);
								shutdown(api);
							}
						}

						else {
							message.reply("`Sorry, but this bot does not accept private messages.`");
						}
						
						JavaHelper.sendSystemMessage(LVL.INFO, "PM recieved from user " + DiscordHelper.getUserInfo(null, messageUser, "content: " + messageContent));
					}
					return;
				}
				
				Server currentServer = eventIn.getServer().get();
				Channel preChannel = eventIn.getChannel();

				ServerChannel serverChannel = preChannel.asServerChannel().get();
				
				if (!message.isPrivateMessage()) {
					ServerChannel currentChannel = serverChannel.asServerChannel().get();
					String channelName = serverChannel.getName();
					String channelID = serverChannel.getIdAsString();
					
					Role adminRole = currentServer.getRoleById(Settings.ADMIN_ROLE).get();
					
					String messageUserName = messageUser.getName();
					String messageUserNick = messageUser.getNickname(currentServer).isPresent() ? messageUser.getNickname(currentServer).get() : messageUserName;
					String messageUserId = messageUser.getIdAsString();
					
					if (!messageUser.isBot()) {
						if (messageContent.contains("]purge")) {
							if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
								ServerTextChannel textChannel = currentChannel.asServerTextChannel().get();
								
								String[] split = messageContent.split(" ");

								if (split.length > 1) {
									try {
										int number = Integer.parseInt(split[1]);
										
										try {
											textChannel.getMessages(number + 1).get().deleteAll();
											messageUser.sendMessage("`Messages deleted from channel: [" + channelName + "]`");
										} catch (InterruptedException | ExecutionException e) {
											messageUser.sendMessage("`Error when using command.`");
										}
										message.delete();
										
									} catch (NumberFormatException e) {
										messageUser.sendMessage("`Error when using command: [second arg must be a number]`");
									}
								} else {
									
									try {
										textChannel.getMessages(1200).get().deleteAll();
										messageUser.sendMessage("`Messages deleted from channel: [" + channelName + "]`");
									} catch (InterruptedException | ExecutionException e) {
										messageUser.sendMessage("`Error when using command.`");
									}
									message.delete();
								}
							}
						}
						
				/** TODO Normie Commands **/
						if (channelID.equals(Settings.COMMANDS_ID)) {
							if (messageContent.contains(Settings.PREFIX + "when")) {
								for (int i = 0; i < Settings.EVENTS.length(); i++) {
									JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
									String eventID = testEvent.getString("id");
									String eventName = testEvent.getString("name");
									
									int eventHour = testEvent.getInt("time_hh");
									int eventMint = testEvent.getInt("time_mm");
									
									if (messageContent.contains(testEvent.getString("id"))) {
										
										message.reply("Event: `" + eventName + "` is next ocurring at: `" + (eventHour < 10 ? "0" + eventHour : eventHour) + ":" + eventMint + "0`.");										
									}
								}
							}
							
							else if (messageContent.contains(Settings.PREFIX + "role")) {
								for (int i = 0; i < Settings.EVENTS.length(); i++) {
									JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
									
									if (messageContent.contains(testEvent.getString("id"))) {
										String role = testEvent.getString("role_id");
										
										Role pingRole = currentServer.getRoleById(role).get();
										
										if (messageUser.getRoles(currentServer).contains(pingRole)) {
											messageUser.removeRole(pingRole);
											
											message.reply("`Role removed @" + pingRole.getName() + "`");
										} else {
											messageUser.addRole(pingRole);
											message.reply("`Role added @" + pingRole.getName() + "`");
										}
									}
								}
							}
							
							else if (messageContent.equals(Settings.PREFIX + "roles")) {
								EmbedBuilder embed = new EmbedBuilder();
								
								embed.setAuthor(currentServer.getMemberById(Settings.BOT_MEMBER_ID).get());
								embed.setThumbnail(WildernessBot.ICON);
								embed.setColor(Color.ORANGE);
								embed.setTitle("Wilderness Flash Event Roles");
								embed.setDescription("All of the roles you are or are not signed up for");
								
								for (int i = 0; i < Settings.EVENTS.length(); i++) {
									JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
									String role = testEvent.getString("role_id");
									String eventName = testEvent.getString("name");
									
									if (messageUser.getRoles(currentServer).contains(currentServer.getRoleById(role).get())) {
										embed.addField(eventName + "", "> :white_check_mark: You are enrolled for notifications!");
									} else {
										embed.addField(eventName + "", "> :red_square: You are not enrolled for notifications.");
									}
									
								}
								
								message.reply(embed);
							}

							else if (messageContent.contains(Settings.PREFIX + "next")) {
								for (int i = 0; i < Settings.EVENTS.length(); i++) {
									JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
									String eventID = testEvent.getString("id");
									
									int eventHour = testEvent.getInt("time_hh");
									int eventMint = testEvent.getInt("time_mm");
									
									if (testEvent.getString("id").equals(Settings.SCHEDULED_EVENT)) {
										message.reply("The next event is: `" + testEvent.getString("name") + "` at: `" + (eventHour < 10 ? "0" + eventHour : eventHour) + ":" + eventMint + "0`.");
									}
								}
							}
							
							else if (messageContent.contains(Settings.PREFIX + "events")) {
								EmbedBuilder embed = new EmbedBuilder();
								
								embed.setAuthor(currentServer.getMemberById(Settings.BOT_MEMBER_ID).get());
								embed.setThumbnail(WildernessBot.ICON);
								embed.setColor(Color.CYAN);
								embed.setTitle("Event list :notepad_spiral:");
								embed.setDescription("A list of all Wilderness Flash Events");
								
								for (int i = 0; i < Settings.EVENTS.length(); i++) {
									JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
									String eventID = testEvent.getString("id");
									String eventName = testEvent.getString("name");
									String eventDesc = testEvent.getString("desc");
									
									embed.addField(i + " - " + eventName, "> `" + eventDesc + "`");
								}

								embed.setFooter(Reference.EMBED_FOOTER);
								embed.setTimestampToNow();
								
								message.reply(embed);
							}
							
							else if (messageContent.contains(Settings.PREFIX + "commands")) {
								String p = Settings.PREFIX;
								
								message.reply("__**Current Command List**__" + 
								    "\n```" + 
									"\n - " + p + "role [event_id] (Adds or removes the ping role for that event)" + 
									"\n - " + p + "events (Provides you with a list of all available Wilderness Flash Events)" + 
									"\n - " + p + "next (Shows you the next scheduled event, and the time it will occur)" +
									"\n - " + p + "when [event_id] (Shows you when that event will next occur)" +
									"```"
								);
							}
							
							
					/** TODO Administrator Commands **/
							else if (messageContent.contains(Settings.PREFIX + "announce")) {
								if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
									for (int i = 0; i < Settings.EVENTS.length(); i++) {
										JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
										
										if (messageContent.contains(testEvent.getString("id"))) {											
											TickTask.announceTick(currentServer, testEvent, true, messageContent.contains("mention"));
											
											message.reply("`Announcement sent for event: '" + testEvent.getString("name") + "'`");
										}
									}
								}
							}
							
							else if (messageContent.contains(Settings.PREFIX + "pre announce")) {
								if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
									for (int i = 0; i < Settings.EVENTS.length(); i++) {
										JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
										
										if (messageContent.contains(testEvent.getString("id"))) {
											TickTask.preAnnounceTick(currentServer, testEvent, true, messageContent.contains("mention"));
											
											message.reply("`Pre Announcement sent for event: '" + testEvent.getString("name") + "'`");
										}
									}
								}
							}

							else if (messageContent.contains(Settings.PREFIX + "tick")) {
								if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
									Settings.set(S.PRE_TICK, !Settings.PRETICK);
									
									System.out.println(Settings.get(S.PRE_TICK));
								}
							}
						
							else if (messageContent.contains(Settings.PREFIX + "sync")) {
								if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
									if (messageContent.contains(" ")) {
										String[] contentSplit = messageContent.split(" ");
										
										if (contentSplit.length >= 14) {
											ArrayList<Integer> eventTimes = new ArrayList<Integer>();
											
											for (int i = 0; i < 14; i++) {
												try {
													int testTime = Integer.parseInt(contentSplit[i + 1]);
													
													if (testTime < 0 || testTime > 24) {
														message.reply("`Time must be between 0 and 24`");
														return;
													}
													
													eventTimes.add(testTime);
												} catch (NumberFormatException e) {
													message.reply("`You must specify a number for each event`");
												}
											}
											
											if (eventTimes.size() == 14) {
												for (int j = 0; j < Settings.EVENTS.length(); j++) {
													JSONObject testEvent = Settings.EVENTS.getJSONObject(j);
													
													testEvent.put("time_hh", eventTimes.get(j));
												}
												
												JavaHelper.sendSystemMessage(LVL.DEBUG, "System synced by: " + DiscordHelper.getUserInfo(currentServer, messageUser, "") + " " + eventTimes);
												Settings.setCurrentEvent(api, currentServer, false);
												message.reply("`Events synced to above times. Please ensure these times are accurate.`");
												
												saveSystem();
											}
										} else {
											message.reply("`Not enough events specified!`");
										}
									} else {
										message.reply("`You must specify the time of the each of the 13 events to sync the Bot.`");
									}
								}
							}
							
							else if (message.getContent().equals(Settings.PREFIX + "shutdown")) {
								if (DiscordHelper.isMessageUserRole(message, adminRole, currentServer)) {
									message.reply("`Shutting down`");
									
									JavaHelper.sleep(5);
									shutdown(api);
								}
							}
							
							startEventClockTask(api, currentServer);
						}
					}
				} 
			}
		});
	}
	
	public static void printPermissions(Server specifiedServer) {
		JavaHelper.sendSystemMessage(LVL.DEBUG, "ALLOWED Permissions: ");
		Set<PermissionType> allowedPerms = specifiedServer.getPermissions(specifiedServer.getMemberById(Settings.BOT_MEMBER_ID).get()).getAllowedPermission();
		allowedPerms.forEach((perm) -> {
			JavaHelper.sendSystemMessage(LVL.DEBUG, " + [ " + perm + " ]");
		});
		JavaHelper.sendSystemMessage(LVL.DEBUG, "Complete");

		JavaHelper.sendSystemMessage(LVL.DEBUG, "UNSET Permissions: ");
		Set<PermissionType> unsetPerms = specifiedServer.getPermissions(specifiedServer.getMemberById(Settings.BOT_MEMBER_ID).get()).getUnsetPermissions();
		unsetPerms.forEach((perm) -> {
			JavaHelper.sendSystemMessage(LVL.DEBUG, " ~ [ " + perm + " ]");
		});
		JavaHelper.sendSystemMessage(LVL.DEBUG, "Complete");
		
		JavaHelper.sendSystemMessage(LVL.DEBUG, "DENIED Permissions: ");
		Set<PermissionType> deniedPerms = specifiedServer.getPermissions(specifiedServer.getMemberById(Settings.BOT_MEMBER_ID).get()).getDeniedPermissions();
		deniedPerms.forEach((perm) -> {
			JavaHelper.sendSystemMessage(LVL.DEBUG, " - [ " + perm + " ]");
		});
		JavaHelper.sendSystemMessage(LVL.DEBUG, "Complete");
		
	}
	
	public static void startEventClockTask(DiscordApi api, Server server) {
		if (timers.size() == 0) {
			startTimer(api, server);
		} else {
			timers.forEach((timer) -> {
				timer.purge();
				timer.cancel();
			});
			
			timers.clear();
			
			startTimer(api, server);
		}
	}
	
	private static void startTimer(DiscordApi api, Server server) {
		Timer timer = new Timer();
		
		TimerTask task = new TickTask(server, api);
		
		timer.scheduleAtFixedRate(task, new Date(), Duration.ofMinutes((int) Settings.get(S.TIMER_INTERVAL)).toMillis());
		
		timers.add(timer);
	}

	public static void saveSystem() {
		Settings.saveSettings();
		Settings.saveEvents();
	}

	public static void shutdown(DiscordApi api) {
		timers.clear();
		saveSystem();
		JavaHelper.sleep(5);
		api.disconnect();
		JavaHelper.sleep(10);
		System.exit(0);
	}
}