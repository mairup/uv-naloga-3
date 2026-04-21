package com.uv.naloge.naloga3;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReservationController {
    @FXML
    private ComboBox<String> destinationCountry, destination, transportMode, payerCountry;
    @FXML
    private TextField accommodationLocation, customTransportMode, customAccommodationType,
            payerName, payerSurname, payerStreet, payerHouseNumber,
            cardNumber, cardHolder, cardSecurityCode;
    @FXML
    private DatePicker departureDate, returnDate, payerBirthDate;
    @FXML
    private ToggleGroup accommodationType;
    @FXML
    private RadioButton roomAccommodation, customAccommodationOption;
    @FXML
    private CheckBox airConditioning, parking, internet, wifi, pool, hotWater, fridge, accessibility;
    @FXML
    private VBox participantRowsBox, page1, page2, page3, page4, pageStack;
    @FXML
    private Label customTransportModeLabel, participantCount, pageIndicator;
    @FXML
    private TextFlow status;
    @FXML
    private MenuItem openItem, saveItem, resetItem, closeItem, aboutItem;
    @FXML
    private Button btnPrev, btnNext, btnReset, btnReserve, btnCheck;
    @FXML
    private HBox navHighlight, actionBar, toolbar;
    private final int[] currentPageIndex = { 0 };
    private final List<Node> pages = new ArrayList<>();
    private final List<String> screenNames = List.of("Potovanje", "Nastanitev", "Podatki plačnika", "Osebe");
    private final ReservationViewModel viewModel = new ReservationViewModel();
    private final Map<CheckBox, String> requirementLabels = new LinkedHashMap<>();
    private final ReservationValidator validator = new ReservationValidator();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final ReservationReportBuilder reportBuilder = new ReservationReportBuilder(dateFormatter,
            dateTimeFormatter);
    private final ReservationFileIO fileIO = new ReservationFileIO();
    private ParticipantManager participantManager;
    private ReservationViewModel confirmedViewModel;
    private Node successScreen;

    private boolean isShowingSuccess() {
        return successScreen != null && successScreen.isVisible();
    }

    private int currentPage() {
        return currentPageIndex[0];
    }

    private void setCurrentPage(int index) {
        currentPageIndex[0] = index;
    }

    @FXML
    public void initialize() {
        java.util.stream.Stream.of(page1, page2, page3, page4)
                .filter(Objects::nonNull).forEach(pages::add);
        initializeRequirementLabels();
        participantManager = new ParticipantManager(participantRowsBox, viewModel, validator, participantCount);
        updatePagination();
        ReservationFormConfigurator.configureChoiceControls(
                destinationCountry, destination, transportMode,
                customTransportMode, customTransportModeLabel,
                accommodationType, customAccommodationOption,
                customAccommodationType, payerCountry,
                this::onDestinationCountryChanged);
        ReservationFormConfigurator.configureNumericInputControls(payerHouseNumber, cardNumber, cardSecurityCode);
        ReservationFormConfigurator.configureDatePickers(departureDate, returnDate, payerBirthDate);
        ReservationFormConfigurator.configureMenuActions(
                openItem, saveItem, resetItem, closeItem, aboutItem,
                this::onOpen, this::onSave, this::onReset, this::onClose, this::onAbout);
        ReservationFormConfigurator.configureResponsiveButtons(btnPrev, btnNext);
        validator.configureValidationRecovery(
                destinationCountry, destination, accommodationLocation, departureDate, returnDate,
                transportMode, customTransportMode, roomAccommodation, customAccommodationOption,
                customAccommodationType, airConditioning, parking, internet, wifi, pool, hotWater,
                fridge, accessibility, payerName, payerSurname, payerStreet, payerHouseNumber,
                payerCountry, payerBirthDate, cardNumber, cardHolder, cardSecurityCode);
        bindViewModel();
        resetForm();
        setNeutralStatus("Pripravljen za vnos rezervacije.");
    }

    private void initializeRequirementLabels() {
        requirementLabels.put(airConditioning, "Klima");
        requirementLabels.put(parking, "Parkirišče");
        requirementLabels.put(internet, "Internet");
        requirementLabels.put(wifi, "Wi-Fi");
        requirementLabels.put(pool, "Bazen");
        requirementLabels.put(hotWater, "Topla voda");
        requirementLabels.put(fridge, "Hladilnik");
        requirementLabels.put(accessibility, "Prilagojen dostop");
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
        participantManager.rebuildParticipantRows();
    }

    @FXML
    private void onReserve() {
        if (isShowingSuccess())
            return;
        populateViewModelOptions();
        if (currentPage() < pages.size() - 1) {
            validator.clearValidationMarks();
            List<String> errors = switch (currentPage()) {
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
        validator.clearValidationMarks();
        List<String> travelErrors = validateCurrentPageTravel();
        List<String> accErrors = validateCurrentPageAccommodation();
        List<String> payErrors = validateCurrentPagePayment();
        List<String> peopleErrors = validateCurrentPagePeople();
        List<String> allErrors = new ArrayList<>();
        allErrors.addAll(travelErrors);
        allErrors.addAll(accErrors);
        allErrors.addAll(payErrors);
        allErrors.addAll(peopleErrors);
        if (!allErrors.isEmpty()) {
            if (!travelErrors.isEmpty())
                setCurrentPage(0);
            else if (!accErrors.isEmpty())
                setCurrentPage(1);
            else if (!payErrors.isEmpty())
                setCurrentPage(2);
            else
                setCurrentPage(3);
            updatePagination();
            showValidationErrors(allErrors, "Vnos ni veljaven.");
            return;
        }
        confirmedViewModel = viewModel;
        showSuccessScreen();
    }

    @FXML
    private void onSave() {
        if (isShowingSuccess()) {
            onSuccessSave();
            return;
        }
        List<String> validationErrors = validateForm();
        if (!validationErrors.isEmpty()) {
            showValidationErrors(validationErrors, "Shranjevanje ni mogoče.");
            return;
        }
        saveReportToFile(reportBuilder.buildReservationReport(viewModel));
    }

    @FXML
    private void onOpen() {
        if (isShowingSuccess())
            onFinish();
        String content = fileIO.openReport(currentWindow(), this::setNeutralStatus, this::setErrorStatus);
        if (content == null) {
            return;
        }
        try {
            restoreFromText(content);
            setSuccessStatus("Vnos obnovljen iz datoteke.");
        } catch (IllegalArgumentException e) {
            setErrorStatus("Neveljaven format datoteke: " + e.getMessage());
        }
    }

    @FXML
    private void onReset() {
        if (isShowingSuccess()) {
            onFinish();
            return;
        }
        resetForm();
        setNeutralStatus("Vnosi so ponastavljeni.");
    }

    @FXML
    private void onClose() {
        if (status.getScene() != null)
            status.getScene().getWindow().hide();
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
        if (isShowingSuccess())
            return;
        if (currentPage() > 0) {
            setCurrentPage(currentPage() - 1);
            validator.clearValidationMarks();
            updatePagination();
        }
    }

    @FXML
    private void onNextPage() {
        if (isShowingSuccess())
            return;
        if (currentPage() < pages.size() - 1) {
            setCurrentPage(currentPage() + 1);
            validator.clearValidationMarks();
            updatePagination();
        }
    }

    @FXML
    private void onCheckCurrentPage() {
        if (isShowingSuccess())
            return;
        populateViewModelOptions();
        validator.clearValidationMarks();
        List<String> errors = switch (currentPage()) {
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

    private void onDestinationCountryChanged() {
        ReservationFormConfigurator.refreshDestinationChoices(destination, destinationCountry.getValue());
    }

    private void updatePagination() {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setVisible(i == currentPage());
            pages.get(i).setManaged(i == currentPage());
        }
        setNeutralStatus("Odprta stran: " + screenNames.get(currentPage()));
        btnPrev.setDisable(currentPage() == 0);
        btnNext.setDisable(currentPage() == pages.size() - 1);
        if (currentPage() == pages.size() - 1) {
            btnReserve.setText("Rezerviraj");
            setButtonIcon(btnReserve, "bi-check2-circle", "success-icon");
        } else {
            btnReserve.setText("Naprej");
            setButtonIcon(btnReserve, "bi-arrow-right", "success-icon");
        }
        if (pageIndicator != null) {
            pageIndicator.setText((currentPage() + 1) + " / " + pages.size());
        }
    }

    private void setButtonIcon(Button button, String iconLiteral, String iconStyle) {
        try {
            org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(iconLiteral);
            icon.setIconSize(14);
            icon.getStyleClass().add(iconStyle);
            button.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("Napaka pri nalaganju ikone gumba: " + e.getMessage());
        }
    }

    private List<String> validateCurrentPageTravel() {
        return validator.validatePageTravel(viewModel,
                destinationCountry, destination, accommodationLocation,
                departureDate, returnDate, transportMode,
                transportMode.getValue(), customTransportMode, customTransportMode.getText());
    }

    private List<String> validateCurrentPageAccommodation() {
        return validator.validatePageAccommodation(viewModel,
                accommodationType, customAccommodationOption,
                customAccommodationType, customAccommodationType.getText());
    }

    private List<String> validateCurrentPagePayment() {
        return validator.validatePagePayment(viewModel,
                payerName, payerSurname, payerStreet, payerHouseNumber,
                payerCountry, payerBirthDate, cardNumber, cardHolder, cardSecurityCode);
    }

    private List<String> validateCurrentPagePeople() {
        return validator.validatePagePeople(viewModel, participantManager.getParticipantRows());
    }

    private List<String> validateForm() {
        populateViewModelOptions();
        return validator.validateAll(viewModel,
                destinationCountry, destination, accommodationLocation,
                departureDate, returnDate, transportMode,
                transportMode.getValue(), customTransportMode, customTransportMode.getText(),
                accommodationType, customAccommodationOption,
                customAccommodationType, customAccommodationType.getText(),
                payerName, payerSurname, payerStreet, payerHouseNumber,
                payerCountry, payerBirthDate, cardNumber, cardHolder, cardSecurityCode,
                participantManager.getParticipantRows());
    }

    private void showValidationErrors(List<String> errors, String prefix) {
        setErrorStatus(prefix + " " + errors.getFirst());
    }

    private void populateViewModelOptions() {
        viewModel.transportModes
                .setAll(ReservationFormConfigurator.selectedTransportModes(transportMode, customTransportMode));
        viewModel.accommodationType.set(ReservationFormConfigurator.selectedAccommodationType(
                accommodationType, customAccommodationOption, customAccommodationType));
        viewModel.specialRequirements
                .setAll(ReservationFormConfigurator.selectedSpecialRequirements(requirementLabels));
    }

    private void showSuccessScreen() {
        for (Node page : pages) {
            page.setVisible(false);
            page.setManaged(false);
        }
        if (successScreen == null) {
            successScreen = buildSuccessScreen();
            pageStack.getChildren().add(successScreen);
        }
        refreshSuccessScreen();
        successScreen.setVisible(true);
        successScreen.setManaged(true);
        toolbar.setVisible(false);
        toolbar.setManaged(false);
        navHighlight.setVisible(false);
        navHighlight.setManaged(false);
        actionBar.getChildren().clear();
        Button btnSave = new Button("Shrani");
        btnSave.getStyleClass().addAll("success-button");
        btnSave.setOnAction(e -> onSuccessSave());
        org.kordamp.ikonli.javafx.FontIcon saveIcon = new org.kordamp.ikonli.javafx.FontIcon("bi-download");
        saveIcon.setIconSize(14);
        saveIcon.getStyleClass().add("success-icon");
        btnSave.setGraphic(saveIcon);
        Button btnFinish = new Button("Nova rezervacija");
        btnFinish.getStyleClass().addAll("primary-button");
        btnFinish.setOnAction(e -> onFinish());
        org.kordamp.ikonli.javafx.FontIcon finishIcon = new org.kordamp.ikonli.javafx.FontIcon(
                "bi-arrow-counterclockwise");
        finishIcon.setIconSize(14);
        finishIcon.getStyleClass().add("button-icon");
        btnFinish.setGraphic(finishIcon);
        actionBar.getChildren().addAll(btnSave, btnFinish);
        pageIndicator.setText("✓");
        setSuccessStatus("Rezervacija je uspešno potrjena.");
    }

    private VBox buildSuccessScreen() {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().addAll("form-scroll");
        wrapper.setSpacing(0);
        VBox content = new VBox();
        content.getStyleClass().addAll("content-pane");
        content.setSpacing(12);
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setSpacing(12);
        Label title = new Label("Rezervacija potrjena");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Pregled rezervacije:");
        subtitle.getStyleClass().add("report-subtitle");
        Label report = new Label();
        report.getStyleClass().add("reservation-report");
        report.setWrapText(true);
        report.setPrefWidth(Double.MAX_VALUE);
        report.setText(reportBuilder.buildReservationReport(confirmedViewModel));
        VBox cardInner = new VBox(title, subtitle, new Label(""), report);
        card.getChildren().add(cardInner);
        card.setPadding(new javafx.geometry.Insets(14, 14, 14, 14));
        content.setPadding(new javafx.geometry.Insets(14, 0, 16, 0));
        content.getChildren().add(card);
        wrapper.getChildren().add(content);
        return wrapper;
    }

    private void refreshSuccessScreen() {
        if (successScreen == null)
            return;
        String reportText = reportBuilder.buildReservationReport(confirmedViewModel);
        Label reportLabel = (Label) successScreen.lookup(".reservation-report");
        reportLabel.setUserData(reportText);
        reportLabel.setText(reportText);
    }

    private void onSuccessSave() {
        saveReportToFile(reportBuilder.buildReservationReport(confirmedViewModel));
    }

    private void onFinish() {
        confirmedViewModel = null;
        successScreen.setVisible(false);
        successScreen.setManaged(false);
        toolbar.setVisible(true);
        toolbar.setManaged(true);
        navHighlight.setVisible(true);
        navHighlight.setManaged(true);
        setCurrentPage(0);
        validator.clearValidationMarks();
        updatePagination();
        restoreActionBar();
        resetForm();
        setNeutralStatus("Pripravljen za vnos rezervacije.");
    }

    private void restoreActionBar() {
        actionBar.getChildren().clear();
        Label separator = new Label("•");
        separator.getStyleClass().add("action-separator");
        actionBar.getChildren().addAll(btnReset, separator, btnCheck, btnReserve);
    }

    private void saveReportToFile(String reportText) {
        fileIO.saveReport(currentWindow(), reportText, this::setNeutralStatus, this::setSuccessStatus,
                this::setErrorStatus);
    }

    private void restoreFromText(String content) {
        fileIO.restoreFromText(content,
                dateFormatter,
                viewModel,
                participantManager,
                requirementLabels,
                destinationCountry,
                this::onDestinationCountryChanged,
                destination,
                accommodationLocation,
                departureDate,
                returnDate,
                transportMode,
                customTransportMode,
                customTransportModeLabel,
                accommodationType,
                customAccommodationOption,
                customAccommodationType,
                payerName,
                payerSurname,
                payerStreet,
                payerHouseNumber,
                payerCountry,
                payerBirthDate,
                cardNumber,
                cardHolder,
                cardSecurityCode);
    }

    private Window currentWindow() {
        return status.getScene() == null ? null : status.getScene().getWindow();
    }

    private void resetForm() {
        ReservationFormConfigurator.resetDestinationAndLocation(
                destinationCountry, destination, accommodationLocation, this::onDestinationCountryChanged);
        ReservationFormConfigurator.resetDates(departureDate, returnDate);
        ReservationFormConfigurator.resetTransportAndAccommodation(
                transportMode, customTransportMode, customTransportModeLabel,
                accommodationType, roomAccommodation, customAccommodationType);
        resetSpecialRequirements();
        ReservationFormConfigurator.resetPayerDetails(
                payerName, payerSurname, payerStreet, payerHouseNumber, payerCountry, payerBirthDate);
        ReservationFormConfigurator.resetCardDetails(cardNumber, cardHolder, cardSecurityCode);
        participantManager.resetParticipants();
    }

    private void resetSpecialRequirements() {
        requirementLabels.keySet().forEach(cb -> cb.setSelected(false));
    }

    private void setNeutralStatus(String message) {
        status.getChildren().setAll(new Text(message));
    }

    private void setSuccessStatus(String message) {
        Text text = new Text(message);
        text.getStyleClass().add("status-success");
        status.getChildren().setAll(text);
    }

    private void setErrorStatus(String message) {
        Text prefix = new Text("Napaka: ");
        prefix.getStyleClass().add("status-error-prefix");
        Text detail = new Text(message);
        detail.getStyleClass().add("status-error-detail");
        status.getChildren().setAll(prefix, detail);
    }
}
