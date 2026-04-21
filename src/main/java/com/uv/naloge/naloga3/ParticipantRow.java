package com.uv.naloge.naloga3;

import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;

class ParticipantRow {
    private static final StringConverter<LocalDate> DATE_FORMATTER = new StringConverter<>() {
        @Override
        public String toString(LocalDate date) {
            return date == null ? "" : date.format(ReservationFormConfigurator.INPUT_DATE_FORMATTER);
        }

        @Override
        public LocalDate fromString(String string) {
            if (string == null || string.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(string, ReservationFormConfigurator.INPUT_DATE_FORMATTER);
            } catch (Exception ignored) {
                return null;
            }
        }
    };

    private static Callback<DatePicker, DateCell> disableFutureDatesDayCellFactory() {
        return datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("disabled-date");
                if (empty || item == null) {
                    setDisable(false);
                    return;
                }
                setDisable(false);
                if (item.isAfter(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("disabled-date");
                }
            }
        };
    }

    final HBox container;
    final Label label;
    final TextField name;
    final TextField surname;
    final DatePicker birthDate;
    final Button removeButton;

    ParticipantRow(int index) {
        label = new Label();
        name = new TextField();
        surname = new TextField();
        birthDate = new DatePicker();
        removeButton = new Button("Odstrani");

        name.setPromptText("Ime");
        surname.setPromptText("Priimek");
        birthDate.setPromptText("Datum rojstva");
        removeButton.getStyleClass().add("tile-remove-button");
        removeButton.setText("");
        org.kordamp.ikonli.javafx.FontIcon removeIcon = new org.kordamp.ikonli.javafx.FontIcon("bi-trash");
        removeIcon.setIconSize(16);
        removeIcon.getStyleClass().add("tile-remove-icon");
        removeButton.setGraphic(removeIcon);

        birthDate.setConverter(DATE_FORMATTER);
        birthDate.setDayCellFactory(disableFutureDatesDayCellFactory());

        HBox.setHgrow(name, Priority.ALWAYS);
        HBox.setHgrow(surname, Priority.ALWAYS);
        HBox.setHgrow(birthDate, Priority.ALWAYS);

        container = new HBox(4, label, name, surname, birthDate, removeButton);
        container.getStyleClass().add("participant-row");

        setIndex(index);
    }

    void setIndex(int index) {
        label.setText(index + ".");
        label.setMinWidth(30);
        label.setPrefWidth(30);
        label.setMaxWidth(30);
        if (!label.getStyleClass().contains("participant-number")) {
            label.getStyleClass().add("participant-number");
        }
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }
}
