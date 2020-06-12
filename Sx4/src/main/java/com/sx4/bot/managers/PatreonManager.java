package com.sx4.bot.managers;

import java.util.ArrayList;
import java.util.List;

import com.sx4.bot.events.patreon.PatreonEvent;
import com.sx4.bot.events.patreon.PatreonMemberEvent;
import com.sx4.bot.events.patreon.PatreonMemberUpdateEvent;
import com.sx4.bot.events.patreon.PatreonPledgeCreateEvent;
import com.sx4.bot.events.patreon.PatreonPledgeDeleteEvent;
import com.sx4.bot.events.patreon.PatreonPledgeEvent;
import com.sx4.bot.events.patreon.PatreonPledgeUpdateEvent;
import com.sx4.bot.hooks.PatreonListener;

public class PatreonManager {
	
	private static final PatreonManager INSTANCE = new PatreonManager();
	
	public static PatreonManager get() {
		return PatreonManager.INSTANCE;
	}

	private final List<PatreonListener> listeners;
	
	private PatreonManager() {
		this.listeners = new ArrayList<>();
	}
	
	public PatreonManager addListener(PatreonListener listener) {
		this.listeners.add(listener);
		
		return this;
	}
	
	public PatreonManager removeListener(PatreonListener listener) {
		this.listeners.remove(listener);
		
		return this;
	}
	
	public void onPatreonPledge(PatreonEvent event) {
		for (PatreonListener listener : this.listeners) {
			listener.onPatreon(event);
			
			if (event instanceof PatreonPledgeEvent) {
				listener.onPatreonPledge((PatreonPledgeEvent) event);
			
				if (event instanceof PatreonPledgeCreateEvent) {
					listener.onPatreonPledgeCreate((PatreonPledgeCreateEvent) event);
				} else if (event instanceof PatreonPledgeUpdateEvent) {
					listener.onPatreonPledgeUpdate((PatreonPledgeUpdateEvent) event);
				} else if (event instanceof PatreonPledgeDeleteEvent) {
					listener.onPatreonPledgeDelete((PatreonPledgeDeleteEvent) event);
				}
			} else if (event instanceof PatreonMemberEvent) {
				listener.onPatreonMember((PatreonMemberEvent) event);
				
				if (event instanceof PatreonMemberUpdateEvent) {
					listener.onPatreonMemberUpdate((PatreonMemberUpdateEvent) event);
				}
			}
		}
	}
	
}
