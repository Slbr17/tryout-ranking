package com.example.tryoutranking.controller;

import com.example.tryoutranking.Main;
import com.example.tryoutranking.model.Player;
import com.example.tryoutranking.model.PlayerStatus;
import com.example.tryoutranking.service.ExcelService;
import com.example.tryoutranking.service.PlayerListStateCommand;
import com.example.tryoutranking.service.RankingService;
import com.example.tryoutranking.service.UndoManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

public class MainController {
    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private TableView<Player> playerTable;
    @FXML private TableColumn<Player, Number> rankColumn;
    @FXML private TableColumn<Player, String> firstNameColumn;
    @FXML private TableColumn<Player, String> lastNameColumn;
    @FXML private TableColumn<Player, String> positionColumn;
    @FXML private TableColumn<Player, String> phoneColumn;
    @FXML private TableColumn<Player, String> emailColumn;
    @FXML private TableColumn<Player, LocalDate> dobColumn;
    @FXML private TableColumn<Player, PlayerStatus> statusColumn;
    @FXML private TableColumn<Player, String> notesColumn;

    @FXML private TextField searchField;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;
    @FXML private Button moveTopButton;
    @FXML private Button moveBottomButton;
    @FXML private TextField rankInput;
    @FXML private Button rankGoButton;
    @FXML private Label selectedLabel;
    @FXML private Label countLabel;
    @FXML private MenuItem undoMenuItem;
    @FXML private MenuItem redoMenuItem;
    @FXML private MenuItem editMenuItem;
    @FXML private MenuItem deleteMenuItem;

    private final ObservableList<Player> players = FXCollections.observableArrayList();
    private final FilteredList<Player> filteredPlayers = new FilteredList<>(players, p -> true);
    private final RankingService rankingService = new RankingService(players);
    private final ExcelService excelService = new ExcelService();
    private final UndoManager undoManager = new UndoManager();

    private Stage stage;
    private Path currentFile;
    private boolean dirty;
    private String savedFingerprint;
    private Player draggedPlayer;

    @FXML
    private void initialize() {
        configureTable();
        configureSearch();
        configureSelectionState();
        configureUndoRedo();

        savedFingerprint = fingerprint(players);
        updateDocumentState();

        Platform.runLater(this::installKeyboardShortcuts);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        updateTitle();
    }

    private void configureTable() {
        playerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        playerTable.setItems(filteredPlayers);
        playerTable.setEditable(true);
        playerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playerTable.setPlaceholder(new Label("No players yet. Click + Add Player to begin."));

        rankColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(players.indexOf(data.getValue()) + 1));
        rankColumn.setStyle("-fx-alignment: CENTER;");

        configureEditableTextColumn(firstNameColumn, Player::getFirstName, Player::setFirstName, "Edit first name");
        configureEditableTextColumn(lastNameColumn, Player::getLastName, Player::setLastName, "Edit last name");
        configureEditableTextColumn(positionColumn, Player::getPosition, Player::setPosition, "Edit position");
        configureEditableTextColumn(phoneColumn, Player::getPhone, Player::setPhone, "Edit phone");
        configureEditableTextColumn(emailColumn, Player::getEmail, Player::setEmail, "Edit email");

