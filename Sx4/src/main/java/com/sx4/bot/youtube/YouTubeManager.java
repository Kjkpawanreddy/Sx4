package com.sx4.bot.youtube;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.WriteModel;
import com.sx4.bot.core.Sx4Bot;
import com.sx4.bot.database.Database;
import com.sx4.bot.interfaces.Sx4Callback;
import com.sx4.bot.modules.ImageModule;
import com.sx4.bot.settings.Settings;
import com.sx4.bot.utils.TokenUtils;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class YouTubeManager {

	private final Map<String, ScheduledFuture<?>> resubscriptions;
	
	private final Set<YouTubeListener> listeners;
	
	public YouTubeManager() {
		this.listeners = new HashSet<>();
		this.resubscriptions = new HashMap<>();
	}
	
	public boolean hasResubscription(String channelId) {
		return this.resubscriptions.containsKey(channelId);
	}
	
	public YouTubeManager putResubscription(String channelId, ScheduledFuture<?> resubscription) {
		this.resubscriptions.put(channelId, resubscription);
		
		return this;
	}
	
	public YouTubeManager removeResubscription(String channelId) {
		this.resubscriptions.remove(channelId);
		
		return this;
	}
	
	public YouTubeManager addListener(YouTubeListener... listeners) {
		this.listeners.addAll(Arrays.asList(listeners));
		
		return this;
	}
	
	public YouTubeManager removeListener(YouTubeListener... listeners) {
		this.listeners.removeAll(Arrays.asList(listeners));
		
		return this;
	}
	
	public void onVideoUpload(YouTubeEvent event) {
		for (YouTubeListener listener : this.listeners) {
			listener.onVideoUpload(event);
		}
	}
	
	public void onVideoDelete(YouTubeEvent event) {
		for (YouTubeListener listener : this.listeners) {
			listener.onVideoDelete(event);
		}
	}
	
	public void onVideoTitleUpdate(YouTubeEvent event) {
		for (YouTubeListener listener : this.listeners) {
			listener.onVideoTitleUpdate(event);
		}
	}
	
	public void onVideoDescriptionUpdate(YouTubeEvent event) {
		for (YouTubeListener listener : this.listeners) {
			listener.onVideoDescriptionUpdate(event);
		}
	}
	
	public DeleteOneModel<Document> resubscribeAndGet(String channelId) {
		long amount = Database.get().getGuilds().countDocuments(Filters.elemMatch("youtubeNotifications", Filters.eq("uploaderId", channelId)));
		
		DeleteOneModel<Document> model = null;
		if (amount != 0) {
			RequestBody body = new MultipartBody.Builder()
					.addFormDataPart("hub.mode", "subscribe")
					.addFormDataPart("hub.topic", "https://www.youtube.com/xml/feeds/videos.xml?channel_id=" + channelId)
					.addFormDataPart("hub.callback", "http://" + Settings.DOMAIN + ":" + Settings.PORT + "/api/v1/youtube")
					.addFormDataPart("hub.verify", "sync")
					.addFormDataPart("hub.verify_token", TokenUtils.YOUTUBE)
					.setType(MultipartBody.FORM)
					.build();
			
			Request request = new Request.Builder()
					.url("https://pubsubhubbub.appspot.com/subscribe")
					.post(body)
					.build();
			
			ImageModule.client.newCall(request).enqueue((Sx4Callback) response -> {
				if (response.isSuccessful()) {
					System.out.println("Resubscribed to " + channelId + " for YouTube notifications");
				}
				
				response.close();
			});
		} else {
			model = new DeleteOneModel<>(Filters.eq("_id", channelId));
		}
		
		this.removeResubscription(channelId);
		
		return model;
	}
	
	public void resubscribe(String channelId) {
		DeleteOneModel<Document> model = this.resubscribeAndGet(channelId);
		if (model != null) {
			Database.get().deleteResubscription(model.getFilter(), (result, exception) -> {
				if (exception != null) {
					exception.printStackTrace();
				}
			});
		}
	}
	
	public void ensureResubscriptions() {
		List<Document> resubscriptions = Database.get().getResubscriptions().find().into(new ArrayList<>());
		
		List<WriteModel<Document>> bulkData = new ArrayList<>();
		for (Document data : resubscriptions) {
			String channelId = data.getString("_id");
			
			long timeTill = data.getLong("resubscribeAt") - Clock.systemUTC().instant().getEpochSecond();
			if (timeTill <= 0) { 
				DeleteOneModel<Document> model = this.resubscribeAndGet(channelId);
				if (model != null) {
					bulkData.add(model);
				}
			} else {
				ScheduledFuture<?> resubscription = Sx4Bot.scheduledExectuor.schedule(() -> this.resubscribe(channelId), timeTill, TimeUnit.SECONDS);
				this.putResubscription(channelId, resubscription);
			}
		}
		
		if (!bulkData.isEmpty()) {
			Database.get().bulkWriteResubscriptions(bulkData, (result, exception) -> {
				if (exception != null) {
					exception.printStackTrace();
				}
			});
		}
	}
	
}
