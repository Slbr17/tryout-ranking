package com.example.tryoutranking.service;

import com.example.tryoutranking.model.Player;
import com.example.tryoutranking.model.PlayerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelServiceTest {
    @TempDir Path tempDir;
    private final ExcelService service = new ExcelService();

    @Test
    void savesAndReloadsPlayersWithOrderAndContent() throws Exception {
        List<Player> input = new ArrayList<>();
        Player a = player("John", "Smith", "07123456789", "GK", "Strong hands", PlayerStatus.SELECTED);
        a.setEmail("john@example.com");
        a.setDateOfBirth(LocalDate.of(2007, 3, 14));
        Player b = player("David", "Jones", "07456000123", "CB", "Good communicator\nWatch recovery pace", PlayerStatus.MAYBE);
        input.add(a);
        input.add(b);

        Path file = tempDir.resolve("players.xlsx");
        service.save(file, input);
        List<Player> output = service.load(file);

        assertEquals(2, output.size());
        assertEquals("John", output.get(0).getFirstName());
        assertEquals("Smith", output.get(0).getLastName());
        assertEquals("07123456789", output.get(0).getPhone());
        assertEquals("john@example.com", output.get(0).getEmail());
        assertEquals("GK", output.get(0).getPosition());
        assertEquals(LocalDate.of(2007, 3, 14), output.get(0).getDateOfBirth());
        assertEquals(PlayerStatus.SELECTED, output.get(0).getStatus());
        assertEquals("Good communicator\nWatch recovery pace", output.get(1).getNotes());
    }

    @Test
    void blankOptionalFieldsSurvive() throws Exception {
        Player p = new Player("Alex", "Blank");
        Path file = tempDir.resolve("blank.xlsx");
        service.save(file, List.of(p));

        Player loaded = service.load(file).getFirst();
        assertEquals("", loaded.getPhone());
        assertEquals("", loaded.getEmail());
        assertEquals("", loaded.getPosition());
        assertEquals("", loaded.getNotes());
        assertNull(loaded.getDateOfBirth());
        assertEquals(PlayerStatus.UNREVIEWED, loaded.getStatus());
    }

    @Test
    void unicodeAndSpecialCharactersSurvive() throws Exception {
        Player p = player("Zoë", "O’Connor", "020 7946 0000", "MF", "Excellent — calm under pressure ⚽", PlayerStatus.RESERVE);
        p.setEmail("zoe.o'connor@example.co.uk");
        Path file = tempDir.resolve("unicode.xlsx");
        service.save(file, List.of(p));

        Player loaded = service.load(file).getFirst();
        assertEquals("Zoë", loaded.getFirstName());
        assertEquals("O’Connor", loaded.getLastName());
        assertEquals("Excellent — calm under pressure ⚽", loaded.getNotes());
    }

    @Test
    void twentyPlayerRearrangementRoundTripsExactly() throws Exception {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            players.add(player("Player" + i, "Surname" + i, "07" + String.format("%09d", i), "P" + i, "Note " + i, PlayerStatus.UNREVIEWED));
        }

        javafx.collections.ObservableList<Player> observable = javafx.collections.FXCollections.observableArrayList(players);
        RankingService ranking = new RankingService(observable);
        ranking.moveToRank(observable.get(19), 1);     // 20 -> 1
        ranking.moveToRank(observable.get(10), 4);
        ranking.moveToBottom(observable.get(2));
        ranking.moveUp(observable.get(8));

        List<String> expectedOrder = observable.stream().map(Player::getFirstName).toList();
        Path file = tempDir.resolve("twenty.xlsx");
        service.save(file, observable);
        List<Player> loaded = service.load(file);

        assertEquals(expectedOrder, loaded.stream().map(Player::getFirstName).toList());
        for (int i = 0; i < loaded.size(); i++) {
            assertEquals(observable.get(i).getNotes(), loaded.get(i).getNotes());
            assertEquals(observable.get(i).getPhone(), loaded.get(i).getPhone());
        }
    }

    private Player player(String first, String last, String phone, String position, String notes, PlayerStatus status) {
        Player p = new Player(first, last);
        p.setPhone(phone);
        p.setPosition(position);
        p.setNotes(notes);
        p.setStatus(status);
        return p;
    }
}
