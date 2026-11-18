package com.example.tryoutranking.controller;

import com.example.tryoutranking.model.Player;
import com.example.tryoutranking.model.PlayerStatus;
import com.example.tryoutranking.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PlayerDialogController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField positionField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<PlayerStatus> statusCombo;
    @FXML private TextArea notesArea;
    @FXML private Label validationLabel;

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll(PlayerStatus.values());
        statusCombo.setValue(PlayerStatus.UNREVIEWED);
    }

    public void setPlayer(Player player) {
        firstNameField.setText(player.getFirstName());
        lastNameField.setText(player.getLastName());
        phoneField.setText(player.getPhone());
        emailField.setText(player.getEmail());
        positionField.setText(player.getPosition());
        dobPicker.setValue(player.getDateOfBirth());
        statusCombo.setValue(player.getStatus());
        notesArea.setText(player.getNotes());
    }

    public boolean validate() {
        validationLabel.setText("");
        if (!ValidationUtil.hasName(firstNameField.getText(), lastNameField.getText())) {
            validationLabel.setText("Enter at least a first name or last name.");
            return false;
        }
        if (!ValidationUtil.isValidEmailOrBlank(emailField.getText())) {
            validationLabel.setText("Enter a valid email address, or leave it blank.");
            return false;
        }
        return true;
    }

    public void applyTo(Player player) {
        player.setFirstName(firstNameField.getText().trim());
        player.setLastName(lastNameField.getText().trim());
        player.setPhone(phoneField.getText().trim());
        player.setEmail(emailField.getText().trim());
        player.setPosition(positionField.getText().trim());
        player.setDateOfBirth(dobPicker.getValue());
        player.setStatus(statusCombo.getValue());
        player.setNotes(notesArea.getText());
    }
}
