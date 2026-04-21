package com.uv.naloge.naloga3;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

class ParticipantManager {

    private final VBox participantRowsBox;
    private final ReservationViewModel viewModel;
    private final ReservationValidator validator;
    private final Label participantCount;
    private final List<ParticipantRow> participantRows = new ArrayList<>();
    private Button addParticipantButton;
    private boolean participantRowsRefreshing = false;

    ParticipantManager(VBox participantRowsBox, ReservationViewModel viewModel,
            ReservationValidator validator, Label participantCount) {
        this.participantRowsBox = participantRowsBox;
        this.viewModel = viewModel;
        this.validator = validator;
        this.participantCount = participantCount;
    }

    List<ParticipantRow> getParticipantRows() {
        return participantRows;
    }

    boolean isParticipantRowsRefreshing() {
        return participantRowsRefreshing;
    }

    void rebuildParticipantRows() {
        participantRowsRefreshing = true;
        try {
            participantRowsBox.getChildren().clear();
            participantRows.clear();

            for (int i = 0; i < viewModel.participants.size(); i++) {
                ParticipantViewModel participant = viewModel.participants.get(i);
                ParticipantRow row = createParticipantRow(i + 1, participant);
                participantRows.add(row);
                participantRowsBox.getChildren().add(row.container);
            }

            updateParticipantRowIndices();
            ensureAddParticipantButton();
        } finally {
            participantRowsRefreshing = false;
        }

        updateParticipantCount();
    }

    void resetParticipants() {
        viewModel.participants.clear();
        viewModel.participants.add(new ParticipantViewModel());
        rebuildParticipantRows();
    }

    void addParticipantRow() {
        ParticipantViewModel participant = new ParticipantViewModel();
        viewModel.participants.add(participant);

        participantRowsRefreshing = true;
        try {
            if (addParticipantButton != null) {
                participantRowsBox.getChildren().remove(addParticipantButton);
            }

            ParticipantRow row = createParticipantRow(viewModel.participants.size(), participant);
            participantRows.add(row);
            participantRowsBox.getChildren().add(row.container);
            updateParticipantRowIndices();
            ensureAddParticipantButton();
        } finally {
            participantRowsRefreshing = false;
        }

        updateParticipantCount();
        ParticipantRow lastRow = participantRows.get(participantRows.size() - 1);
        Platform.runLater(lastRow.name::requestFocus);
    }

    void removeParticipantRow(int index) {
        if (index < 0 || index >= viewModel.participants.size())
            return;

        participantRowsRefreshing = true;
        try {
            viewModel.participants.remove(index);
            ParticipantRow removedRow = participantRows.remove(index);
            participantRowsBox.getChildren().remove(removedRow.container);
            updateParticipantRowIndices();
        } finally {
            participantRowsRefreshing = false;
        }

        ensureAddParticipantButton();
        updateParticipantCount();
    }

    void onParticipantRowChanged() {
        if (participantRowsRefreshing)
            return;
        updateParticipantCount();
    }

    void updateParticipantCount() {
        int filledRows = ReservationValidator.filledParticipantCount(viewModel.participants);
        participantCount.setText("Vnesene osebe: " + filledRows);
    }

    private ParticipantRow createParticipantRow(int index, ParticipantViewModel participant) {
        ParticipantRow row = new ParticipantRow(index);

        row.name.textProperty().bindBidirectional(participant.name);
        row.surname.textProperty().bindBidirectional(participant.surname);
        row.birthDate.valueProperty().bindBidirectional(participant.birthDate);

        row.name.textProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());
        row.surname.textProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());
        row.birthDate.valueProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());

        validator.attachValidationRecovery(row.name);
        validator.attachValidationRecovery(row.surname);
        validator.attachValidationRecovery(row.birthDate);

        row.removeButton.setOnAction(event -> removeParticipantRow(participantRows.indexOf(row)));
        return row;
    }

    private Button createAddParticipantButton() {
        Button button = new Button();
        button.getStyleClass().add("tile-add-button");
        button.setPrefWidth(36);
        button.setMaxWidth(36);
        button.setMinWidth(36);
        org.kordamp.ikonli.javafx.FontIcon addIcon = new org.kordamp.ikonli.javafx.FontIcon("bi-plus");
        addIcon.setIconSize(22);
        addIcon.getStyleClass().add("tile-add-icon");
        button.setGraphic(addIcon);
        button.setOnAction(event -> addParticipantRow());
        button.setAlignment(javafx.geometry.Pos.CENTER);
        return button;
    }

    private void ensureAddParticipantButton() {
        if (addParticipantButton == null) {
            addParticipantButton = createAddParticipantButton();
        }
        if (!participantRowsBox.getChildren().contains(addParticipantButton)) {
            participantRowsBox.getChildren().add(addParticipantButton);
        }
    }

    private void updateParticipantRowIndices() {
        for (int i = 0; i < participantRows.size(); i++) {
            participantRows.get(i).setIndex(i + 1);
        }
    }
}
