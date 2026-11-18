package com.example.tryoutranking.service;

import com.example.tryoutranking.model.Player;
import javafx.collections.ObservableList;

import java.util.List;

public class PlayerListStateCommand implements UndoManager.UndoableCommand {
    private final ObservableList<Player> target;
    private final List<Player> before;
    private final List<Player> after;
    private final String description;

    public PlayerListStateCommand(ObservableList<Player> target,
                                  List<Player> before,
                                  List<Player> after,
                                  String description) {
        this.target = target;
        this.before = deepCopy(before);
        this.after = deepCopy(after);
        this.description = description;
    }

    @Override
    public void undo() {
        restore(before);
    }

    @Override
    public void redo() {
        restore(after);
    }

    @Override
    public String description() {
        return description;
    }

    private void restore(List<Player> state) {
        target.setAll(deepCopy(state));
    }

    public static List<Player> deepCopy(List<Player> source) {
        return source.stream().map(Player::copy).toList();
    }
}
