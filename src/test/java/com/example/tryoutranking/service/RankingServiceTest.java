package com.example.tryoutranking.service;

import com.example.tryoutranking.model.Player;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingServiceTest {
    private ObservableList<Player> players;
    private RankingService service;

    @BeforeEach
    void setUp() {
        players = FXCollections.observableArrayList();
        for (int i = 1; i <= 10; i++) players.add(player("P" + i));
        service = new RankingService(players);
    }

    @Test
    void move10To1() {
        service.moveToRank(players.get(9), 1);
        assertOrder("P10", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9");
    }

    @Test
    void move1To10() {
        service.moveToRank(players.getFirst(), 10);
        assertOrder("P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P1");
    }

    @Test
    void move5To6() {
        Player p5 = players.get(4);
        assertTrue(service.moveDown(p5));
        assertOrder("P1", "P2", "P3", "P4", "P6", "P5", "P7", "P8", "P9", "P10");
    }

    @Test
    void move6To5() {
        Player p6 = players.get(5);
        assertTrue(service.moveUp(p6));
        assertOrder("P1", "P2", "P3", "P4", "P6", "P5", "P7", "P8", "P9", "P10");
    }

    @Test
    void firstPlayerCannotMoveUp() {
        assertFalse(service.moveUp(players.getFirst()));
        assertOrder("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10");
    }

    @Test
    void lastPlayerCannotMoveDown() {
        assertFalse(service.moveDown(players.getLast()));
        assertOrder("P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10");
    }

    @Test
    void movePlayerToTop() {
        service.moveToTop(players.get(6));
        assertEquals("P7", players.getFirst().getFirstName());
    }

    @Test
    void movePlayerToBottom() {
        service.moveToBottom(players.get(2));
        assertEquals("P3", players.getLast().getFirstName());
    }

    @Test
    void moveDirectlyToRank() {
        Player p4 = players.get(3);
        service.moveToRank(p4, 8);
        assertOrder("P1", "P2", "P3", "P5", "P6", "P7", "P8", "P4", "P9", "P10");
    }

    @Test
    void deletePlayerFromMiddle() {
        service.deletePlayer(players.get(4));
        assertOrder("P1", "P2", "P3", "P4", "P6", "P7", "P8", "P9", "P10");
    }

    @Test
    void addNewPlayerAtBottom() {
        service.addPlayer(player("P11"));
        assertEquals("P11", players.getLast().getFirstName());
        assertEquals(11, players.size());
    }

    @Test
    void dragDropEquivalentMovementDownward() {
        Player p3 = players.get(2);
        service.moveToIndex(p3, 7);
        assertOrder("P1", "P2", "P4", "P5", "P6", "P7", "P8", "P3", "P9", "P10");
    }

    @Test
    void dragDropEquivalentMovementUpward() {
        Player p8 = players.get(7);
        service.moveToIndex(p8, 1);
        assertOrder("P1", "P8", "P2", "P3", "P4", "P5", "P6", "P7", "P9", "P10");
    }

    @Test
    void rejectsOutOfRangeRank() {
        assertThrows(IllegalArgumentException.class, () -> service.moveToRank(players.getFirst(), 0));
        assertThrows(IllegalArgumentException.class, () -> service.moveToRank(players.getFirst(), 11));
    }

    private Player player(String name) {
        return new Player(name, "Test");
    }

    private void assertOrder(String... names) {
        assertEquals(List.of(names), players.stream().map(Player::getFirstName).toList());
    }
}