        dobColumn.setCellValueFactory(data -> data.getValue().dateOfBirthProperty());
        dobColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : DOB_FORMAT.format(date));
            }
        });

        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(PlayerStatus.values()));
        statusColumn.setOnEditCommit(event -> {
            Player player = event.getRowValue();
            PlayerStatus newStatus = event.getNewValue();
            if (newStatus != null && newStatus != event.getOldValue()) {
                performMutation("Change status", player.getId(), () -> player.setStatus(newStatus));
            }
        });

        notesColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(notesPreview(data.getValue().getNotes())));
        notesColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(value);
                    Player p = getTableRow() == null ? null : (Player) getTableRow().getItem();
                    setTooltip(p == null || p.getNotes().isBlank() ? null : new Tooltip(p.getNotes()));
                }
            }
        });

        playerTable.getColumns().forEach(column -> column.setSortable(false));
        configureRowDragAndDrop();

        playerTable.setRowFactory(table -> {
            TableRow<Player> row = createDragDropRow(table);
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    playerTable.getSelectionModel().select(row.getItem());
                    handleEditPlayer();
                }
            });
            return row;
        });
    }

    private void configureEditableTextColumn(TableColumn<Player, String> column,
                                             java.util.function.Function<Player, String> getter,
                                             BiConsumer<Player, String> setter,
                                             String description) {
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            Player player = event.getRowValue();
            String oldValue = getter.apply(player);
            String newValue = event.getNewValue() == null ? "" : event.getNewValue().trim();
            if (!Objects.equals(oldValue, newValue)) {
                performMutation(description, player.getId(), () -> setter.accept(player, newValue));
            }
        });
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String needle = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filteredPlayers.setPredicate(player -> needle.isBlank() || matchesSearch(player, needle));
            playerTable.refresh();
            updateCountLabel();
        });
    }

    private boolean matchesSearch(Player p, String needle) {
        return p.getFirstName().toLowerCase(Locale.ROOT).contains(needle)
                || p.getLastName().toLowerCase(Locale.ROOT).contains(needle)
                || p.getPhone().toLowerCase(Locale.ROOT).contains(needle)
                || p.getEmail().toLowerCase(Locale.ROOT).contains(needle)
                || p.getPosition().toLowerCase(Locale.ROOT).contains(needle)
                || p.getStatus().toString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private void configureSelectionState() {
        playerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateSelectionControls());
        players.addListener((javafx.collections.ListChangeListener<Player>) change -> {
            updateSelectionControls();
            updateCountLabel();
        });
        updateSelectionControls();
        updateCountLabel();
    }

    private void configureUndoRedo() {
        undoMenuItem.disableProperty().bind(undoManager.canUndoProperty().not());
        redoMenuItem.disableProperty().bind(undoManager.canRedoProperty().not());
    }

    private void installKeyboardShortcuts() {
        if (playerTable.getScene() == null) return;
        playerTable.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl) return;

            if (event.isAltDown() && event.getCode() == KeyCode.UP) {
                handleMoveUp(); event.consume();
            } else if (event.isAltDown() && event.getCode() == KeyCode.DOWN) {
                handleMoveDown(); event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.UP) {
                handleMoveTop(); event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.DOWN) {
                handleMoveBottom(); event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.Z) {
                handleUndo(); event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.Y) {
                handleRedo(); event.consume();
            }
        });
    }

    private void configureRowDragAndDrop() {
        // The actual handlers are attached in the row factory so each row can display insertion feedback.
    }

    private TableRow<Player> createDragDropRow(TableView<Player> table) {
        TableRow<Player> row = new TableRow<>();

        row.setOnDragDetected(event -> {
            if (row.isEmpty()) return;
            draggedPlayer = row.getItem();
            table.getSelectionModel().select(draggedPlayer);
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(draggedPlayer.getId().toString());
            db.setContent(content);
            event.consume();
        });

        row.setOnDragOver(event -> {
            if (draggedPlayer != null && draggedPlayer != row.getItem() && event.getGestureSource() != row) {
                event.acceptTransferModes(TransferMode.MOVE);
                updateDropPseudoClasses(row, event.getY() < row.getHeight() / 2.0);
            }
            event.consume();
        });

        row.setOnDragExited(event -> clearDropPseudoClasses(row));

        row.setOnDragDropped(event -> {
            boolean completed = false;
            if (draggedPlayer != null) {
                UUID id = draggedPlayer.getId();
                int sourceIndex = players.indexOf(draggedPlayer);
                int insertionPoint;

                if (row.isEmpty() || row.getItem() == null) {
                    insertionPoint = players.size();
                } else {
                    int targetIndex = players.indexOf(row.getItem());
                    boolean after = event.getY() >= row.getHeight() / 2.0;
                    insertionPoint = targetIndex + (after ? 1 : 0);
                }

                if (sourceIndex >= 0) {
                    int finalIndex = insertionPoint;
                    if (sourceIndex < insertionPoint) finalIndex--;
                    finalIndex = Math.max(0, Math.min(finalIndex, players.size() - 1));
                    final int moveIndex = finalIndex;
                    performMutation("Drag player", id, () -> rankingService.moveToIndex(findById(id), moveIndex));
                    completed = true;
                }
            }
            clearDropPseudoClasses(row);
            draggedPlayer = null;
            event.setDropCompleted(completed);
            event.consume();
        });

        row.setOnDragDone(event -> {
            clearDropPseudoClasses(row);
            draggedPlayer = null;
        });

        return row;
    }

    private void updateDropPseudoClasses(TableRow<Player> row, boolean before) {
        row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-before"), before);
        row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-after"), !before);
    }

    private void clearDropPseudoClasses(TableRow<Player> row) {
        row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-before"), false);
        row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop-after"), false);
    }

    @FXML
    private void handleAddPlayer() {
        Player player = new Player();
        showPlayerDialog(player, "Add Player").ifPresent(result ->
                performMutation("Add player", result.getId(), () -> rankingService.addPlayer(result)));
    }

    @FXML
    private void handleEditPlayer() {
        Player selected = getSelectedPlayer();
        if (selected == null) return;
        showPlayerDialog(selected.copy(), "Edit Player").ifPresent(edited ->
                performMutation("Edit player", selected.getId(), () -> selected.copyValuesFrom(edited)));
    }

    @FXML
    private void handleDeletePlayer() {
        Player selected = getSelectedPlayer();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Remove Player");
        confirm.setHeaderText("Remove " + selected.getDisplayName() + " from this tryout?");
        confirm.setContentText("The remaining players will automatically move up in the ranking.");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int oldIndex = players.indexOf(selected);
            performMutation("Delete player", null, () -> rankingService.deletePlayer(selected));
            if (!players.isEmpty()) {
                int newIndex = Math.min(oldIndex, players.size() - 1);
                selectPlayer(players.get(newIndex).getId());
            }
        }
    }

    @FXML private void handleMoveUp() { moveSelected("Move player up", p -> rankingService.moveUp(p)); }
    @FXML private void handleMoveDown() { moveSelected("Move player down", p -> rankingService.moveDown(p)); }
    @FXML private void handleMoveTop() { moveSelected("Move player to top", p -> rankingService.moveToTop(p)); }
    @FXML private void handleMoveBottom() { moveSelected("Move player to bottom", p -> rankingService.moveToBottom(p)); }

    private void moveSelected(String description, java.util.function.Predicate<Player> action) {
        Player selected = getSelectedPlayer();
        if (selected == null) return;
        UUID id = selected.getId();
        performMutation(description, id, () -> action.test(findById(id)));
    }

    @FXML
    private void handleMoveToRank() {
        Player selected = getSelectedPlayer();
        if (selected == null) return;
        try {
            int rank = Integer.parseInt(rankInput.getText().trim());
            if (rank < 1 || rank > players.size()) throw new NumberFormatException();
            UUID id = selected.getId();
            performMutation("Move player to rank " + rank, id,
                    () -> rankingService.moveToRank(findById(id), rank));
            rankInput.clear();
        } catch (NumberFormatException ex) {
            showWarning("Invalid rank", "Enter a whole number between 1 and " + players.size() + ".");
        }
    }

    @FXML
    private void handleUndo() {
        UUID selection = selectedId();
        if (undoManager.undo()) {
            afterStateRestore(selection);
        }
    }

    @FXML
    private void handleRedo() {
        UUID selection = selectedId();
        if (undoManager.redo()) {
            afterStateRestore(selection);
        }
    }

    @FXML
    private void handleNew() {
        if (!confirmSaveIfNeeded("creating a new list")) return;
        players.clear();
        currentFile = null;
        searchField.clear();
        undoManager.clear();
        markSavedState();
        playerTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleOpen() {
        if (!confirmSaveIfNeeded("opening another file")) return;

        FileChooser chooser = excelFileChooser("Open Tryout Ranking");
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;

        try {
            List<Player> loaded = excelService.load(selected.toPath());
            players.setAll(loaded);
            currentFile = selected.toPath();
            searchField.clear();
            undoManager.clear();
            markSavedState();
            if (!players.isEmpty()) selectPlayer(players.getFirst().getId());
        } catch (Exception ex) {
            showError("Could not open workbook", ex.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        saveDocument(false);
    }

    @FXML
    private void handleSaveAs() {
        saveDocument(true);
    }

    @FXML
    private void handleExit() {
        if (stage != null) stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void handleCloseRequest(WindowEvent event) {
        if (!confirmSaveIfNeeded("closing the application")) {
            event.consume();
        }
    }

    private boolean saveDocument(boolean forceSaveAs) {
        Path target = currentFile;
        if (forceSaveAs || target == null) {
            FileChooser chooser = excelFileChooser("Save Tryout Ranking");
            chooser.setInitialFileName(currentFile == null ? "Tryout-Ranking.xlsx" : currentFile.getFileName().toString());
            File chosen = chooser.showSaveDialog(stage);
            if (chosen == null) return false;
            target = ensureXlsxExtension(chosen.toPath());
        }

        try {
            excelService.save(target, players);
            currentFile = target;
            markSavedState();
            return true;
        } catch (Exception ex) {
            showError("Could not save workbook", ex.getMessage());
            return false;
        }
    }

    private boolean confirmSaveIfNeeded(String action) {
        updateDocumentState();
        if (!dirty) return true;

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.YES);
        ButtonType dontSave = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Would you like to save before " + action + "?");
        alert.getButtonTypes().setAll(save, dontSave, cancel);

        ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == cancel) return false;
        if (result == save) return saveDocument(false);
        return true;
    }

    private Optional<Player> showPlayerDialog(Player initial, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/player-dialog.fxml"));
            Node content = loader.load();
            PlayerDialogController controller = loader.getController();
            controller.setPlayer(initial);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(stage);
            dialog.setTitle(title);
            dialog.setHeaderText(title.equals("Add Player") ? "Add a player to the bottom of the ranking" : initial.getDisplayName());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dialog.getDialogPane().getStylesheets().add(Main.class.getResource("/css/application.css").toExternalForm());

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, event -> {
                if (!controller.validate()) event.consume();
            });

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                controller.applyTo(initial);
                return Optional.of(initial);
            }
        } catch (IOException ex) {
            showError("Could not open player editor", ex.getMessage());
        }
        return Optional.empty();
    }

    private void performMutation(String description, UUID selectAfter, Runnable mutation) {
        List<Player> before = PlayerListStateCommand.deepCopy(players);
        mutation.run();
        List<Player> after = PlayerListStateCommand.deepCopy(players);

        if (!fingerprint(before).equals(fingerprint(after))) {
            undoManager.recordExecuted(new PlayerListStateCommand(players, before, after, description));
            playerTable.refresh();
            updateDocumentState();
            if (selectAfter != null) selectPlayer(selectAfter);
        }
    }

    private void afterStateRestore(UUID preferredSelection) {
        filteredPlayers.setPredicate(filteredPlayers.getPredicate());
        playerTable.refresh();
        updateDocumentState();
        if (preferredSelection != null && findById(preferredSelection) != null) {
            selectPlayer(preferredSelection);
        } else if (!filteredPlayers.isEmpty()) {
            playerTable.getSelectionModel().selectFirst();
        }
    }

    private void markSavedState() {
        savedFingerprint = fingerprint(players);
        dirty = false;
        updateTitle();
        updateSelectionControls();
        updateCountLabel();
    }

    private void updateDocumentState() {
        dirty = !Objects.equals(savedFingerprint, fingerprint(players));
        updateTitle();
        updateSelectionControls();
        updateCountLabel();
    }

    private void updateTitle() {
        if (stage == null) return;
        String fileName = currentFile == null ? "Untitled" : currentFile.getFileName().toString();
        stage.setTitle("Tryout Ranking — " + fileName + (dirty ? " *" : ""));
    }

    private void updateSelectionControls() {
        Player selected = getSelectedPlayer();
        boolean none = selected == null;
        int index = none ? -1 : players.indexOf(selected);
        int last = players.size() - 1;

        editButton.setDisable(none);
        deleteButton.setDisable(none);
        editMenuItem.setDisable(none);
        deleteMenuItem.setDisable(none);
        moveUpButton.setDisable(none || index <= 0);
        moveTopButton.setDisable(none || index <= 0);
        moveDownButton.setDisable(none || index < 0 || index >= last);
        moveBottomButton.setDisable(none || index < 0 || index >= last);
        rankInput.setDisable(none);
        rankGoButton.setDisable(none);

        if (none) {
            selectedLabel.setText("Selected: —");
        } else {
            selectedLabel.setText("Selected: " + selected.getDisplayName() + " — Rank #" + (index + 1));
        }
    }

    private void updateCountLabel() {
        if (filteredPlayers.size() == players.size()) {
            countLabel.setText(players.size() + (players.size() == 1 ? " player" : " players"));
        } else {
            countLabel.setText(filteredPlayers.size() + " shown / " + players.size() + " total");
        }
    }

    private Player getSelectedPlayer() {
        return playerTable.getSelectionModel().getSelectedItem();
    }

    private UUID selectedId() {
        Player p = getSelectedPlayer();
        return p == null ? null : p.getId();
    }

    private Player findById(UUID id) {
        if (id == null) return null;
        for (Player player : players) {
            if (player.getId().equals(id)) return player;
        }
        return null;
    }

    private void selectPlayer(UUID id) {
        Player player = findById(id);
        if (player == null || !filteredPlayers.contains(player)) return;
        playerTable.getSelectionModel().select(player);
        playerTable.scrollTo(player);
    }

    private FileChooser excelFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        if (currentFile != null && currentFile.toAbsolutePath().getParent() != null) {
            File directory = currentFile.toAbsolutePath().getParent().toFile();
            if (directory.isDirectory()) chooser.setInitialDirectory(directory);
        }
        return chooser;
    }

    private static Path ensureXlsxExtension(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) return path;
        return path.resolveSibling(name + ".xlsx");
    }

    private static String notesPreview(String notes) {
        if (notes == null) return "";
        String singleLine = notes.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 80 ? singleLine : singleLine.substring(0, 77) + "…";
    }

    private static String fingerprint(List<Player> list) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Player p : list) {
                updateDigest(digest, p.getId().toString());
                updateDigest(digest, p.getFirstName());
                updateDigest(digest, p.getLastName());
                updateDigest(digest, p.getPosition());
                updateDigest(digest, p.getPhone());
                updateDigest(digest, p.getEmail());
                updateDigest(digest, p.getDateOfBirth() == null ? "" : p.getDateOfBirth().toString());
                updateDigest(digest, p.getStatus().name());
                updateDigest(digest, p.getNotes());
                digest.update((byte) 0x1E);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0x1F);
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
