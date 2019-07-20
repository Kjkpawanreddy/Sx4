package com.sx4.uno;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sx4.core.Sx4Bot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class Player {

	private List<UnoCard> cards = new ArrayList<>();
	private long userId;
	
	public Player(Member member) {
		this.userId = member.getIdLong();
	}
	
	public Player(Member member, List<UnoCard> cards) {
		this.userId = member.getIdLong();
		this.cards = cards;
	}
	
	public List<UnoCard> getCards() {
		return this.cards;
	}
	
	public Player addCards(UnoCard... cards) {
		this.cards.addAll(List.of(cards));
		
		return this;
	}
	
	public Player addCards(Collection<UnoCard> cards) {
		this.cards.addAll(cards);
		
		return this;
	}
	
	public Player removeCards(UnoCard... cards) {
		this.cards.removeAll(List.of(cards));
		
		return this;
	}
	
	public Player removeCards(Collection<UnoCard> cards) {
		this.cards.removeAll(cards);
		
		return this;
	}
	
	public List<UnoCard> getPlayableCards(UnoCard lastCard) {
		List<UnoCard> playableCards = new ArrayList<>();
		for (UnoCard card : this.cards) {
			if (card.isPlayable(lastCard)) {
				playableCards.add(card);
			}
		}
		
		return playableCards;
	}
	
	public boolean canPlay(UnoCard lastCard) {
		for (UnoCard card : this.cards) {
			if (card.isPlayable(lastCard)) {
				return true;
			}
		}
		
		return false;
	}
	
	public long getUserId() {
		return this.userId;
	}
	
	public User getUser() {
		return Sx4Bot.getShardManager().getUserById(this.userId);
	}
	
}