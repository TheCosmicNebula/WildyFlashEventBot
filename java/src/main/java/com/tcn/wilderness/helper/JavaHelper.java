package com.tcn.wilderness.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.javacord.api.entity.message.Message;

public class JavaHelper {

	public static Logger LOGGER = null;

	public enum LVL {
		INFO("INFO", Level.INFO),
		DEBUG("DEBUG", Level.CONFIG),
		ERROR("ERROR", Level.WARNING),
		CRITICAL("CRITICAL", Level.SEVERE);
		
		String ident;
		Level level;
		
		LVL(String identIn, Level levelIn) {
			ident = identIn;
			level = levelIn;
		}
		
		public String getIdent() {
			return this.ident;
		}
		
		public Level getLevel() {
			return this.level;
		}
	}
	
	public static void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void setupLogger(String name) {
		LOGGER = Logger.getLogger(name);
		LOGGER.setUseParentHandlers(false);
		
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
		
		File latest = new File("log/latest.log");
		latest.delete();
		File debug = new File("log/debug.log");
		debug.delete();
	}
	
	/**
	 * Gets the current time.
	 * 
	 * @return The current time in the format: [YYYY-MM-DD | HH-MM-SS]
	 */
	public static String getTime(int state, int hoursToAdd, boolean twelve) {
		if (state == 0) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern((twelve ? "hh" : "HH") + ":mm:ss");
			LocalDateTime now = LocalDateTime.now().plusHours(hoursToAdd);
			
			return dtf.format(now).replace("/", "-").replace(" ", " | ");
		} else if (state == 1) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern((twelve ? "hh" : "HH") + ":mm:ss");
			LocalDateTime now = LocalDateTime.now().plusHours(hoursToAdd);

			return dtf.format(now).replace("/", "-").replace(" ", " | ").split(":")[0];
		} else if (state == 2) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern((twelve ? "hh" : "HH") + ":mm:ss");
			LocalDateTime now = LocalDateTime.now().plusHours(hoursToAdd);

			return dtf.format(now).replace("/", "-").replace(" ", " | ").split(":")[1];
		}

		return "";
	}
	
	public static int addHourFromHour(boolean twelve, int hourFrom, int hoursToAdd) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(twelve ? "hh" : "HH");
		LocalDateTime next = LocalDateTime.of(LocalDateTime.now().getYear(), LocalDateTime.now().getMonth(), LocalDateTime.now().getDayOfMonth(), hourFrom, LocalDateTime.now().getMinute()).plusHours(hoursToAdd);
		
		return Integer.parseInt(dtf.format(next).replace("/", "-").replace(" ", " | ").split(":")[0]);
	}

	public static String getDate() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("YYYY:MM:dd");
		LocalDateTime now = LocalDateTime.now();

		return dtf.format(now).replace("/", "-").replace(" ", " | ");
	}

	public static void sendSystemMessage(LVL levelIn, Object objectIn) {
		FileHandler[] handlers = new FileHandler[] { null, null };
		
		String output = "[" + getDate() + "] [" + getTime(0, 0, false) + "] [" + levelIn.getIdent() + "] {" + getSimpleCallerClassName() + "} " + objectIn;

		try {
			handlers[0] = new FileHandler("log/latest.log", true);
			LOGGER.addHandler(handlers[0]);
			SimpleFormatter formatter = new SimpleFormatter();
			handlers[0].setFormatter(formatter);
			handlers[0].setLevel(Level.INFO);
			
			handlers[1] = new FileHandler("log/debug.log", true);
			LOGGER.addHandler(handlers[1]);
			SimpleFormatter formatterA = new SimpleFormatter();
			handlers[1].setFormatter(formatterA);
			handlers[1].setLevel(Level.CONFIG);
			
			LOGGER.setLevel(Level.FINEST);
			LOGGER.log(levelIn.getLevel(), output);
			
			for (int i = 0; i < handlers.length; i++) {
				handlers[i].close();
			}
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(output);
	}

	/**
	 * Method to access the current class.
	 * @return String [full.class.name]
	 */
	public static String getCallerClassName() {
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		for (int i = 1; i < stElements.length; i++) {
			StackTraceElement ste = stElements[i];
			if (!ste.getClassName().equals(JavaHelper.class.getName()) && ste.getClassName().indexOf("java.lang.Thread") != 0) {
				return ste.getClassName().replace("_", "\\.");
			}
		}
		
		return null;
	}

	/**
	 * Method to return the simple class name of the current class.
	 * @return String [simpleclassname]
	 */
	public static String getSimpleCallerClassName() {
		String c = getCallerClassName();
		String[] split = c.split("\\.");
		int last = (split.length - 1);
		return split[last];
	}

	public static void getIPAddress(Message message, String webAddress) {
		URL whatismyip = null;
        BufferedReader in = null;
        try {
        	whatismyip = new URL(webAddress);
        	
            in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            
            String ip = in.readLine();
            
            message.reply("The current IP of the bot host is: `" + ip + "` - Found using host: [ " + webAddress + " ]");
        } catch (MalformedURLException e) {
        	message.reply("`ERROR: Malformed URL (" + e.getLocalizedMessage() + ")`");
			e.printStackTrace();
		} catch (IOException e) {
			message.reply("`ERROR: IOException (" + e.getLocalizedMessage() + ")` - Maybe this service is down?");
			e.printStackTrace();
		} finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
}