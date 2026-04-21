package com.uv.naloge.naloga3;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;

class ReservationViewModel {
    public final StringProperty destinationCountry = new SimpleStringProperty();
    public final StringProperty destination = new SimpleStringProperty();
    public final StringProperty accommodationLocation = new SimpleStringProperty();
    public final ObjectProperty<LocalDate> departureDate = new SimpleObjectProperty<>();
    public final ObjectProperty<LocalDate> returnDate = new SimpleObjectProperty<>();
    public final ObservableList<String> transportModes = FXCollections.observableArrayList();
    public final StringProperty accommodationType = new SimpleStringProperty();
    public final ObservableList<String> specialRequirements = FXCollections.observableArrayList();
    public final StringProperty payerName = new SimpleStringProperty();
    public final StringProperty payerSurname = new SimpleStringProperty();
    public final StringProperty payerStreet = new SimpleStringProperty();
    public final StringProperty payerHouseNumber = new SimpleStringProperty();
    public final StringProperty payerCountry = new SimpleStringProperty();
    public final ObjectProperty<LocalDate> payerBirthDate = new SimpleObjectProperty<>();
    public final StringProperty cardNumber = new SimpleStringProperty();
    public final StringProperty cardHolder = new SimpleStringProperty();
    public final StringProperty cardSecurityCode = new SimpleStringProperty();
    public final ObservableList<ParticipantViewModel> participants = FXCollections.observableArrayList();
}
