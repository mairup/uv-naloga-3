package com.uv.naloge.naloga3;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReservationController {

    // --- REGEX CONSTANTS ---
    private static final String NAME_REGEX = "[\\p{L}][\\p{L}\\s'\\-]{1,49}";
    private static final String HOUSE_NUMBER_REGEX = "[1-9]\\d{0,3}\\s?[a-zA-Z]?";
    private static final String CARD_NUMBER_REGEX = "\\d{13,19}";
    private static final String CARD_SECURITY_CODE_REGEX = "\\d{3,4}";

    // --- FXML UI COMPONENTS ---
    @FXML
    private ComboBox<String> destinationCountry;
    @FXML
    private ComboBox<String> destination;
    @FXML
    private TextField accommodationLocation;
    @FXML
    private DatePicker departureDate;
    @FXML
    private DatePicker returnDate;

    @FXML
    private ComboBox<String> transportMode;
    @FXML
    private Label customTransportModeLabel;
    @FXML
    private TextField customTransportMode;

    @FXML
    private ToggleGroup accommodationType;
    @FXML
    private RadioButton roomAccommodation;
    @FXML
    private RadioButton customAccommodationOption;
    @FXML
    private TextField customAccommodationType;

    @FXML
    private CheckBox airConditioning;
    @FXML
    private CheckBox parking;
    @FXML
    private CheckBox internet;
    @FXML
    private CheckBox wifi;
    @FXML
    private CheckBox pool;
    @FXML
    private CheckBox hotWater;
    @FXML
    private CheckBox fridge;
    @FXML
    private CheckBox accessibility;

    @FXML
    private TextField payerName;
    @FXML
    private TextField payerSurname;
    @FXML
    private TextField payerStreet;
    @FXML
    private TextField payerHouseNumber;
    @FXML
    private ComboBox<String> payerCountry;
    @FXML
    private DatePicker payerBirthDate;

    @FXML
    private TextField cardNumber;
    @FXML
    private TextField cardHolder;
    @FXML
    private TextField cardSecurityCode;

    @FXML
    private VBox participantRowsBox;
    @FXML
    private Label participantCount;
    @FXML
    private Label status;
    @FXML
    private Label pageIndicator;
    @FXML
    private Label actionSeparator;

    @FXML
    private MenuItem openItem;
    @FXML
    private MenuItem saveItem;
    @FXML
    private MenuItem resetItem;
    @FXML
    private MenuItem closeItem;
    @FXML
    private MenuItem aboutItem;

    @FXML
    private javafx.scene.layout.VBox page1;
    @FXML
    private javafx.scene.layout.VBox page2;
    @FXML
    private javafx.scene.layout.VBox page3;
    @FXML
    private javafx.scene.layout.VBox page4;
    @FXML
    private javafx.scene.control.Button btnPrev;
    @FXML
    private javafx.scene.control.Button btnNext;
    @FXML
    private javafx.scene.control.Button btnReset;
    @FXML
    private javafx.scene.control.Button btnReserve;
    @FXML
    private javafx.scene.control.Button btnCheck;

    // --- CATALOGS & DATA STRUCTURES ---
    private static final Map<String, List<String>> DESTINATION_CATALOG = new LinkedHashMap<>();
    static {
        DESTINATION_CATALOG.put("Slovenija", List.of("Bled", "Bovec", "Piran", "Moravske Toplice"));
        DESTINATION_CATALOG.put("Hrvaška", List.of("Rovinj", "Split", "Dubrovnik", "Otok Krk"));
        DESTINATION_CATALOG.put("Italija", List.of("Benetke", "Trst", "Rim", "Sardinija"));
        DESTINATION_CATALOG.put("Avstrija", List.of("Dunaj", "Salzburg", "Innsbruck", "Beljak"));
        DESTINATION_CATALOG.put("Španija", List.of("Barcelona", "Madrid", "Malaga", "Mallorca"));
    }

    private static final List<String> TRANSPORT_MODES = List.of(
            "Letalo", "Ladja", "Avtobus", "Kolo", "Vlak", "Po izbiri");

    private static final List<String> PAYER_COUNTRIES = List.of(
            "Slovenija", "Hrvaška", "Italija", "Avstrija", "Španija", "Nemčija", "Francija");

    // --- STATE & FORMATTERS ---
    private int currentPageIndex = 0;
    private final List<Node> pages = new ArrayList<>();
    private final List<Control> invalidControls = new ArrayList<>();
    private final Map<Control, Tooltip> originalTooltips = new HashMap<>();

    private final ReservationViewModel viewModel = new ReservationViewModel();

    private final List<ParticipantRow> participantRows = new ArrayList<>();
    private boolean participantRowsRefreshing = false;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        java.util.stream.Stream.of(page1, page2, page3, page4)
                .filter(Objects::nonNull).forEach(pages::add);
        updatePagination();

        configureChoiceControls();
        configureNumericInputControls();
        configureDatePickers();
        configureMenuActions();
        configureValidationRecovery();
        configureResponsiveButtons();
        bindViewModel();
        resetForm();
        setNeutralStatus("Pripravljen za vnos rezervacije.");
    }

    private void bindViewModel() {
        destinationCountry.valueProperty().bindBidirectional(viewModel.destinationCountry);
        destination.valueProperty().bindBidirectional(viewModel.destination);
        accommodationLocation.textProperty().bindBidirectional(viewModel.accommodationLocation);
        departureDate.valueProperty().bindBidirectional(viewModel.departureDate);
        returnDate.valueProperty().bindBidirectional(viewModel.returnDate);

        payerName.textProperty().bindBidirectional(viewModel.payerName);
        payerSurname.textProperty().bindBidirectional(viewModel.payerSurname);
        payerStreet.textProperty().bindBidirectional(viewModel.payerStreet);
        payerHouseNumber.textProperty().bindBidirectional(viewModel.payerHouseNumber);
        payerCountry.valueProperty().bindBidirectional(viewModel.payerCountry);
        payerBirthDate.valueProperty().bindBidirectional(viewModel.payerBirthDate);

        cardNumber.textProperty().bindBidirectional(viewModel.cardNumber);
        cardHolder.textProperty().bindBidirectional(viewModel.cardHolder);
        cardSecurityCode.textProperty().bindBidirectional(viewModel.cardSecurityCode);

        refreshParticipantRows();
    }

    private void configureResponsiveButtons() {
        Platform.runLater(() -> {
            if (btnNext.getScene() != null) {
                updateResponsiveControls(btnNext.getScene().getWidth());
            }
        });

        btnNext.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateResponsiveControls(newScene.getWidth());
                newScene.widthProperty().addListener((szObs, oldWidth, newWidth) -> {
                    updateResponsiveControls(newWidth.doubleValue());
                });
            }
        });
    }

    private void updateResponsiveControls(double windowWidth) {
        boolean compactNavigation = windowWidth < 520;

        if (compactNavigation) {
            btnPrev.setText("");
            btnNext.setText("");
            btnPrev.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            btnNext.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            if (actionSeparator != null) {
                actionSeparator.setVisible(false);
                actionSeparator.setManaged(false);
            }
        } else {
            btnPrev.setText("Nazaj");
            btnNext.setText("Naprej");
            btnPrev.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btnNext.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            if (actionSeparator != null) {
                actionSeparator.setVisible(true);
                actionSeparator.setManaged(true);
            }
        }
    }

    @FXML
    private void onReserve() {
        populateViewModelOptions();

        if (currentPageIndex < pages.size() - 1) {
            clearValidationMarks();
            List<String> errors = switch (currentPageIndex) {
                case 0 -> validateCurrentPageTravel();
                case 1 -> validateCurrentPageAccommodation();
                case 2 -> validateCurrentPagePayment();
                case 3 -> validateCurrentPagePeople();
                default -> List.of();
            };

            if (errors.isEmpty()) {
                onNextPage();
            } else {
                showValidationErrors(errors, "Vnos ni veljaven.");
            }
            return;
        }

        clearValidationMarks();
        List<String> travelErrors = validateCurrentPageTravel();
        if (!travelErrors.isEmpty()) {
            currentPageIndex = 0;
            updatePagination();
            showValidationErrors(travelErrors, "Vnos ni veljaven.");
            return;
        }

        List<String> accErrors = validateCurrentPageAccommodation();
        if (!accErrors.isEmpty()) {
            currentPageIndex = 1;
            updatePagination();
            showValidationErrors(accErrors, "Vnos ni veljaven.");
            return;
        }

        List<String> payErrors = validateCurrentPagePayment();
        if (!payErrors.isEmpty()) {
            currentPageIndex = 2;
            updatePagination();
            showValidationErrors(payErrors, "Vnos ni veljaven.");
            return;
        }

        List<String> peopleErrors = validateCurrentPagePeople();
        if (!peopleErrors.isEmpty()) {
            currentPageIndex = 3;
            updatePagination();
            showValidationErrors(peopleErrors, "Vnos ni veljaven.");
            return;
        }

        setSuccessStatus("Rezervacija je uspešno potrjena.");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rezervacija potrjena");
        alert.setHeaderText("Vnos je veljaven");
        alert.setContentText("Rezervacija je pripravljena za nadaljnjo obdelavo.");
        alert.showAndWait();
    }

    @FXML
    private void onSave() {
        populateViewModelOptions();
        List<String> validationErrors = validateForm(viewModel);
        if (!validationErrors.isEmpty()) {
            showValidationErrors(validationErrors, "Shranjevanje ni mogoče.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Shrani rezervacijo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Besedilna datoteka (*.txt)", "*.txt"));
        fileChooser
                .setInitialFileName("rezervacija-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt");

        Window window = status.getScene() == null ? null : status.getScene().getWindow();
        File chosenFile = fileChooser.showSaveDialog(window);
        if (chosenFile == null) {
            setNeutralStatus("Shranjevanje je preklicano.");
            return;
        }

        try {
            Files.writeString(chosenFile.toPath(), buildReservationReport(viewModel), StandardCharsets.UTF_8);
            setSuccessStatus("Vnos je shranjen: " + chosenFile.getName());
        } catch (IOException exception) {
            setErrorStatus("Shranjevanje ni uspelo: " + exception.getMessage());
        }
    }

    @FXML
    private void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odpri rezervacijo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Besedilna datoteka (*.txt)", "*.txt"));

        Window window = status.getScene() == null ? null : status.getScene().getWindow();
        File chosenFile = fileChooser.showOpenDialog(window);
        if (chosenFile == null) {
            setNeutralStatus("Odpiranje je preklicano.");
            return;
        }

        try {
            String content = Files.readString(chosenFile.toPath(), StandardCharsets.UTF_8);
            restoreFromText(content);
            setSuccessStatus("Vnos obnovljen iz: " + chosenFile.getName());
        } catch (IllegalArgumentException e) {
            setErrorStatus("Neveljaven format datoteke: " + e.getMessage());
        } catch (IOException e) {
            setErrorStatus("Odpiranje ni uspelo: " + e.getMessage());
        }
    }

    private void restoreFromText(String content) {
        String[] lines = content.split("\\r?\\n");
        Map<String, String> fields = parseReportFields(lines);

        if (!fields.containsKey("REZERVACIJA POČITNIC")) {
            throw new IllegalArgumentException("manjka glava 'REZERVACIJA POČITNIC'");
        }

        setFieldSafely(destinationCountry, fields, "Država");
        refreshDestinationChoices(destinationCountry.getValue());
        setFieldSafely(destination, fields, "Kraj");
        accommodationLocation.setText(fields.getOrDefault("Kraj nastanitve", ""));
        departureDate.setValue(parseDateField(fields.get("Odhod")));
        returnDate.setValue(parseDateField(fields.get("Vrnitev")));

        restoreTransport(fields.getOrDefault("PREVOZ", ""));
        restoreAccommodationType(fields.getOrDefault("Tip nastanitve", ""));
        restoreSpecialRequirements(fields.getOrDefault("Posebne zahteve", ""));

        payerName.setText(fields.getOrDefault("Ime", ""));
        payerSurname.setText(fields.getOrDefault("Priimek", ""));
        payerStreet.setText(fields.getOrDefault("Ulica", ""));
        payerHouseNumber.setText(fields.getOrDefault("Hišna številka", ""));
        setFieldSafely(payerCountry, fields, "Država plačnika");
        payerBirthDate.setValue(parseDateField(fields.get("Datum rojstva plačnika")));

        cardNumber.setText(fields.getOrDefault("Številka kartice", ""));
        cardHolder.setText(fields.getOrDefault("Imetnik kartice", ""));
        cardSecurityCode.setText(fields.getOrDefault("Varnostna koda", ""));

        restoreParticipants(fields.getOrDefault("OSEBE", ""));
    }

    private Map<String, String> parseReportFields(String[] lines) {
        Map<String, String> fields = new HashMap<>();
        String currentSection = null;
        StringBuilder participantBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

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
                    if (key.equals("Številka")) key = "Številka kartice";
                    if (key.equals("Imetnik")) key = "Imetnik kartice";
                    if (key.equals("Varnostna koda")) key = "Varnostna koda";
                }
                fields.put(key, value);
            } else if (currentSection != null && currentSection.equals("PREVOZ")) {
                fields.put("PREVOZ", trimmed);
            }
        }

        fields.put("OSEBE", participantBlock.toString().trim());
        return fields;
    }

    private void setFieldSafely(ComboBox<String> comboBox, Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) return;
        for (String item : comboBox.getItems()) {
            if (item.equals(value)) {
                comboBox.getSelectionModel().select(value);
                return;
            }
        }
    }

    private LocalDate parseDateField(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim(), dateFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    private void restoreTransport(String transportLine) {
        if (transportLine.isEmpty()) return;

        String[] modes = transportLine.split("\\s*,\\s*");
        if (modes.length > 0 && !modes[0].isEmpty()) {
            String firstMode = modes[0].trim();
            boolean foundInCatalog = TRANSPORT_MODES.contains(firstMode);
            if (foundInCatalog) {
                transportMode.getSelectionModel().select(firstMode);
            } else {
                transportMode.getSelectionModel().select("Po izbiri");
                customTransportMode.setText(firstMode);
                hideAndDisable(customTransportMode, false);
                hideAndDisable(customTransportModeLabel, false);
            }
        }
    }

    private void restoreAccommodationType(String typeValue) {
        if (typeValue.isEmpty()) {
            accommodationType.selectToggle(null);
            return;
        }

        for (Toggle toggle : accommodationType.getToggles()) {
            RadioButton radio = (RadioButton) toggle;
            if (radio.getText().equals(typeValue)) {
                accommodationType.selectToggle(toggle);
                if (toggle == customAccommodationOption) {
                    hideAndDisable(customAccommodationType, false);
                }
                return;
            }
        }

        accommodationType.selectToggle(customAccommodationOption);
        customAccommodationType.setText(typeValue);
        hideAndDisable(customAccommodationType, false);
    }

    private void restoreSpecialRequirements(String requirementsLine) {
        airConditioning.setSelected(false);
        parking.setSelected(false);
        internet.setSelected(false);
        wifi.setSelected(false);
        pool.setSelected(false);
        hotWater.setSelected(false);
        fridge.setSelected(false);
        accessibility.setSelected(false);

        if (requirementsLine.isEmpty() || requirementsLine.equals("Brez posebnih zahtev")) return;

        Map<CheckBox, String> checkboxMap = new HashMap<>();
        checkboxMap.put(airConditioning, "Klima");
        checkboxMap.put(parking, "Parkirišče");
        checkboxMap.put(internet, "Internet");
        checkboxMap.put(wifi, "Wi-Fi");
        checkboxMap.put(pool, "Bazen");
        checkboxMap.put(hotWater, "Topla voda");
        checkboxMap.put(fridge, "Hladilnik");
        checkboxMap.put(accessibility, "Prilagojen dostop");

        String[] parts = requirementsLine.split("\\s*,\\s*");
        for (String part : parts) {
            String trimmed = part.trim();
            for (Map.Entry<CheckBox, String> entry : checkboxMap.entrySet()) {
                if (entry.getValue().equals(trimmed)) {
                    entry.getKey().setSelected(true);
                    break;
                }
            }
        }
    }

    private void restoreParticipants(String participantBlock) {
        participantRowsRefreshing = true;
        viewModel.participants.clear();

        if (participantBlock.isEmpty() || participantBlock.equals("Ni vnesenih oseb.")) {
            viewModel.participants.add(new ParticipantViewModel());
            participantRowsRefreshing = false;
            refreshParticipantRows();
            return;
        }

        String[] lines = participantBlock.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            int dotIdx = trimmed.indexOf('.');
            if (dotIdx < 0) continue;

            String afterNumber = trimmed.substring(dotIdx + 1).trim();
            int lastDash = afterNumber.lastIndexOf(" - ");
            if (lastDash < 0) continue;

            String namePart = afterNumber.substring(0, lastDash).trim();
            String datePart = afterNumber.substring(lastDash + 3).trim();

            int lastSpace = namePart.lastIndexOf(' ');
            String pName = lastSpace > 0 ? namePart.substring(0, lastSpace).trim() : namePart;
            String pSurname = lastSpace > 0 ? namePart.substring(lastSpace + 1).trim() : "";

            ParticipantViewModel pvm = new ParticipantViewModel();
            pvm.name.set(pName);
            pvm.surname.set(pSurname);
            pvm.birthDate.set(parseDateField(datePart));
            viewModel.participants.add(pvm);
        }

        viewModel.participants.add(new ParticipantViewModel());
        participantRowsRefreshing = false;
        refreshParticipantRows();
    }

    @FXML
    private void onReset() {
        resetForm();
        setNeutralStatus("Vnosi so ponastavljeni.");
    }

    @FXML
    private void onClose() {
        if (status.getScene() != null) {
            status.getScene().getWindow().hide();
        }
    }

    @FXML
    private void onAbout() {
        new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/mairup/uv-naloga-3"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onPrevPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            updatePagination();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPageIndex < pages.size() - 1) {
            currentPageIndex++;
            updatePagination();
        }
    }

    @FXML
    private void onCheckCurrentPage() {
        clearValidationMarks();
        List<String> errors = switch (currentPageIndex) {
            case 0 -> validateCurrentPageTravel();
            case 1 -> validateCurrentPageAccommodation();
            case 2 -> validateCurrentPagePayment();
            case 3 -> validateCurrentPagePeople();
            default -> List.of("Stran ni na voljo za preverjanje.");
        };

        if (errors.isEmpty()) {
            setSuccessStatus("Trenutni zaslon je veljaven.");
            return;
        }

        setErrorStatus("Popravite označena polja. " + errors.getFirst());
    }

    private void updatePagination() {
        clearValidationMarks();
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setVisible(i == currentPageIndex);
            pages.get(i).setManaged(i == currentPageIndex);
        }

        btnPrev.setDisable(currentPageIndex == 0);
        btnNext.setDisable(currentPageIndex == pages.size() - 1);

        if (currentPageIndex == pages.size() - 1) {
            btnReserve.setText("Rezerviraj");
            try {
                org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon("bi-check2-circle");
                icon.setIconSize(14);
                icon.getStyleClass().add("success-icon");
                btnReserve.setGraphic(icon);
            } catch (Exception e) {
            }
        } else {
            btnReserve.setText("Naprej");
            try {
                org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon("bi-arrow-right");
                icon.setIconSize(14);
                icon.getStyleClass().add("success-icon");
                btnReserve.setGraphic(icon);
            } catch (Exception e) {
            }
        }

        if (pageIndicator != null) {
            pageIndicator.setText((currentPageIndex + 1) + " / " + pages.size());
        }
    }

    private void check(Control control, boolean isInvalid, String errorMessage, List<String> errors) {
        if (isInvalid) {
            markInvalid(control, errorMessage);
            if (errors != null)
                errors.add(errorMessage);
        }
    }

    private List<String> validateCurrentPageTravel() {
        populateViewModelOptions();
        List<String> errors = new ArrayList<>();

        check(destinationCountry, isBlank(viewModel.destinationCountry.get()), "Izberite državo destinacije.", errors);
        check(destination, isBlank(viewModel.destination.get()), "Izberite destinacijo.", errors);
        check(accommodationLocation, isBlank(viewModel.accommodationLocation.get()), "Vpišite kraj nastanitve.",
                errors);

        LocalDate depart = viewModel.departureDate.get();
        LocalDate return_ = viewModel.returnDate.get();

        check(departureDate, depart == null, "Izberite datum odhoda.", errors);
        if (depart != null) {
            check(departureDate, depart.isBefore(LocalDate.now()),
                    "Datum odhoda ne sme biti v preteklosti.", errors);
        }
        check(returnDate, return_ == null, "Izberite datum vrnitve.", errors);
        if (depart != null && return_ != null) {
            check(returnDate, !return_.isAfter(depart),
                    "Datum vrnitve mora biti po datumu odhoda.", errors);
        }
        check(transportMode, viewModel.transportModes.isEmpty(), "Izberite način prevoza.", errors);
        if ("Po izbiri".equals(transportMode.getValue())) {
            check(customTransportMode, isBlank(customTransportMode.getText()),
                    "Vpišite način prevoza po izbiri.", errors);
        }

        return errors;
    }

    private List<String> validateCurrentPageAccommodation() {
        populateViewModelOptions();
        List<String> errors = new ArrayList<>();

        if (isBlank(viewModel.accommodationType.get())) {
            accommodationType.getToggles().stream()
                    .filter(t -> t instanceof RadioButton)
                    .map(t -> (RadioButton) t)
                    .forEach(r -> check(r, true, "Izberite tip nastanitve.", null));
            errors.add("Izberite tip nastanitve.");
        }

        if (accommodationType.getSelectedToggle() == customAccommodationOption) {
            check(customAccommodationType, isBlank(customAccommodationType.getText()),
                    "Vpišite tip nastanitve po izbiri.", errors);
        }

        return errors;
    }

    private List<String> validateCurrentPagePayment() {
        List<String> errors = new ArrayList<>();

        check(payerName, isBlank(viewModel.payerName.get()), "Vpišite ime plačnika.", errors);
        if (!isBlank(viewModel.payerName.get()))
            check(payerName, !isValidPersonName(viewModel.payerName.get()), "Ime plačnika vsebuje neveljavne znake.",
                    errors);

        check(payerSurname, isBlank(viewModel.payerSurname.get()), "Vpišite priimek plačnika.", errors);
        if (!isBlank(viewModel.payerSurname.get()))
            check(payerSurname, !isValidPersonName(viewModel.payerSurname.get()),
                    "Priimek plačnika vsebuje neveljavne znake.", errors);

        check(payerStreet, isBlank(viewModel.payerStreet.get()), "Vpišite ulico plačnika.", errors);

        check(payerHouseNumber, isBlank(viewModel.payerHouseNumber.get()), "Vpišite hišno številko.", errors);
        if (!isBlank(viewModel.payerHouseNumber.get()))
            check(payerHouseNumber, !viewModel.payerHouseNumber.get().matches(HOUSE_NUMBER_REGEX),
                    "Neveljavna hišna številka.", errors);

        check(payerCountry, isBlank(viewModel.payerCountry.get()), "Izberite državo plačnika.", errors);

        LocalDate birth = viewModel.payerBirthDate.get();
        check(payerBirthDate, birth == null, "Izberite datum rojstva plačnika.", errors);
        if (birth != null) {
            check(payerBirthDate, birth.isAfter(LocalDate.now()),
                    "Datum rojstva plačnika ne sme biti v prihodnosti.", errors);
        }

        check(cardNumber, isBlank(viewModel.cardNumber.get()), "Vpišite številko kartice.", errors);
        if (!isBlank(viewModel.cardNumber.get()))
            check(cardNumber, !viewModel.cardNumber.get().matches(CARD_NUMBER_REGEX),
                    "Številka kartice mora imeti od 13 do 19 števk.", errors);

        check(cardHolder, isBlank(viewModel.cardHolder.get()), "Vpišite ime in priimek na kartici.", errors);
        if (!isBlank(viewModel.cardHolder.get()))
            check(cardHolder, !isValidPersonName(viewModel.cardHolder.get()),
                    "Ime na kartici vsebuje neveljavne znake.",
                    errors);

        check(cardSecurityCode, isBlank(viewModel.cardSecurityCode.get()), "Vpišite varnostno kodo kartice.", errors);
        if (!isBlank(viewModel.cardSecurityCode.get()))
            check(cardSecurityCode, !viewModel.cardSecurityCode.get().matches(CARD_SECURITY_CODE_REGEX),
                    "Varnostna koda mora imeti 3 ali 4 števke.", errors);

        return errors;
    }

    private List<String> validateCurrentPagePeople() {
        List<String> errors = new ArrayList<>();

        boolean hasAtLeastOneFilledRow = false;

        for (int i = 0; i < viewModel.participants.size() && i < participantRows.size(); i++) {
            ParticipantViewModel participant = viewModel.participants.get(i);
            ParticipantRow row = participantRows.get(i);

            if (isParticipantEmpty(participant)) {
                continue;
            }

            hasAtLeastOneFilledRow = true;
            int personNumber = i + 1;

            check(row.name, isBlank(participant.name.get()), "Vpišite ime za osebo " + personNumber + ".", errors);
            if (!isBlank(participant.name.get())) {
                check(row.name, !isValidPersonName(participant.name.get()),
                        "Ime osebe " + personNumber + " vsebuje neveljavne znake.", errors);
            }

            check(row.surname, isBlank(participant.surname.get()), "Vpišite priimek za osebo " + personNumber + ".",
                    errors);
            if (!isBlank(participant.surname.get())) {
                check(row.surname, !isValidPersonName(participant.surname.get()),
                        "Priimek osebe " + personNumber + " vsebuje neveljavne znake.", errors);
            }

            String birthDateText = row.birthDate.getEditor() == null ? "" : row.birthDate.getEditor().getText();
            LocalDate birthDate = participant.birthDate.get();
            check(row.birthDate, isBlank(birthDateText) && birthDate == null,
                    "Vpišite datum rojstva za osebo " + personNumber + ".", errors);
            check(row.birthDate, !isBlank(birthDateText) && birthDate == null,
                    "Datum rojstva osebe " + personNumber + " ni v veljavnem formatu.", errors);
        }

        if (!hasAtLeastOneFilledRow) {
            errors.add("Vnesite vsaj eno osebo.");
            if (!participantRows.isEmpty()) {
                markInvalid(participantRows.get(0).name, "Vnesite vsaj eno osebo.");
            }
        }

        return errors;
    }

    private void markInvalid(Control control, String message) {
        if (!control.getStyleClass().contains("field-error")) {
            control.getStyleClass().add("field-error");
        }
        if (!invalidControls.contains(control)) {
            invalidControls.add(control);
            originalTooltips.put(control, control.getTooltip());
        }
        control.setTooltip(new Tooltip(message));
    }

    private void removeValidationMark(Control control) {
        control.getStyleClass().remove("field-error");
        if (originalTooltips.containsKey(control)) {
            control.setTooltip(originalTooltips.get(control));
        }
        invalidControls.remove(control);
        originalTooltips.remove(control);
    }

    private void clearValidationMarks() {
        for (Control control : invalidControls) {
            control.getStyleClass().remove("field-error");
            control.setTooltip(originalTooltips.get(control));
        }
        invalidControls.clear();
        originalTooltips.clear();
    }

    private void configureValidationRecovery() {
        java.util.stream.Stream.of(
                destinationCountry, destination, accommodationLocation, departureDate, returnDate,
                transportMode, customTransportMode, roomAccommodation, customAccommodationOption,
                customAccommodationType, airConditioning, parking, internet, wifi, pool, hotWater,
                fridge, accessibility, payerName, payerSurname, payerStreet, payerHouseNumber,
                payerCountry, payerBirthDate, cardNumber, cardHolder, cardSecurityCode)
                .forEach(this::attachValidationRecovery);
    }

    private void attachValidationRecovery(Control control) {
        if (control == null) {
            return;
        }
        control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                removeValidationMark(control);
            }
        });
    }

    private void configureChoiceControls() {
        destinationCountry.setItems(FXCollections.observableArrayList(DESTINATION_CATALOG.keySet()));
        destinationCountry.valueProperty()
                .addListener((observable, oldValue, newValue) -> refreshDestinationChoices(newValue));

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

    private void configureNumericInputControls() {
        payerHouseNumber.setTextFormatter(
                new TextFormatter<>(
                        change -> change.getControlNewText().matches("\\d{0,4}\\s?[a-zA-Z]?") ? change : null));

        cardNumber.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,19}") ? change : null));
        cardNumber.setPromptText("1234 5678 1234 5678");

        cardSecurityCode.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));
    }

    private void configureDatePickers() {
        javafx.util.StringConverter<LocalDate> converter = new javafx.util.StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };

        departureDate.setConverter(converter);
        returnDate.setConverter(converter);
        payerBirthDate.setConverter(converter);

        javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> disablePastDatesDayCellFactory = new javafx.util.Callback<DatePicker, javafx.scene.control.DateCell>() {
            @Override
            public javafx.scene.control.DateCell call(final DatePicker datePicker) {
                return new javafx.scene.control.DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isBefore(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffc0cb;");
                        }
                    }
                };
            }
        };

        departureDate.setDayCellFactory(disablePastDatesDayCellFactory);
        returnDate.setDayCellFactory(disablePastDatesDayCellFactory);

        javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> disableFutureDatesDayCellFactory = new javafx.util.Callback<DatePicker, javafx.scene.control.DateCell>() {
            @Override
            public javafx.scene.control.DateCell call(final DatePicker datePicker) {
                return new javafx.scene.control.DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isAfter(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #ffc0cb;");
                        }
                    }
                };
            }
        };

        payerBirthDate.setDayCellFactory(disableFutureDatesDayCellFactory);
    }

    private void configureMenuActions() {
        openItem.setOnAction(event -> onOpen());
        saveItem.setOnAction(event -> onSave());
        resetItem.setOnAction(event -> onReset());
        closeItem.setOnAction(event -> onClose());
        aboutItem.setOnAction(event -> onAbout());
    }

    private void resetForm() {
        resetDestinationAndLocation();
        resetDates();
        resetTransportAndAccommodation();
        resetSpecialRequirements();
        resetPayerDetails();
        resetCardDetails();
        resetParticipants();
    }

    private void resetDestinationAndLocation() {
        destinationCountry.getSelectionModel().selectFirst();
        refreshDestinationChoices(destinationCountry.getValue());
        destination.getSelectionModel().selectFirst();
        accommodationLocation.clear();
    }

    private void resetDates() {
        LocalDate defaultDepartureDate = LocalDate.now().plusDays(30);
        departureDate.setValue(defaultDepartureDate);
        returnDate.setValue(defaultDepartureDate.plusDays(7));
    }

    private void hideAndDisable(Node node, boolean hide) {
        if (node == null)
            return;
        node.setVisible(!hide);
        node.setManaged(!hide);
        node.setDisable(hide);
    }

    private void resetTransportAndAccommodation() {
        transportMode.getSelectionModel().select("Avtobus");
        customTransportMode.clear();
        hideAndDisable(customTransportMode, true);
        hideAndDisable(customTransportModeLabel, true);

        if (roomAccommodation != null)
            accommodationType.selectToggle(roomAccommodation);
        customAccommodationType.clear();
        hideAndDisable(customAccommodationType, true);
    }

    private void resetSpecialRequirements() {
        java.util.stream.Stream.of(
                airConditioning, parking, internet, wifi, pool, hotWater, fridge, accessibility)
                .filter(Objects::nonNull).forEach(cb -> cb.setSelected(false));
    }

    private void resetPayerDetails() {
        java.util.stream.Stream.of(payerName, payerSurname, payerStreet, payerHouseNumber)
                .filter(Objects::nonNull).forEach(TextField::clear);
        payerCountry.getSelectionModel().select("Slovenija");
        payerBirthDate.setValue(LocalDate.now().minusYears(18));
    }

    private void resetCardDetails() {
        java.util.stream.Stream.of(cardNumber, cardHolder, cardSecurityCode)
                .filter(Objects::nonNull).forEach(TextField::clear);
    }

    private void resetParticipants() {
        viewModel.participants.clear();
        viewModel.participants.add(new ParticipantViewModel());
        refreshParticipantRows();
    }

    private void refreshDestinationChoices(String country) {
        List<String> destinations = DESTINATION_CATALOG.getOrDefault(country, List.of());
        destination.setItems(FXCollections.observableArrayList(destinations));
        if (!destinations.isEmpty()) {
            destination.getSelectionModel().selectFirst();
        } else {
            destination.getSelectionModel().clearSelection();
        }
    }

    private void refreshParticipantRows() {
        ensureSingleTrailingEmptyParticipantRow();
        rebuildParticipantRows();
    }

    private void rebuildParticipantRows() {
        participantRowsRefreshing = true;
        participantRowsBox.getChildren().clear();
        participantRows.clear();

        for (int i = 0; i < viewModel.participants.size(); i++) {
            int rowIndex = i;
            ParticipantViewModel pVm = viewModel.participants.get(i);
            ParticipantRow row = new ParticipantRow(i + 1, () -> removeParticipantRow(rowIndex));

            row.name.textProperty().bindBidirectional(pVm.name);
            row.surname.textProperty().bindBidirectional(pVm.surname);
            row.birthDate.valueProperty().bindBidirectional(pVm.birthDate);

            row.name.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    onParticipantRowFocused(rowIndex, ParticipantField.NAME);
                }
            });
            row.surname.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    onParticipantRowFocused(rowIndex, ParticipantField.SURNAME);
                }
            });
            row.birthDate.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    onParticipantRowFocused(rowIndex, ParticipantField.BIRTH_DATE);
                }
            });

            row.name.textProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());
            row.surname.textProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());
            row.birthDate.valueProperty().addListener((obs, oldVal, newVal) -> onParticipantRowChanged());

            attachValidationRecovery(row.name);
            attachValidationRecovery(row.surname);
            attachValidationRecovery(row.birthDate);

            participantRows.add(row);
            participantRowsBox.getChildren().add(row.container);
        }

        participantRowsRefreshing = false;
        updateParticipantRowsVisualState();
    }

    private void onParticipantRowChanged() {
        if (participantRowsRefreshing) {
            return;
        }

        int previousSize = viewModel.participants.size();
        ensureSingleTrailingEmptyParticipantRow();

        if (viewModel.participants.size() != previousSize) {
            rebuildParticipantRows();
        } else {
            updateParticipantRowsVisualState();
        }
    }

    private void removeParticipantRow(int index) {
        if (index < 0 || index >= viewModel.participants.size()) {
            return;
        }

        viewModel.participants.remove(index);
        ensureSingleTrailingEmptyParticipantRow();
        rebuildParticipantRows();
    }

    private void onParticipantRowFocused(int rowIndex, ParticipantField focusedField) {
        if (participantRowsRefreshing || rowIndex < 0 || rowIndex >= viewModel.participants.size()) {
            return;
        }

        boolean isLastRow = rowIndex == viewModel.participants.size() - 1;
        if (!isLastRow || !isParticipantEmpty(viewModel.participants.get(rowIndex))) {
            return;
        }

        viewModel.participants.add(new ParticipantViewModel());
        rebuildParticipantRows();

        if (rowIndex < participantRows.size()) {
            ParticipantRow row = participantRows.get(rowIndex);
            Platform.runLater(() -> {
                switch (focusedField) {
                    case NAME -> row.name.requestFocus();
                    case SURNAME -> row.surname.requestFocus();
                    case BIRTH_DATE -> row.birthDate.requestFocus();
                }
            });
        }
    }

    private void ensureSingleTrailingEmptyParticipantRow() {
        if (viewModel.participants.isEmpty()) {
            viewModel.participants.add(new ParticipantViewModel());
            return;
        }

        while (viewModel.participants.size() > 1) {
            int lastIndex = viewModel.participants.size() - 1;
            ParticipantViewModel last = viewModel.participants.get(lastIndex);
            ParticipantViewModel beforeLast = viewModel.participants.get(lastIndex - 1);
            if (isParticipantEmpty(last) && isParticipantEmpty(beforeLast)) {
                viewModel.participants.remove(lastIndex);
            } else {
                break;
            }
        }

        ParticipantViewModel last = viewModel.participants.get(viewModel.participants.size() - 1);
        if (!isParticipantEmpty(last)) {
            viewModel.participants.add(new ParticipantViewModel());
        }
    }

    private void updateParticipantRowsVisualState() {
        int filledRows = filledParticipantCount();
        participantCount.setText("Vnesene osebe: " + filledRows);

        int lastIndex = participantRows.size() - 1;
        for (int i = 0; i < participantRows.size(); i++) {
            ParticipantRow row = participantRows.get(i);
            row.setIndex(i + 1);

            boolean isTrailingEmpty = i == lastIndex && isParticipantEmpty(viewModel.participants.get(i));
            row.setPlaceholder(isTrailingEmpty);
            row.setRemoveVisible(true);
        }
    }

    private int filledParticipantCount() {
        int count = 0;
        for (ParticipantViewModel participant : viewModel.participants) {
            if (!isParticipantEmpty(participant)) {
                count++;
            }
        }
        return count;
    }

    private boolean isParticipantEmpty(ParticipantViewModel participant) {
        return participant == null
                || (isBlank(participant.name.get())
                        && isBlank(participant.surname.get())
                        && participant.birthDate.get() == null);
    }

    private void populateViewModelOptions() {
        viewModel.transportModes.setAll(selectedTransportModes());
        viewModel.accommodationType.set(selectedAccommodationType());
        viewModel.specialRequirements.setAll(selectedSpecialRequirements());
    }

    private List<String> validateForm(ReservationViewModel data) {
        List<String> errors = new ArrayList<>();

        errors.addAll(validateCurrentPageTravel());
        errors.addAll(validateCurrentPageAccommodation());
        errors.addAll(validateCurrentPagePayment());
        errors.addAll(validateCurrentPagePeople());

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidPersonName(String name) {
        return name.trim().matches(NAME_REGEX);
    }

    private void showValidationErrors(List<String> validationErrors, String messagePrefix) {
        setErrorStatus(messagePrefix + " Preverite polja.");

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Napaka vnosa");
        alert.setHeaderText(messagePrefix);
        alert.setContentText(String.join("\n", validationErrors));
        alert.showAndWait();
    }

    private String buildReservationReport(ReservationViewModel data) {
        return """
                REZERVACIJA POČITNIC
                Ustvarjeno: %s

                DESTINACIJA
                Država: %s
                Kraj: %s
                Kraj nastanitve: %s
                Odhod: %s
                Vrnitev: %s

                PREVOZ
                %s

                NASTANITEV IN ZAHTEVE
                Tip nastanitve: %s
                Število oseb: %s
                Posebne zahteve: %s

                PLAČNIK
                Ime: %s
                Priimek: %s
                Ulica: %s
                Hišna številka: %s
                Država: %s
                Datum rojstva: %s

                KARTICA
                Številka: %s
                Imetnik: %s
                Varnostna koda: %s

                OSEBE
                %s""".formatted(
                LocalDateTime.now().format(dateTimeFormatter),
                nonNullText(data.destinationCountry.get()),
                nonNullText(data.destination.get()),
                nonNullText(data.accommodationLocation.get()),
                formatDate(data.departureDate.get()),
                formatDate(data.returnDate.get()),
                String.join(", ", data.transportModes),
                data.accommodationType.get(),
                countFilledParticipants(data.participants),
                String.join(", ", data.specialRequirements),
                nonNullText(data.payerName.get()),
                nonNullText(data.payerSurname.get()),
                nonNullText(data.payerStreet.get()),
                nonNullText(data.payerHouseNumber.get()),
                nonNullText(data.payerCountry.get()),
                formatDate(data.payerBirthDate.get()),
                nonNullText(data.cardNumber.get()),
                nonNullText(data.cardHolder.get()),
                nonNullText(data.cardSecurityCode.get()),
                buildParticipantsList(data.participants));
    }

    private int countFilledParticipants(List<ParticipantViewModel> participants) {
        int filledCount = 0;
        for (ParticipantViewModel participant : participants) {
            if (!isParticipantEmpty(participant)) {
                filledCount++;
            }
        }
        return filledCount;
    }

    private String buildParticipantsList(List<ParticipantViewModel> participants) {
        StringBuilder list = new StringBuilder();
        int listedIndex = 1;
        for (int index = 0; index < participants.size(); index++) {
            ParticipantViewModel p = participants.get(index);
            if (isParticipantEmpty(p)) {
                continue;
            }
            list.append("%d. %s %s - %s%n".formatted(
                    listedIndex,
                    nonNullText(p.name.get()),
                    nonNullText(p.surname.get()),
                    formatDate(p.birthDate.get())));
            listedIndex++;
        }
        if (list.isEmpty()) {
            return "Ni vnesenih oseb.";
        }
        return list.toString().trim();
    }

    private String nonNullText(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(dateFormatter);
    }

    private List<String> selectedTransportModes() {
        List<String> selectedModes = new ArrayList<>();

        if ("Po izbiri".equals(transportMode.getValue())) {
            if (!isBlank(customTransportMode.getText())) {
                selectedModes.add(customTransportMode.getText().trim());
            }
        } else if (!isBlank(transportMode.getValue())) {
            selectedModes.add(transportMode.getValue());
        }

        return selectedModes;
    }

    private String selectedAccommodationType() {
        Toggle selected = accommodationType.getSelectedToggle();
        if (selected == null) {
            return "";
        }

        if (selected == customAccommodationOption) {
            return isBlank(customAccommodationType.getText()) ? "" : customAccommodationType.getText().trim();
        }

        RadioButton selectedRadio = (RadioButton) selected;
        return selectedRadio.getText();
    }

    private List<String> selectedSpecialRequirements() {
        List<String> selectedRequirements = new ArrayList<>();
        Map<CheckBox, String> requirementsMap = Map.of(
                airConditioning, "Klima",
                parking, "Parkirišče",
                internet, "Internet",
                wifi, "Wi-Fi",
                pool, "Bazen",
                hotWater, "Topla voda",
                fridge, "Hladilnik");

        requirementsMap.forEach((checkbox, name) -> {
            if (checkbox != null && checkbox.isSelected())
                selectedRequirements.add(name);
        });

        if (accessibility != null && accessibility.isSelected()) {
            selectedRequirements.add("Prilagojen dostop");
        }

        if (selectedRequirements.isEmpty()) {
            selectedRequirements.add("Brez posebnih zahtev");
        }

        return selectedRequirements;
    }

    private void setStatus(String message, String applyClass, String removeClass) {
        status.getStyleClass().remove(removeClass);
        if (applyClass != null && !status.getStyleClass().contains(applyClass)) {
            status.getStyleClass().add(applyClass);
        }
        status.setText(message);
    }

    private void setNeutralStatus(String message) {
        status.getStyleClass().removeAll("status-success", "status-error");
        status.setText(message);
    }

    private void setSuccessStatus(String message) {
        setStatus(message, "status-success", "status-error");
    }

    private void setErrorStatus(String message) {
        setStatus(message, "status-error", "status-success");
    }

    private static final class ParticipantRow {
        private final HBox container;
        private final Label label;
        private final TextField name;
        private final TextField surname;
        private final DatePicker birthDate;
        private final Button removeButton;

        private ParticipantRow(int index, Runnable onRemove) {
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
            removeButton.setOnAction(event -> onRemove.run());

            javafx.util.StringConverter<LocalDate> converter = new javafx.util.StringConverter<LocalDate>() {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return dateFormatter.format(date);
                    } else {
                        return "";
                    }
                }

                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.isEmpty()) {
                        return LocalDate.parse(string, dateFormatter);
                    } else {
                        return null;
                    }
                }
            };
            birthDate.setConverter(converter);

            javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> disableFutureDatesDayCellFactory = new javafx.util.Callback<DatePicker, javafx.scene.control.DateCell>() {
                @Override
                public javafx.scene.control.DateCell call(final DatePicker datePicker) {
                    return new javafx.scene.control.DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item.isAfter(LocalDate.now())) {
                                setDisable(true);
                                setStyle("-fx-background-color: #ffc0cb;");
                            }
                        }
                    };
                }
            };
            birthDate.setDayCellFactory(disableFutureDatesDayCellFactory);

            HBox.setHgrow(name, Priority.ALWAYS);
            HBox.setHgrow(surname, Priority.ALWAYS);
            HBox.setHgrow(birthDate, Priority.ALWAYS);

            container = new HBox(4, label, name, surname, birthDate, removeButton);
            container.getStyleClass().add("participant-row");

            setIndex(index);
        }

        private void setIndex(int index) {
            label.setText(index + ".");
            label.setMinWidth(30);
            label.setPrefWidth(30);
            label.setMaxWidth(30);
            label.setStyle("-fx-font-size: 14px; -fx-padding: 0 1px 0 0px;");
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        private void setPlaceholder(boolean placeholder) {
            if (placeholder) {
                if (!container.getStyleClass().contains("participant-row-placeholder")) {
                    container.getStyleClass().add("participant-row-placeholder");
                }
            } else {
                container.getStyleClass().remove("participant-row-placeholder");
            }
        }

        private void setRemoveVisible(boolean visible) {
            removeButton.setVisible(visible);
            removeButton.setManaged(visible);
        }
    }

    private enum ParticipantField {
        NAME,
        SURNAME,
        BIRTH_DATE
    }

    public static final class ReservationViewModel {
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

    public static final class ParticipantViewModel {
        public final StringProperty name = new SimpleStringProperty();
        public final StringProperty surname = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    }
}
