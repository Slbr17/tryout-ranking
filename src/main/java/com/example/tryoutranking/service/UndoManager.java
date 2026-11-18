package com.example.tryoutranking.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    public interface UndoableCommand {
        void undo();
        void redo();
        String description();
    }

    private final Deque<UndoableCommand> undoStack = new ArrayDeque<>();
    private final Deque<UndoableCommand> redoStack = new ArrayDeque<>();
    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);

    public void recordExecuted(UndoableCommand command) {
        undoStack.push(command);
        redoStack.clear();
        refreshState();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        UndoableCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        refreshState();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        UndoableCommand command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        refreshState();
        return true;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        refreshState();
    }

    public ReadOnlyBooleanProperty canUndoProperty() { return canUndo; }
    public ReadOnlyBooleanProperty canRedoProperty() { return canRedo; }

    private void refreshState() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
    }
}
