package com.sx4.bot.commands.notifications;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.option.Option;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.sx4.bot.annotations.command.Examples;
import com.sx4.bot.annotations.command.Redirects;
import com.sx4.bot.category.ModuleCategory;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.core.Sx4CommandEvent;
import com.sx4.bot.entities.argument.ReminderArgument;
import com.sx4.bot.paged.PagedResult;
import com.sx4.bot.utility.ExceptionUtility;
import com.sx4.bot.utility.TimeUtility;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class ReminderCommand extends Sx4Command {
	
	public ReminderCommand() {
		super("reminder");
		
		super.setDescription("Create reminders to keep up to date with tasks");
		super.setExamples("reminder add", "reminder remove", "reminder list");
		super.setCategoryAll(ModuleCategory.NOTIFICATIONS);
	}
	
	public void onCommand(Sx4CommandEvent event) {
		event.replyHelp().queue();
	}
	
	@Command(value="add", description="Create a reminder so the bot will message you when the time is up", argumentInfo="reminder add <reminder>* in <time>*\nreminder add <reminder>* at <date time>*")
	@Examples({"reminder add Football game in 4 hours", "reminder add Party at 21/07/20 15:00 UTC+1", "reminder add Finish coursework at 12:00", "reminder add fish in 5 minutes --repeat", "reminder add weekly task at 23/05 --repeat=7d"})
	public void add(Sx4CommandEvent event, @Argument(value="reminder", endless=true) ReminderArgument reminder, @Option(value="repeat", description="Continuosly repeats the reminder after the initial duration is up") Duration repeat) {
		long initialDuration = reminder.getDuration();
		boolean repeatOption = event.isOptionPresent("repeat");

		long duration = repeat == null ? initialDuration : repeat.toSeconds();
		if (duration < 30 && repeatOption) {
			event.reply("Repeated reminders have to be at least 30 seconds long " + this.config.getFailureEmote()).queue();
			return;
		}
		
		if (reminder.getReminder().length() > 1500) {
			event.reply("Your reminder cannot be longer than 1500 characters " + this.config.getFailureEmote()).queue();
			return;
		}
		
		ObjectId id = ObjectId.get();
		
		Document reminderData = new Document("id", id)
			.append("duration", duration)
			.append("remindAt", Clock.systemUTC().instant().getEpochSecond() + initialDuration)
			.append("reminder", reminder.getReminder())
			.append("repeat", repeatOption);
		
		this.database.updateUserById(event.getAuthor().getIdLong(), Updates.push("reminder.reminders", reminderData)).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}
				
			this.reminderManager.putReminder(event.getAuthor().getIdLong(), initialDuration, reminderData);
				
			event.replyFormat("I will remind you about that in **%s**, your reminder id is `%s` %s", TimeUtility.getTimeString(initialDuration), id.toHexString(), this.config.getSuccessEmote()).queue();
		});
	}
	
	@Command(value="remove", description="Remove a reminder from being notified about")
	@Examples({"reminder remove 5ec67a3b414d8776950f0eee"})
	public void remove(Sx4CommandEvent event, @Argument(value="id") ObjectId id) {
		this.database.updateUserById(event.getAuthor().getIdLong(), Updates.pull("reminder.reminders", Filters.eq("id", id))).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}
			
			if (result.getModifiedCount() == 0) {
				event.reply("You do not have a reminder with that id " + this.config.getFailureEmote()).queue();
				return;
			}
			
			this.reminderManager.deleteExecutor(event.getAuthor().getIdLong(), id);
			
			event.reply("You will no longer be reminded about that reminder " + this.config.getSuccessEmote()).queue();
		});
	}
	
	@Command(value="list", description="Get a list of your current active reminders")
	@Examples({"reminder list"})
	@Redirects({"reminders"})
	public void list(Sx4CommandEvent event) {
		List<Document> reminders = this.database.getUserById(event.getAuthor().getIdLong(), Projections.include("reminder.reminders")).getEmbedded(List.of("reminder", "reminders"), Collections.emptyList());
		if (reminders.isEmpty()) {
			event.reply("You do not have any active reminders " + this.config.getFailureEmote()).queue();
			return;
		}
		
		long timeNow = Clock.systemUTC().instant().getEpochSecond();
		
		PagedResult<Document> paged = new PagedResult<>(reminders)
			.setIndexed(false)
			.setAuthor(event.getAuthor().getName() + "'s Reminders", null, event.getAuthor().getEffectiveAvatarUrl())
			.setDisplayFunction(data -> data.getObjectId("id").toHexString() + " - `" + TimeUtility.getTimeString(data.getLong("remindAt") - timeNow) + "`");
		
		paged.execute(event);
	}
	
	@Command(value="time zone", aliases={"zone"}, description="Set the default time zone to be used when specifiying a date when adding a reminder")
	@Examples({"reminder time zone UTC", "reminder time zone PST", "reminder time zone UTC+1"})
	public void timeZone(Sx4CommandEvent event, @Argument(value="time zone") TimeZone timeZone) {
		String zoneId = timeZone.getID();
		
		this.database.updateUserById(event.getAuthor().getIdLong(), Updates.set("reminder.timeZone", zoneId)).whenComplete((result, exception) -> {
			if (ExceptionUtility.sendExceptionally(event, exception)) {
				return;
			}
			
			if (result.getModifiedCount() == 0) {
				event.reply("Your default time zone was already set to that " + this.config.getFailureEmote()).queue();
				return;
			}
			
			event.replyFormat("Your default time zone has been set to `%s` " + this.config.getSuccessEmote(), zoneId).queue();
		});
	}

}