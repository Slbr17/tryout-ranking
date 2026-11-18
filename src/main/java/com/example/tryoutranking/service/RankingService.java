package com.example.tryoutranking.service;

import com.example.tryoutranking.model.Player;
import javafx.collections.ObservableList;

import java.util.Objects;

public class RankingService {
    private final ObservableList<Player> players;

    public RankingService(ObservableList<Player> players) {
        this.players = Objects.requireNonNull(players);
    }

    public ObservableList<Player> getPlayers() {
        return players;
    }

    public void addPlayer(Player player) {
        players.add(Objects.requireNonNull(player));
    }

    public void deletePlayer(Player player) {
        players.remove(player);
    }

    public boolean moveUp(Player player) {
        int index = players.indexOf(player);
        if (index <= 0) return false;
        return moveToIndex(player, index - 1);
    }

    public boolean moveDown(Player player) {
        int index = players.indexOf(player);
        if (index < 0 || index >= players.size() - 1) return false;
        return moveToIndex(player, index + 1);
    }

    public boolean moveToTop(Player player) {
        return moveToIndex(player, 0);
    }

    public boolean moveToBottom(Player player) {
        return moveToIndex(player, players.size() - 1);
    }

    public boolean moveToRank(Player player, int rank) {
        if (rank < 1 || rank > players.size()) {
            throw new IllegalArgumentException("Rank must be between 1 and " + players.size());
        }
        return moveToIndex(player, rank - 1);
    }

    /**
     * Moves a player to the requested final zero-based index.
     */
    public boolean moveToIndex(Player player, int targetIndex) {
        Objects.requireNonNull(player);
        int currentIndex = players.indexOf(player);
        if (currentIndex < 0 || players.isEmpty()) return false;

        int bounded = Math.max(0, Math.min(targetIndex, players.size() - 1));
        if (currentIndex == bounded) return false;

        players.remove(currentIndex);
        players.add(bounded, player);
        return true;
    }
}
