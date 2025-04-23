package com.tcn.wilderness.helper;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class DiscordHelper {

	public static boolean userEquals(Message message, User user) {
		if (message.getAuthor().asUser().get().equals(user)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean userNameEquals(Message message, String userName) {
		if (message.getAuthor().getName().equals(userName)) {
			return true;
		} else {
			return false;
		}
	}

	public static void sendUserPM(User msgU, String msg) {
		msgU.sendMessage(msg);
	}

	public static boolean messageContentEquals(Message message, String equals) {
		return message.getContent().equals(equals);
	}

	public static boolean messageUserEquals(Message message, User user) {
		if (message.getUserAuthor().isPresent()) {
			return message.getUserAuthor().get().equals(user);
		}
		
		return false;
	}

	public static boolean messageUserNameEquals(Message message, String name) {
		return message.getAuthor().getName().equals(name);
	}

	public static boolean isMessageUserRole(Message message, Role role, Server serv) {
		boolean hasRole = false;
		
		if (role != null) {
			if (message.getUserAuthor().isPresent()) {
				User msgAuth = message.getUserAuthor().get();
				hasRole = msgAuth.getRoles(serv).contains(role);
			}
		}
		
		if (!hasRole) { 
			message.reply("`You do not have permission to access that command.`");
		}
		
		return hasRole;
	}

	public static boolean isMessageUserRole(Message message, Role role1, Role role2, Server serv) {
		if (message.getUserAuthor().isPresent()) {
			User msgAuth = message.getUserAuthor().get();
			
			if (msgAuth.getRoles(serv).contains(role1) || msgAuth.getRoles(serv).contains(role2)) {
				return true;
			} else {
				return false;
			}
		}
		
		return false;
	}

	public static boolean isMessageUserRole(Message message, Role role1, Role role2, Role role3, Server serv) {
		if (message.getUserAuthor().isPresent()) {
			User msgAuth = message.getUserAuthor().get();
			
			if (msgAuth.getRoles(serv).contains(role1) || msgAuth.getRoles(serv).contains(role2) || msgAuth.getRoles(serv).contains(role3)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	public static void messageUserNoPerm(Message message, User messageUser) {
		messageUser.sendMessage("You don't have access to that command.");
		message.delete();
	}

	public static String getUserInfo(Server serverIn, User userIn, String extraInfo) {
		if (userIn != null) {
			return "[ name: '" + userIn.getName() + (serverIn != null ? "', nick: '" + userIn.getNickname(serverIn) : "") + "', id: '" + userIn.getIdAsString() + (extraInfo.isEmpty() ? "'" : "', '" + extraInfo + "'") + " ]";
		}
		return "";
	}
	
}