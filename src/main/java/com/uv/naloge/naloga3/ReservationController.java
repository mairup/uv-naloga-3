package com.uv.naloge.naloga3;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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
import java.time.Period;
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
    private static final String HOUSE_NUMBER_REGEX = "[1-9]\\d{0,3}";
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
    private Spinner<Integer> childrenCount;
    @FXML
    private Spinner<Integer> teenCount;
    @FXML
    private Spinner<Integer> adultCount;

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
    private MenuItem saveItem;
    @FXML
    private MenuItem resetItem;
    @FXML
    private MenuItem closeItem;
    @FXML
    private MenuItem aboutItem;

    @FXML
    private javafx.scene.control.ScrollPane page1;
    @FXML
    private javafx.scene.layout.VBox page2;
    @FXML
    private javafx.scene.control.ScrollPane page3;
    @FXML
    private javafx.scene.control.ScrollPane page4;
    @FXML
    private javafx.scene.control.Button btnPrev;
    @FXML
    private javafx.scene.control.Button btnNext;
    @FXML
    private javafx.scene.control.Button btnSave;
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
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        java.util.stream.Stream.of(page1, page2, page3, page4)
                .filter(Objects::nonNull).forEach(pages::add);
        updatePagination();

        configureChoiceControls();
        configureCountControls();
        configureNumericInputControls();
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

        childrenCount.getValueFactory().valueProperty().bindBidirectional(viewModel.childrenCount.asObject());
        teenCount.getValueFactory().valueProperty().bindBidirectional(viewModel.teenCount.asObject());
        adultCount.getValueFactory().valueProperty().bindBidirectional(viewModel.adultCount.asObject());

        payerName.textProperty().bindBidirectional(viewModel.payerName);
        payerSurname.textProperty().bindBidirectional(viewModel.payerSurname);
        payerStreet.textProperty().bindBidirectional(viewModel.payerStreet);
        payerHouseNumber.textProperty().bindBidirectional(viewModel.payerHouseNumber);
        payerCountry.valueProperty().bindBidirectional(viewModel.payerCountry);
        payerBirthDate.valueProperty().bindBidirectional(viewModel.payerBirthDate);

        cardNumber.textProperty().bindBidirectional(viewModel.cardNumber);
        cardHolder.textProperty().bindBidirectional(viewModel.cardHolder);
        cardSecurityCode.textProperty().bindBidirectional(viewModel.cardSecurityCode);

        viewModel.participants
                .addListener((javafx.collections.ListChangeListener.Change<? extends ParticipantViewModel> c) -> {
                    while (c.next()) {
                        if (c.wasAdded() || c.wasRemoved()) {
                            participantRowsBox.getChildren().clear();
                            participantRows.clear();
                            for (int i = 0; i < viewModel.participants.size(); i++) {
                                ParticipantViewModel pVm = viewModel.participants.get(i);
                                ParticipantRow row = new ParticipantRow(i + 1);

                                row.name.textProperty().bindBidirectional(pVm.name);
                                row.surname.textProperty().bindBidirectional(pVm.surname);
                                row.birthDate.valueProperty().bindBidirectional(pVm.birthDate);

                                attachValidationRecovery(row.name);
                                attachValidationRecovery(row.surname);
                                attachValidationRecovery(row.birthDate);

                                participantRows.add(row);
                                participantRowsBox.getChildren().add(row.container);
                            }
                        }
                    }
                });
    }

    private void configureResponsiveButtons() {
        btnNext.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW != null) {
                        newW.widthProperty().addListener((szObs, oldWidth, newWidth) -> {
                            if (newWidth.doubleValue() < 550) {
                                btnPrev.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                                btnNext.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                            } else {
                                btnPrev.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                                btnNext.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                            }
                        });
                    }
                });
            }
        });
    }

    @FXML
    private void onReserve() {
        populateViewModelOptions();
        List<String> validationErrors = validateForm(viewModel);
        if (!validationErrors.isEmpty()) {
            showValidationErrors(validationErrors, "Vnos ni veljaven.");
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("O aplikaciji");
        alert.setHeaderText("Rezervacija počitnic");
        alert.setContentText("Aplikacija omogoča urejen vnos, preverjanje, ponastavitev in shranjevanje rezervacije.");
        alert.showAndWait();
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
                    "Hišna številka mora biti pozitivno celo število.", errors);

        check(payerCountry, isBlank(viewModel.payerCountry.get()), "Izberite državo plačnika.", errors);

        LocalDate birth = viewModel.payerBirthDate.get();
        check(payerBirthDate, birth == null, "Izberite datum rojstva plačnika.", errors);
        if (birth != null) {
            check(payerBirthDate, birth.isAfter(LocalDate.now()),
                    "Datum rojstva plačnika ne sme biti v prihodnosti.", errors);
            check(payerBirthDate, Period.between(birth, LocalDate.now()).getYears() < 18,
                    "Plačnik mora biti polnoleten.", errors);
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

        int totalCount = viewModel.childrenCount.get() + viewModel.teenCount.get() + viewModel.adultCount.get();
        if (totalCount <= 0) {
            check(childrenCount, true, "Skupno število oseb mora biti vsaj 1.", null);
            check(teenCount, true, "Skupno število oseb mora biti vsaj 1.", null);
            check(adultCount, true, "Skupno število oseb mora biti vsaj 1.", errors);
        }

        int computedChildren = 0;
        int computedTeenagers = 0;
        int computedAdults = 0;
        LocalDate dep = viewModel.departureDate.get();

        for (int index = 0; index < participantRows.size(); index++) {
            ParticipantRow row = participantRows.get(index);
            ParticipantViewModel pVm = viewModel.participants.get(index);
            int id = index + 1;

            check(row.name, isBlank(pVm.name.get()), "Vpišite ime za osebo " + id + ".", errors);
            if (!isBlank(pVm.name.get()))
                check(row.name, !isValidPersonName(pVm.name.get()),
                        "Ime osebe " + id + " vsebuje neveljavne znake.", errors);

            check(row.surname, isBlank(pVm.surname.get()), "Vpišite priimek za osebo " + id + ".", errors);
            if (!isBlank(pVm.surname.get()))
                check(row.surname, !isValidPersonName(pVm.surname.get()),
                        "Priimek osebe " + id + " vsebuje neveljavne znake.", errors);

            LocalDate birth = pVm.birthDate.get();
            check(row.birthDate, birth == null, "Izberite datum rojstva za osebo " + id + ".",
                    errors);
            if (birth != null) {
                check(row.birthDate, birth.isAfter(LocalDate.now()),
                        "Datum rojstva osebe " + id + " ne sme biti v prihodnosti.", errors);

                if (dep != null && birth.isAfter(dep)) {
                    check(row.birthDate, true, "Datum rojstva osebe " + id + " mora biti pred datumom odhoda.", errors);
                } else if (dep != null) {
                    int ageOnDeparture = Period.between(birth, dep).getYears();
                    if (ageOnDeparture <= 7) {
                        computedChildren++;
                    } else if (ageOnDeparture <= 18) {
                        computedTeenagers++;
                    } else {
                        computedAdults++;
                    }
                }
            }
        }

        if (dep != null && !viewModel.participants.isEmpty()) {
            check(childrenCount, computedChildren != viewModel.childrenCount.get(),
                    "Število oseb do 7 let ni usklajeno z rojstnimi datumi.", errors);
            check(teenCount, computedTeenagers != viewModel.teenCount.get(),
                    "Število oseb od 8 do 18 let ni usklajeno z rojstnimi datumi.", errors);
            check(adultCount, computedAdults != viewModel.adultCount.get(),
                    "Število odraslih oseb ni usklajeno z rojstnimi datumi.", errors);
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
                payerCountry, payerBirthDate, cardNumber, cardHolder, cardSecurityCode,
                childrenCount, teenCount, adultCount).forEach(this::attachValidationRecovery);
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

    private void configureCountControls() {
        childrenCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0));
        teenCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0));
        adultCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 1));

        childrenCount.setEditable(true);
        teenCount.setEditable(true);
        adultCount.setEditable(true);

        ChangeListener<Integer> countListener = (observable, oldValue, newValue) -> refreshParticipantRows();
        childrenCount.valueProperty().addListener(countListener);
        teenCount.valueProperty().addListener(countListener);
        adultCount.valueProperty().addListener(countListener);
    }

    private void configureNumericInputControls() {
        payerHouseNumber.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));

        cardNumber.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,19}") ? change : null));

        cardSecurityCode.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));
    }

    private void configureMenuActions() {
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

        childrenCount.getValueFactory().setValue(0);
        teenCount.getValueFactory().setValue(0);
        adultCount.getValueFactory().setValue(1);
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
        payerBirthDate.setValue(null);
    }

    private void resetCardDetails() {
        java.util.stream.Stream.of(cardNumber, cardHolder, cardSecurityCode)
                .filter(Objects::nonNull).forEach(TextField::clear);
    }

    private void resetParticipants() {
        refreshParticipantRows();
        viewModel.participants.forEach(p -> {
            p.name.set("");
            p.surname.set("");
            p.birthDate.set(null);
        });
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
        int targetCount = viewModel.childrenCount.get() + viewModel.teenCount.get() + viewModel.adultCount.get();

        while (viewModel.participants.size() < targetCount) {
            viewModel.participants.add(new ParticipantViewModel());
        }

        while (viewModel.participants.size() > targetCount) {
            viewModel.participants.remove(viewModel.participants.size() - 1);
        }

        participantCount.setText("Število oseb v podrobnostih: " + targetCount);
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
                Do 7 let: %s
                Od 8 do 18 let: %s
                Odrasli: %s
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
                data.childrenCount.get(),
                data.teenCount.get(),
                data.adultCount.get(),
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

    private String buildParticipantsList(List<ParticipantViewModel> participants) {
        StringBuilder list = new StringBuilder();
        for (int index = 0; index < participants.size(); index++) {
            ParticipantViewModel p = participants.get(index);
            list.append("%d. %s %s - %s%n".formatted(
                    index + 1,
                    nonNullText(p.name.get()),
                    nonNullText(p.surname.get()),
                    formatDate(p.birthDate.get())));
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

        private ParticipantRow(int index) {
            label = new Label();
            name = new TextField();
            surname = new TextField();
            birthDate = new DatePicker();

            name.setPromptText("Ime");
            surname.setPromptText("Priimek");
            birthDate.setPromptText("Datum rojstva");

            HBox.setHgrow(name, Priority.ALWAYS);
            HBox.setHgrow(surname, Priority.ALWAYS);
            HBox.setHgrow(birthDate, Priority.ALWAYS);

            container = new HBox(10, label, name, surname, birthDate);
            container.getStyleClass().add("participant-row");

            setIndex(index);
        }

        private void setIndex(int index) {
            label.setText(index + ". oseba");
            label.setMinWidth(72);
        }
    }

    public static final class ReservationViewModel {
        public final StringProperty destinationCountry = new SimpleStringProperty();
        public final StringProperty destination = new SimpleStringProperty();
        public final StringProperty accommodationLocation = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> departureDate = new SimpleObjectProperty<>();
        public final ObjectProperty<LocalDate> returnDate = new SimpleObjectProperty<>();
        public final ObservableList<String> transportModes = FXCollections.observableArrayList();
        public final StringProperty accommodationType = new SimpleStringProperty();
        public final IntegerProperty childrenCount = new SimpleIntegerProperty();
        public final IntegerProperty teenCount = new SimpleIntegerProperty();
        public final IntegerProperty adultCount = new SimpleIntegerProperty();
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
