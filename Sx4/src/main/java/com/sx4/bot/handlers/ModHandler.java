package com.sx4.bot.handlers;

import org.bson.Document;

import com.mongodb.client.model.Projections;
import com.sx4.bot.database.Database;
import com.sx4.bot.entities.mod.action.Action;
import com.sx4.bot.entities.mod.action.ModAction;
import com.sx4.bot.entities.mod.modlog.ModLog;
import com.sx4.bot.events.mod.BanEvent;
import com.sx4.bot.events.mod.KickEvent;
import com.sx4.bot.events.mod.ModActionEvent;
import com.sx4.bot.events.mod.MuteEvent;
import com.sx4.bot.events.mod.MuteExtendEvent;
import com.sx4.bot.events.mod.TemporaryBanEvent;
import com.sx4.bot.events.mod.UnbanEvent;
import com.sx4.bot.events.mod.UnmuteEvent;
import com.sx4.bot.events.mod.WarnEvent;
import com.sx4.bot.hooks.ModActionListener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class ModHandler implements ModActionListener, EventListener {
	
	public static final ModHandler INSTANCE = new ModHandler();
	
	public void onAction(ModActionEvent event) {
		Database database = Database.get();
		
		Guild guild = event.getGuild();
		
		Action action = event.getAction();
		
		ModAction modAction = action.getModAction();
		if (modAction.isOffence()) {
			// TODO: insert offence
		}
		
		Document data = database.getGuildById(guild.getIdLong(), Projections.include("modLog.channelId", "modLog.enabled")).get("modLog", Database.EMPTY_DOCUMENT);
		
		long channelId = data.getLong("channelId");
		if (!data.getBoolean("enabled", false) || channelId == 0) {
			return;
		}
		
		TextChannel channel = guild.getTextChannelById(channelId);
		if (channel == null) {
			return;
		}
		
		ModLog modLog = new ModLog(
			channel.getIdLong(),
			guild.getIdLong(),
			event.getTarget().getIdLong(),
			event.getModerator().getIdLong(),
			event.getReason(),
			event.getAction()
		);
		
		channel.sendMessage(modLog.getEmbed(event.getModerator().getUser(), event.getTarget())).queue(message -> {
			modLog.setMessageId(message.getIdLong());
			
			database.insertModLog(modLog.toData()).whenComplete(Database.exceptionally());
		});
	}

	public void onBan(BanEvent event) {
		
	}
	
	public void onTemporaryBan(TemporaryBanEvent event) {
		
	}
	
	public void onKick(KickEvent event) {
		
	}
	
	public void onMute(MuteEvent event) {
		
	}
	
	public void onMuteExtend(MuteExtendEvent event) {
		
	}
	
	public void onWarn(WarnEvent event) {
		
	}
	
	public void onUnban(UnbanEvent event) {
		
	}
	
	public void onUnmute(UnmuteEvent event) {
		
	}

	public void onEvent(GenericEvent genericEvent) {
		
	}
	
}
