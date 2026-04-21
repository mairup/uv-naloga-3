package com.uv.naloge.naloga3;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class ReservationFileIO {

    ReservationFileIO() {
    }

    void saveReport(Window window, String reportText,
            Consumer<String> onNeutral,
            Consumer<String> onSuccess,
            Consumer<String> onError) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Shrani rezervacijo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Besedilna datoteka (*.txt)", "*.txt"));
        fileChooser.setInitialFileName(
                "rezervacija-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");

        File chosenFile = fileChooser.showSaveDialog(window);
        if (chosenFile == null) {
            onNeutral.accept("Shranjevanje je preklicano.");
            return;
        }

        try {
            Files.writeString(chosenFile.toPath(), reportText, StandardCharsets.UTF_8);
            onSuccess.accept("Vnos je shranjen: " + chosenFile.getName());
        } catch (IOException exception) {
            onError.accept("Shranjevanje ni uspelo: " + exception.getMessage());
        }
    }

    String openReport(Window window,
            Consumer<String> onNeutral,
            Consumer<String> onError) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odpri rezervacijo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Besedilna datoteka (*.txt)", "*.txt"));

        File chosenFile = fileChooser.showOpenDialog(window);
        if (chosenFile == null) {
            onNeutral.accept("Odpiranje je preklicano.");
            return null;
        }

        try {
            return Files.readString(chosenFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            onError.accept("Odpiranje ni uspelo: " + exception.getMessage());
            return null;
        }
    }

    Map<String, String> parseReportFields(String[] lines) {
        Map<String, String> fields = new HashMap<>();
        String currentSection = null;
        StringBuilder participantBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;

            if (trimmed.startsWith("REZERVACIJA")) {
                fields.put("REZERVACIJA POČITNIC", trimmed);
                continue;
            }
            if (trimmed.startsWith("Ustvarjeno:")) {
                continue;
            }

            if (trimmed.equals("DESTINACIJA") || trimmed.equals("PREVOZ")
                    || trimmed.equals("NASTANITEV IN ZAHTEVE")
                    || trimmed.equals("PLAČNIK") || trimmed.equals("KARTICA")
                    || trimmed.equals("OSEBE")) {
                currentSection = trimmed;
                if (trimmed.equals("OSEBE")) {
                    participantBlock = new StringBuilder();
                }
                continue;
            }

            if (currentSection != null && currentSection.equals("OSEBE")) {
                participantBlock.append(trimmed).append("\n");
                continue;
            }

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();
                if (currentSection != null && currentSection.equals("PLAČNIK") && key.equals("Država")) {
                    key = "Država plačnika";
                }
                if (currentSection != null && currentSection.equals("KARTICA")) {
                    if (key.equals("Številka"))
                        key = "Številka kartice";
                    if (key.equals("Imetnik"))
                        key = "Imetnik kartice";
                    if (key.equals("Varnostna koda"))
                        key = "Varnostna koda";
                }
                fields.put(key, value);
            } else if (currentSection != null && currentSection.equals("PREVOZ")) {
                fields.put("PREVOZ", trimmed);
            }
        }

        fields.put("OSEBE", participantBlock.toString().trim());
        return fields;
    }

    static LocalDate parseDateField(String value, DateTimeFormatter dateFormatter) {
        if (value == null || value.isEmpty())
            return null;
        try {
            return LocalDate.parse(value.trim(), dateFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    static void setComboBoxSafely(ComboBox<String> comboBox, Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.isEmpty())
            return;
        for (String item : comboBox.getItems()) {
            if (item.equals(value)) {
                comboBox.getSelectionModel().select(value);
                return;
            }
        }
    }

    void restoreFromText(String content,
            DateTimeFormatter dateFormatter,
            ReservationViewModel viewModel,
            ParticipantManager participantManager,
            Map<CheckBox, String> requirementLabels,
            ComboBox<String> destinationCountry,
            Runnable onDestinationCountryChanged,
            ComboBox<String> destination,
            TextField accommodationLocation,
            DatePicker departureDate,
            DatePicker returnDate,
            ComboBox<String> transportMode,
            TextField customTransportMode,
            Label customTransportModeLabel,
            ToggleGroup accommodationType,
            RadioButton customAccommodationOption,
            TextField customAccommodationType,
            TextField payerName,
            TextField payerSurname,
            TextField payerStreet,
            TextField payerHouseNumber,
            ComboBox<String> payerCountry,
            DatePicker payerBirthDate,
            TextField cardNumber,
            TextField cardHolder,
            TextField cardSecurityCode) {

        String[] lines = content.split("\\r?\\n");
        Map<String, String> fields = parseReportFields(lines);

        if (!fields.containsKey("REZERVACIJA POČITNIC")) {
            throw new IllegalArgumentException("manjka glava 'REZERVACIJA POČITNIC'");
        }

        setComboBoxSafely(destinationCountry, fields, "Država");
        onDestinationCountryChanged.run();
        setComboBoxSafely(destination, fields, "Kraj");
        accommodationLocation.setText(fields.getOrDefault("Kraj nastanitve", ""));
        departureDate.setValue(parseDateField(fields.get("Odhod"), dateFormatter));
        returnDate.setValue(parseDateField(fields.get("Vrnitev"), dateFormatter));

        restoreTransport(fields.getOrDefault("PREVOZ", ""),
                transportMode, customTransportMode, customTransportModeLabel);
        restoreAccommodationType(fields.getOrDefault("Tip nastanitve", ""),
                accommodationType, customAccommodationOption, customAccommodationType);
        restoreSpecialRequirements(fields.getOrDefault("Posebne zahteve", ""), requirementLabels);

        payerName.setText(fields.getOrDefault("Ime", ""));
        payerSurname.setText(fields.getOrDefault("Priimek", ""));
        payerStreet.setText(fields.getOrDefault("Ulica", ""));
        payerHouseNumber.setText(fields.getOrDefault("Hišna številka", ""));
        setComboBoxSafely(payerCountry, fields, "Država plačnika");
        payerBirthDate.setValue(parseDateField(fields.get("Datum rojstva"), dateFormatter));

        cardNumber.setText(fields.getOrDefault("Številka kartice", ""));
        cardHolder.setText(fields.getOrDefault("Imetnik kartice", ""));
        cardSecurityCode.setText(fields.getOrDefault("Varnostna koda", ""));

        restoreParticipants(fields.getOrDefault("OSEBE", ""),
                dateFormatter, viewModel, participantManager);
    }

    private static void restoreTransport(String transportLine,
            ComboBox<String> transportMode,
            TextField customTransportMode,
            Label customTransportModeLabel) {
        if (transportLine.isEmpty()) {
            return;
        }

        String[] modes = transportLine.split("\\s*,\\s*");
        if (modes.length > 0 && !modes[0].isEmpty()) {
            String firstMode = modes[0].trim();
            if (ReservationFormConfigurator.TRANSPORT_MODES.contains(firstMode)) {
                transportMode.getSelectionModel().select(firstMode);
            } else {
                transportMode.getSelectionModel().select("Po izbiri");
                customTransportMode.setText(firstMode);
                ReservationFormConfigurator.hideAndDisable(customTransportMode, false);
                ReservationFormConfigurator.hideAndDisable(customTransportModeLabel, false);
            }
        }
    }

    private static void restoreAccommodationType(String typeValue,
            ToggleGroup accommodationType,
            RadioButton customAccommodationOption,
            TextField customAccommodationType) {
        if (typeValue.isEmpty()) {
            accommodationType.selectToggle(null);
            return;
        }

        for (Toggle toggle : accommodationType.getToggles()) {
            RadioButton radio = (RadioButton) toggle;
            if (radio.getText().equals(typeValue)) {
                accommodationType.selectToggle(toggle);
                if (toggle == customAccommodationOption) {
                    ReservationFormConfigurator.hideAndDisable(customAccommodationType, false);
                }
                return;
            }
        }

        accommodationType.selectToggle(customAccommodationOption);
        customAccommodationType.setText(typeValue);
        ReservationFormConfigurator.hideAndDisable(customAccommodationType, false);
    }

    private static void restoreSpecialRequirements(String requirementsLine,
            Map<CheckBox, String> requirementLabels) {
        requirementLabels.keySet().forEach(checkBox -> checkBox.setSelected(false));

        if (requirementsLine.isEmpty() || requirementsLine.equals("Brez posebnih zahtev")) {
            return;
        }

        String[] parts = requirementsLine.split("\\s*,\\s*");
        for (String part : parts) {
            String trimmed = part.trim();
            for (Map.Entry<CheckBox, String> entry : requirementLabels.entrySet()) {
                if (entry.getValue().equals(trimmed)) {
                    entry.getKey().setSelected(true);
                    break;
                }
            }
        }
    }

    private static void restoreParticipants(String participantBlock,
            DateTimeFormatter dateFormatter,
            ReservationViewModel viewModel,
            ParticipantManager participantManager) {
        participantManager.resetParticipants();
        viewModel.participants.clear();

        if (participantBlock.isEmpty() || participantBlock.equals("Ni vnesenih oseb.")) {
            viewModel.participants.add(new ParticipantViewModel());
        } else {
            String[] lines = participantBlock.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                int dotIdx = trimmed.indexOf('.');
                if (dotIdx < 0) {
                    continue;
                }

                String afterNumber = trimmed.substring(dotIdx + 1).trim();
                int lastDash = afterNumber.lastIndexOf(" - ");
                if (lastDash < 0) {
                    continue;
                }

                String namePart = afterNumber.substring(0, lastDash).trim();
                String datePart = afterNumber.substring(lastDash + 3).trim();

                int lastSpace = namePart.lastIndexOf(' ');
                String participantName = lastSpace > 0 ? namePart.substring(0, lastSpace).trim() : namePart;
                String participantSurname = lastSpace > 0 ? namePart.substring(lastSpace + 1).trim() : "";

                ParticipantViewModel participant = new ParticipantViewModel();
                participant.name.set(participantName);
                participant.surname.set(participantSurname);
                participant.birthDate.set(parseDateField(datePart, dateFormatter));
                viewModel.participants.add(participant);
            }
        }

        participantManager.rebuildParticipantRows();
    }
}
