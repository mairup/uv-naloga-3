package com.uv.naloge.naloga3;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ReservationFormConfigurator {

    static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd. MM. yyyy");

    static final Map<String, List<String>> DESTINATION_CATALOG = new LinkedHashMap<>();
    static final List<String> TRANSPORT_MODES = List.of(
            "Letalo", "Ladja", "Avtobus", "Kolo", "Vlak", "Po izbiri");
    static final List<String> PAYER_COUNTRIES = List.of(
            "Slovenija", "Hrvaška", "Italija", "Avstrija", "Španija", "Nemčija", "Francija");

    static {
        DESTINATION_CATALOG.put("Slovenija", List.of("Bled", "Bovec", "Piran", "Moravske Toplice"));
        DESTINATION_CATALOG.put("Hrvaška", List.of("Rovinj", "Split", "Dubrovnik", "Otok Krk"));
        DESTINATION_CATALOG.put("Italija", List.of("Benetke", "Trst", "Rim", "Sardinija"));
        DESTINATION_CATALOG.put("Avstrija", List.of("Dunaj", "Salzburg", "Innsbruck", "Beljak"));
        DESTINATION_CATALOG.put("Španija", List.of("Barcelona", "Madrid", "Malaga", "Mallorca"));
    }

    static StringConverter<LocalDate> createDateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : INPUT_DATE_FORMATTER.format(date);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(string, INPUT_DATE_FORMATTER);
                } catch (Exception ignored) {
                    return null;
                }
            }
        };
    }

    static Callback<DatePicker, DateCell> disablePastDatesDayCellFactory() {
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
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    getStyleClass().add("disabled-date");
                }
            }
        };
    }

    static void configureChoiceControls(ComboBox<String> destinationCountry,
            ComboBox<String> destination, ComboBox<String> transportMode,
            TextField customTransportMode, Label customTransportModeLabel,
            ToggleGroup accommodationType, RadioButton customAccommodationOption,
            TextField customAccommodationType, ComboBox<String> payerCountry,
            Runnable refreshDestinations) {

        destinationCountry.setItems(FXCollections.observableArrayList(DESTINATION_CATALOG.keySet()));
        destinationCountry.valueProperty()
                .addListener((observable, oldValue, newValue) -> refreshDestinations.run());

        transportMode.setItems(FXCollections.observableArrayList(TRANSPORT_MODES));
        transportMode.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean custom = "Po izbiri".equals(newValue);
            hideAndDisable(customTransportMode, !custom);
            hideAndDisable(customTransportModeLabel, !custom);
            if (!custom)
                customTransportMode.clear();
        });

        accommodationType.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean custom = newToggle == customAccommodationOption;
            hideAndDisable(customAccommodationType, !custom);
            if (!custom)
                customAccommodationType.clear();
        });

        payerCountry.setItems(FXCollections.observableArrayList(PAYER_COUNTRIES));
    }

    static void configureNumericInputControls(TextField payerHouseNumber,
            TextField cardNumber, TextField cardSecurityCode) {

        payerHouseNumber.setTextFormatter(
                new TextFormatter<>(
                        change -> change.getControlNewText().matches("\\d{0,4}\\s?[a-zA-Z]?") ? change : null));

        cardNumber.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,19}") ? change : null));
        cardNumber.setPromptText("1234 5678 1234 5678");

        cardSecurityCode.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));
    }

    static void configureDatePickers(DatePicker departureDate, DatePicker returnDate, DatePicker payerBirthDate) {
        StringConverter<LocalDate> converter = createDateConverter();
        departureDate.setConverter(converter);
        returnDate.setConverter(converter);
        payerBirthDate.setConverter(converter);

        departureDate.setDayCellFactory(disablePastDatesDayCellFactory());
        returnDate.setDayCellFactory(disablePastDatesDayCellFactory());
        payerBirthDate.setDayCellFactory(disableFutureDatesDayCellFactory());
    }

    static Callback<DatePicker, DateCell> disableFutureDatesDayCellFactory() {
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

    static void configureMenuActions(MenuItem openItem, MenuItem saveItem, MenuItem resetItem,
            MenuItem closeItem, MenuItem aboutItem,
            Runnable onOpen, Runnable onSave, Runnable onReset, Runnable onClose, Runnable onAbout) {
        openItem.setOnAction(event -> onOpen.run());
        saveItem.setOnAction(event -> onSave.run());
        resetItem.setOnAction(event -> onReset.run());
        closeItem.setOnAction(event -> onClose.run());
        aboutItem.setOnAction(event -> onAbout.run());
    }

    static void configureResponsiveButtons(Button btnPrev, Button btnNext) {
        Platform.runLater(() -> {
            if (btnNext.getScene() != null) {
                updateResponsiveControls(btnPrev, btnNext, btnNext.getScene().getWidth());
            }
        });

        btnNext.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateResponsiveControls(btnPrev, btnNext, newScene.getWidth());
                newScene.widthProperty().addListener((szObs, oldWidth, newWidth) -> {
                    updateResponsiveControls(btnPrev, btnNext, newWidth.doubleValue());
                });
            }
        });
    }

    static void updateResponsiveControls(Button btnPrev, Button btnNext, double windowWidth) {
        boolean compactNavigation = windowWidth < 520;

        if (compactNavigation) {
            btnPrev.setText("");
            btnNext.setText("");
            btnPrev.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnNext.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            btnPrev.setText("Nazaj");
            btnNext.setText("Naprej");
            btnPrev.setContentDisplay(ContentDisplay.LEFT);
            btnNext.setContentDisplay(ContentDisplay.RIGHT);
        }
    }

    static void hideAndDisable(Node node, boolean hide) {
        if (node == null)
            return;
        node.setVisible(!hide);
        node.setManaged(!hide);
        node.setDisable(hide);
    }

    static void refreshDestinationChoices(ComboBox<String> destination, String country) {
        List<String> destinations = DESTINATION_CATALOG.getOrDefault(country, List.of());
        destination.setItems(FXCollections.observableArrayList(destinations));
        if (!destinations.isEmpty()) {
            destination.getSelectionModel().selectFirst();
        } else {
            destination.getSelectionModel().clearSelection();
        }
    }

    static void resetDestinationAndLocation(ComboBox<String> destinationCountry, ComboBox<String> destination,
            TextField accommodationLocation, Runnable refreshDestinations) {
        destinationCountry.getSelectionModel().selectFirst();
        refreshDestinations.run();
        destination.getSelectionModel().selectFirst();
        accommodationLocation.clear();
    }

    static void resetDates(DatePicker departureDate, DatePicker returnDate) {
        LocalDate defaultDepartureDate = LocalDate.now().plusDays(30);
        departureDate.setValue(defaultDepartureDate);
        returnDate.setValue(defaultDepartureDate.plusDays(7));
    }

    static void resetTransportAndAccommodation(ComboBox<String> transportMode,
            TextField customTransportMode, Label customTransportModeLabel,
            ToggleGroup accommodationType, RadioButton roomAccommodation,
            TextField customAccommodationType) {
        transportMode.getSelectionModel().select("Avtobus");
        customTransportMode.clear();
        hideAndDisable(customTransportMode, true);
        hideAndDisable(customTransportModeLabel, true);

        if (roomAccommodation != null)
            accommodationType.selectToggle(roomAccommodation);
        customAccommodationType.clear();
        hideAndDisable(customAccommodationType, true);
    }

    static void resetPayerDetails(TextField payerName, TextField payerSurname,
            TextField payerStreet, TextField payerHouseNumber,
            ComboBox<String> payerCountry, DatePicker payerBirthDate) {
        java.util.stream.Stream.of(payerName, payerSurname, payerStreet, payerHouseNumber)
                .filter(Objects::nonNull).forEach(TextField::clear);
        payerCountry.getSelectionModel().select("Slovenija");
        payerBirthDate.setValue(null);
    }

    static void resetCardDetails(TextField cardNumber, TextField cardHolder, TextField cardSecurityCode) {
        java.util.stream.Stream.of(cardNumber, cardHolder, cardSecurityCode)
                .filter(Objects::nonNull).forEach(TextField::clear);
    }

    static List<String> selectedTransportModes(ComboBox<String> transportMode, TextField customTransportMode) {
        if ("Po izbiri".equals(transportMode.getValue())) {
            if (!ReservationValidator.isBlank(customTransportMode.getText())) {
                return List.of(customTransportMode.getText().trim());
            }
            return List.of();
        }
        if (!ReservationValidator.isBlank(transportMode.getValue())) {
            return List.of(transportMode.getValue());
        }
        return List.of();
    }

    static String selectedAccommodationType(ToggleGroup accommodationType,
            RadioButton customAccommodationOption,
            TextField customAccommodationType) {
        var selected = accommodationType.getSelectedToggle();
        if (selected == null) {
            return "";
        }
        if (selected == customAccommodationOption) {
            return ReservationValidator.isBlank(customAccommodationType.getText())
                    ? ""
                    : customAccommodationType.getText().trim();
        }
        return ((RadioButton) selected).getText();
    }

    static List<String> selectedSpecialRequirements(Map<CheckBox, String> requirementLabels) {
        List<String> selected = new ArrayList<>();
        requirementLabels.forEach((checkbox, name) -> {
            if (checkbox != null && checkbox.isSelected()) {
                selected.add(name);
            }
        });
        if (selected.isEmpty()) {
            selected.add("Brez posebnih zahtev");
        }
        return selected;
    }
}
