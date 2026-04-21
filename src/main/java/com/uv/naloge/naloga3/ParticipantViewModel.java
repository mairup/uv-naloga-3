package com.uv.naloge.naloga3;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

class ParticipantViewModel {
    public final StringProperty name = new SimpleStringProperty();
    public final StringProperty surname = new SimpleStringProperty();
    public final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
}
