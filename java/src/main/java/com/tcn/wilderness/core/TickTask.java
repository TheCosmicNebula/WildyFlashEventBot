package com.tcn.wilderness.core;

import java.awt.Color;
import java.io.File;
import java.util.TimerTask;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.json.JSONObject;

import com.tcn.wilderness.core.Settings.S;
import com.tcn.wilderness.helper.JavaHelper;
import com.tcn.wilderness.helper.JavaHelper.LVL;

class TickTask extends TimerTask {
	
	private Server server;
	
	public TickTask(Server serverIn, DiscordApi apiIn) {		
		this.server = serverIn;
	}

	@Override
	public void run() {
		JavaHelper.sendSystemMessage(LVL.DEBUG, "tick success");
		
		int currentHour = Integer.parseInt(JavaHelper.getTime(1, 0, false));
		int currentMint = Integer.parseInt(JavaHelper.getTime(2, 0, false));
		
		if (currentMint > 30 - Settings.TIMER_INTERVAL && currentMint < 30 + Settings.TIMER_INTERVAL) {
			JavaHelper.sendSystemMessage(LVL.INFO, "Half hour tick notification");
		}
		
		for (int i = 0; i < Settings.EVENTS.length(); i++) {
			JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
			String nextEvent = testEvent.getString("next_event");
			String eventID = testEvent.getString("id");
			
			int eventHour = testEvent.getInt("time_hh");
			int eventMint = testEvent.getInt("time_mm");
			
			int currentHourLocalized = currentHour;
			int eventHourLocalized = (eventHour == 0 ? 24 : eventHour);
			
			//PRETICK
			if (!Settings.PRETICK) {
				if (currentHourLocalized == (eventHourLocalized - 1)) {
					if (currentMint >= 50) {
						Settings.set(S.SCHEDULED_EVENT, eventID);
						
						preAnnounceTick(this.server, testEvent, false, true);
						Settings.set(S.PRE_TICK, true);
						break;
					}
				}
			}
			
			//TICK
			if (currentHour == eventHour) {
				if (eventID.equals(Settings.SCHEDULED_EVENT)) {
					if (currentMint >= eventMint && currentMint <= (eventMint + 5)) {
						Settings.set(S.SCHEDULED_EVENT, nextEvent);
						
						announceTick(this.server, testEvent, false, true);
						
						testEvent.put("time_hh", Integer.parseInt(JavaHelper.getTime(1, 14, false)));
						
						WildernessBot.saveSystem();
						Settings.set(S.PRE_TICK, false);
						break;
					}
				}
			}
		}
	}
	
	public static void preAnnounceTick(Server serverIn, JSONObject eventIn, boolean dev, boolean mention) {
		ServerTextChannel announcements = serverIn.getChannelById(Settings.ANNOUNCE_ID).get().asServerTextChannel().get();
		
		String eventID = eventIn.getString("id");
		String eventName = eventIn.getString("name");
		String eventPreDesc = eventIn.getString("pre_desc");
		String roleID = eventIn.getString("role_id");
		boolean special = eventIn.getBoolean("special");
		int eventHour = eventIn.getInt("time_hh");
		//int eventMint = eventIn.getInt("time_mm");
		
		MessageBuilder message = new MessageBuilder();
		if (dev) {
			message.setContent((mention ? serverIn.getRoleById(roleID).get().getMentionTag() : serverIn.getRoleById(roleID).get().getName()) + " - **Event starting soon!** __***THIS IS A TEST***__");
		} else {
			message.setContent((mention ? serverIn.getRoleById(roleID).get().getMentionTag() : serverIn.getRoleById(roleID).get().getName()) + " - **Event starting soon!**");
		}
		
		Integer time = (eventHour > 12 ? eventHour - 12 : eventHour);
		String clockID = ":clock" + (time == 0 ? 12 : time) + ":";
		
		EmbedBuilder embed = new EmbedBuilder();
			embed.setAuthor(serverIn.getMemberById(Settings.BOT_MEMBER_ID).get());
			embed.setUrl("https://runescape.wiki/w/Wilderness_Flash_Events#" + eventName.replace(" ", "_"));
			embed.setThumbnail(WildernessBot.ICON);
			embed.setColor(dev ? Color.RED : Color.ORANGE);
			embed.setTitle("Wilderness Flash Events: " + eventName + " " + clockID);
			embed.setDescription("Heads up! The next wilderness flash event will start on the hour!");
			embed.addField("Event Info :information_source:", "> `" + eventPreDesc + "`");

			if (special) {
				embed.addField("Special Event :star2:", "> This event is a **special** event, granting a `Sack of very wild rewards`!");
			}
			
			embed.addField("Location :map:", "");
			System.out.println(eventID);
			embed.setImage(new File("locations/" + eventID + ".png"));
			embed.setFooter(Reference.EMBED_FOOTER);
			embed.setTimestampToNow();

		message.setEmbed(embed);
		message.send(announcements);
		
		JavaHelper.sendSystemMessage(LVL.INFO, "Pre Announcement sent for: [ " + eventName + " ]");
	}
	
