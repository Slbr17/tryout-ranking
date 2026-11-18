package com.example.tryoutranking.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Player {
    private final UUID id;
    private final StringProperty firstName = new SimpleStringProperty("");
    private final StringProperty lastName = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty position = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
    private final ObjectProperty<PlayerStatus> status = new SimpleObjectProperty<>(PlayerStatus.UNREVIEWED);
    private final StringProperty notes = new SimpleStringProperty("");

    public Player() {
        this(UUID.randomUUID());
    }

    public Player(UUID id) {
        this.id = Objects.requireNonNull(id);
    }

    public Player(String firstName, String lastName) {
        this();
        setFirstName(firstName);
        setLastName(lastName);
    }

    public Player(Player other) {
        this(other.id);
        copyValuesFrom(other);
    }

    public void copyValuesFrom(Player other) {
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setPhone(other.getPhone());
        setEmail(other.getEmail());
        setPosition(other.getPosition());
        setDateOfBirth(other.getDateOfBirth());
        setStatus(other.getStatus());
        setNotes(other.getNotes());
    }

    public UUID getId() { return id; }

    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String value) { firstName.set(safe(value)); }
    public StringProperty firstNameProperty() { return firstName; }

    public String getLastName() { return lastName.get(); }
    public void setLastName(String value) { lastName.set(safe(value)); }
    public StringProperty lastNameProperty() { return lastName; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String value) { phone.set(safe(value)); }
    public StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(safe(value)); }
    public StringProperty emailProperty() { return email; }

    public String getPosition() { return position.get(); }
    public void setPosition(String value) { position.set(safe(value)); }
    public StringProperty positionProperty() { return position; }

    public LocalDate getDateOfBirth() { return dateOfBirth.get(); }
    public void setDateOfBirth(LocalDate value) { dateOfBirth.set(value); }
    public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }

    public PlayerStatus getStatus() { return status.get(); }
    public void setStatus(PlayerStatus value) { status.set(value == null ? PlayerStatus.UNREVIEWED : value); }
    public ObjectProperty<PlayerStatus> statusProperty() { return status; }

    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(safe(value)); }
    public StringProperty notesProperty() { return notes; }

    public String getDisplayName() {
        String combined = (getFirstName() + " " + getLastName()).trim();
        return combined.isBlank() ? "Unnamed player" : combined;
    }

    public Player copy() {
        return new Player(this);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
