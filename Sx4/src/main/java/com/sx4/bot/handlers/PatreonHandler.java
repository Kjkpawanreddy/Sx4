package com.sx4.bot.handlers;

import com.mongodb.client.model.*;
import com.sx4.bot.database.Database;
import com.sx4.bot.events.patreon.PatreonPledgeCreateEvent;
import com.sx4.bot.events.patreon.PatreonPledgeDeleteEvent;
import com.sx4.bot.events.patreon.PatreonPledgeUpdateEvent;
import com.sx4.bot.hooks.PatreonListener;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PatreonHandler implements PatreonListener, EventListener {
	
	public static final PatreonHandler INSTANCE = new PatreonHandler();

	public void onPatreonPledgeCreate(PatreonPledgeCreateEvent event) {
		if (event.getAmount() == 0) {
			return;
		}
		
		Bson update = Updates.combine(
			Updates.set("amount", event.getAmount()),
			Updates.set("since", Clock.systemUTC().instant().getEpochSecond()),
			Updates.set("guilds", Collections.EMPTY_LIST)
		);
		
		if (event.hasDiscord()) {
			update = Updates.combine(update, Updates.set("discordId", event.getDiscordId()));
		}
		
		Database.get().updatePatronById(event.getId(), update).whenComplete(Database.exceptionally());
	}
	
	public void onPatreonPledgeUpdate(PatreonPledgeUpdateEvent event) {
		Database.get().updatePatronById(event.getId(), Updates.set("amount", event.getAmount())).whenComplete(Database.exceptionally());
	}
	
	public void onPatreonPledgeDelete(PatreonPledgeDeleteEvent event) {
		Database database = Database.get();
		
		database.findAndDeletePatronById(event.getId(), Projections.include("guilds")).thenCompose(data -> {
			List<Long> guilds = data.getList("guilds", Long.class, Collections.emptyList());
			
			List<WriteModel<Document>> bulkData = guilds.stream()
				.map(guildId -> new UpdateOneModel<Document>(Filters.eq("_id", guildId), Updates.unset("premium")))
				.collect(Collectors.toList());
			
			if (!bulkData.isEmpty()) {
    			return database.bulkWriteGuilds(bulkData);
			}

			return CompletableFuture.completedFuture(null);
		}).whenComplete(Database.exceptionally());
	}
	
	public void onEvent(GenericEvent event) {
		if (event instanceof GuildLeaveEvent) {
			long guildId = ((GuildLeaveEvent) event).getGuild().getIdLong();
			
			Database database = Database.get();
			
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE).projection(Projections.include("premium"));
			database.findAndUpdateGuildById(guildId, Updates.unset("premium"), options).thenCompose(data -> {
				if (data == null) {
					return CompletableFuture.completedFuture(null);
				}
				
				return database.updatePatronByFilter(Filters.eq("discordId", data.getLong("premium")), Updates.pull("guilds", guildId));
			}).whenComplete(Database.exceptionally());
		}
	}
	
}