	public static void announceTick(Server serverIn, JSONObject eventIn, boolean dev, boolean mention) {
		ServerTextChannel announcements = serverIn.getChannelById(Settings.ANNOUNCE_ID).get().asServerTextChannel().get();
		
		String eventName = eventIn.getString("name");
		String eventDesc = eventIn.getString("desc");
		String eventNextID = eventIn.getString("next_event");
		String roleID = eventIn.getString("role_id");
		boolean special = eventIn.getBoolean("special");
		
		JSONObject nextEvent = null;

		for (int i = 0; i < Settings.EVENTS.length(); i++) {
			JSONObject testEvent = Settings.EVENTS.getJSONObject(i);
			
			if (testEvent.getString("id").equals(eventNextID)) {
				nextEvent = testEvent;
				break;
			}
		}
		
		String nextEventName = nextEvent.getString("name");
		int nextEventHour = nextEvent.getInt("time_hh");
		int nextEventMint = nextEvent.getInt("time_mm");
		
		MessageBuilder message = new MessageBuilder();
		if (dev) {
			message.setContent((mention ? serverIn.getRoleById(roleID).get().getMentionTag() : serverIn.getRoleById(roleID).get().getName()) + " - **Event has started!** __***THIS IS A TEST***__");
		} else {
			message.setContent((mention ? serverIn.getRoleById(roleID).get().getMentionTag() : serverIn.getRoleById(roleID).get().getName()) + " - **Event has started!**");
		}
		
		EmbedBuilder embed = new EmbedBuilder();
			embed.setAuthor(serverIn.getMemberById(Settings.BOT_MEMBER_ID).get());
			embed.setUrl("https://runescape.wiki/w/Wilderness_Flash_Events#" + eventName.replace(" ", "_"));
			embed.setThumbnail(WildernessBot.ICON);
			embed.setColor(dev ? Color.CYAN : Color.GREEN);
			embed.setTitle("Wilderness Flash Events: " + eventName + " :crossed_swords:");
			embed.setDescription("Heads up! The next wilderness flash event has started!");
			
			if (special) {
				embed.addField("Special Event :star2:", "> This event is a **special** event, granting a `Sack of very wild rewards`!");
			}
			
			embed.addField("Event Info :information_source:", "> `" + eventDesc + "`");
			embed.addField("Up Next :track_next:", "> The next event is: `" + nextEventName + "`, at: `" + (nextEventHour < 10 ? "0" + nextEventHour : nextEventHour) + ":" + nextEventMint + "0`");
			embed.setFooter(Reference.EMBED_FOOTER);
			embed.setTimestampToNow();
		
		message.setEmbed(embed);
		message.send(announcements);
		
		JavaHelper.sendSystemMessage(LVL.INFO, "Announcement sent for: [ " + eventName + " ]");
	}
}