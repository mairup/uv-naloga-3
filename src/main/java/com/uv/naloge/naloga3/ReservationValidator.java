package com.uv.naloge.naloga3;

import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleGroup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class ReservationValidator {

    static final String NAME_REGEX = "[\\p{L}][\\p{L}\\s'\\-]{1,49}";
    static final String HOUSE_NUMBER_REGEX = "[1-9]\\d{0,3}\\s?[a-zA-Z]?";
    static final String CARD_NUMBER_REGEX = "\\d{13,19}";
    static final String CARD_SECURITY_CODE_REGEX = "\\d{3,4}";

    private final List<Control> invalidControls = new ArrayList<>();
    private final Map<Control, Tooltip> originalTooltips = new HashMap<>();

    List<Control> getInvalidControls() {
        return invalidControls;
    }

    List<String> validatePageTravel(ReservationViewModel viewModel,
            Control destinationCountryControl, Control destinationControl,
            Control accommodationLocationControl, DatePicker departureDateControl,
            DatePicker returnDateControl, Control transportModeControl,
            String transportModeValue, Control customTransportModeControl,
            String customTransportModeText) {

        List<String> errors = new ArrayList<>();

        validateField(destinationCountryControl, isBlank(viewModel.destinationCountry.get()),
                "Izberite državo destinacije.", errors);
        validateField(destinationControl, isBlank(viewModel.destination.get()),
                "Izberite destinacijo.", errors);
        validateField(accommodationLocationControl, isBlank(viewModel.accommodationLocation.get()),
                "Vpišite kraj nastanitve.", errors);

        LocalDate depart = viewModel.departureDate.get();
        LocalDate return_ = viewModel.returnDate.get();

        validateField(departureDateControl, depart == null, "Izberite datum odhoda.", errors);
        if (depart != null) {
            validateField(departureDateControl, depart.isBefore(LocalDate.now()),
                    "Datum odhoda ne sme biti v preteklosti.", errors);
        }
        validateField(returnDateControl, return_ == null, "Izberite datum vrnitve.", errors);
        if (depart != null && return_ != null) {
            validateField(returnDateControl, !return_.isAfter(depart),
                    "Datum vrnitve mora biti po datumu odhoda.", errors);
        }
        validateField(transportModeControl, viewModel.transportModes.isEmpty(),
                "Izberite način prevoza.", errors);
        if ("Po izbiri".equals(transportModeValue)) {
            validateField(customTransportModeControl, isBlank(customTransportModeText),
                    "Vpišite način prevoza po izbiri.", errors);
        }

        return errors;
    }

    List<String> validatePageAccommodation(ReservationViewModel viewModel,
            ToggleGroup accommodationType, RadioButton customAccommodationOption,
            Control customAccommodationTypeControl, String customAccommodationTypeText) {

        List<String> errors = new ArrayList<>();

        if (isBlank(viewModel.accommodationType.get())) {
            accommodationType.getToggles().stream()
                    .filter(t -> t instanceof RadioButton)
                    .map(t -> (RadioButton) t)
                    .forEach(r -> validateField(r, true, "Izberite tip nastanitve.", null));
            errors.add("Izberite tip nastanitve.");
        }

        if (accommodationType.getSelectedToggle() == customAccommodationOption) {
            validateField(customAccommodationTypeControl, isBlank(customAccommodationTypeText),
                    "Vpišite tip nastanitve po izbiri.", errors);
        }

        return errors;
    }

    List<String> validatePagePayment(ReservationViewModel viewModel,
            Control payerNameControl, Control payerSurnameControl,
            Control payerStreetControl, Control payerHouseNumberControl,
            Control payerCountryControl, DatePicker payerBirthDateControl,
            Control cardNumberControl, Control cardHolderControl,
            Control cardSecurityCodeControl) {

        List<String> errors = new ArrayList<>();
        String payerNameValue = viewModel.payerName.get();
        String payerSurnameValue = viewModel.payerSurname.get();
        String payerStreetValue = viewModel.payerStreet.get();
        String payerHouseNumberValue = viewModel.payerHouseNumber.get();
        String payerCountryValue = viewModel.payerCountry.get();
        String cardNumberValue = viewModel.cardNumber.get();
        String cardHolderValue = viewModel.cardHolder.get();
        String cardSecurityCodeValue = viewModel.cardSecurityCode.get();

        requireNonBlank(payerNameControl, payerNameValue, "Vpišite ime plačnika.", errors);
        requireFormat(payerNameControl, payerNameValue,
                isBlank(payerNameValue) || isValidPersonName(payerNameValue),
                "Ime plačnika vsebuje neveljavne znake.", errors);

        requireNonBlank(payerSurnameControl, payerSurnameValue, "Vpišite priimek plačnika.", errors);
        requireFormat(payerSurnameControl, payerSurnameValue,
                isBlank(payerSurnameValue) || isValidPersonName(payerSurnameValue),
                "Priimek plačnika vsebuje neveljavne znake.", errors);

        requireNonBlank(payerStreetControl, payerStreetValue, "Vpišite ulico plačnika.", errors);

        requireNonBlank(payerHouseNumberControl, payerHouseNumberValue, "Vpišite hišno številko.", errors);
        requireFormat(payerHouseNumberControl, payerHouseNumberValue,
                isBlank(payerHouseNumberValue) || payerHouseNumberValue.matches(HOUSE_NUMBER_REGEX),
                "Neveljavna hišna številka.", errors);

        requireNonBlank(payerCountryControl, payerCountryValue, "Izberite državo plačnika.", errors);

        LocalDate birth = viewModel.payerBirthDate.get();
        validateField(payerBirthDateControl, birth == null, "Izberite datum rojstva plačnika.", errors);
        if (birth != null) {
            boolean isInFuture = birth.isAfter(LocalDate.now());
            validateField(payerBirthDateControl, isInFuture,
                    "Datum rojstva plačnika ne sme biti v prihodnosti.", errors);
            validateField(payerBirthDateControl, !isInFuture && birth.isAfter(LocalDate.now().minusYears(18)),
                    "Plačnik mora biti star vsaj 18 let.", errors);
        }

        requireNonBlank(cardNumberControl, cardNumberValue, "Vpišite številko kartice.", errors);
        requireFormat(cardNumberControl, cardNumberValue,
                isBlank(cardNumberValue) || cardNumberValue.matches(CARD_NUMBER_REGEX),
                "Številka kartice mora imeti od 13 do 19 števk.", errors);

        requireNonBlank(cardHolderControl, cardHolderValue, "Vpišite ime in priimek na kartici.", errors);
        requireFormat(cardHolderControl, cardHolderValue,
                isBlank(cardHolderValue) || isValidPersonName(cardHolderValue),
                "Ime na kartici vsebuje neveljavne znake.", errors);

        requireNonBlank(cardSecurityCodeControl, cardSecurityCodeValue, "Vpišite varnostno kodo kartice.", errors);
        requireFormat(cardSecurityCodeControl, cardSecurityCodeValue,
                isBlank(cardSecurityCodeValue) || cardSecurityCodeValue.matches(CARD_SECURITY_CODE_REGEX),
                "Varnostna koda mora imeti 3 ali 4 števke.", errors);

        return errors;
    }

    List<String> validatePagePeople(ReservationViewModel viewModel,
            List<ParticipantRow> participantRows) {

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

            validateField(row.name, isBlank(participant.name.get()),
                    "Vpišite ime za osebo " + personNumber + ".", errors);
            if (!isBlank(participant.name.get())) {
                validateField(row.name, !isValidPersonName(participant.name.get()),
                        "Ime osebe " + personNumber + " vsebuje neveljavne znake.", errors);
            }

            validateField(row.surname, isBlank(participant.surname.get()),
                    "Vpišite priimek za osebo " + personNumber + ".", errors);
            if (!isBlank(participant.surname.get())) {
                validateField(row.surname, !isValidPersonName(participant.surname.get()),
                        "Priimek osebe " + personNumber + " vsebuje neveljavne znake.", errors);
            }

            String birthDateText = row.birthDate.getEditor() == null ? "" : row.birthDate.getEditor().getText();
            LocalDate birthDate = participant.birthDate.get();
            validateField(row.birthDate, isBlank(birthDateText) && birthDate == null,
                    "Vpišite datum rojstva za osebo " + personNumber + ".", errors);
            validateField(row.birthDate, !isBlank(birthDateText) && birthDate == null,
                    "Datum rojstva osebe " + personNumber + " ni v veljavnem formatu.", errors);
        }

        if (!hasAtLeastOneFilledRow) {
            errors.add("Vnesite vsaj eno osebo.");
            for (ParticipantRow row : participantRows) {
                markInvalid(row.name, "Vnesite vsaj eno osebo.");
                markInvalid(row.surname, "Vnesite vsaj eno osebo.");
                String bdText = row.birthDate.getEditor() == null ? "" : row.birthDate.getEditor().getText();
                if (isBlank(bdText)) {
                    markInvalid(row.birthDate, "Vnesite vsaj eno osebo.");
                }
            }
        }

        return errors;
    }

    List<String> validateAll(ReservationViewModel viewModel,
            Control destinationCountryControl, Control destinationControl,
            Control accommodationLocationControl, DatePicker departureDateControl,
            DatePicker returnDateControl, Control transportModeControl,
            String transportModeValue, Control customTransportModeControl,
            String customTransportModeText, ToggleGroup accommodationType,
            RadioButton customAccommodationOption, Control customAccommodationTypeControl,
            String customAccommodationTypeText, Control payerNameControl,
            Control payerSurnameControl, Control payerStreetControl,
            Control payerHouseNumberControl, Control payerCountryControl,
            DatePicker payerBirthDateControl, Control cardNumberControl,
            Control cardHolderControl, Control cardSecurityCodeControl,
            List<ParticipantRow> participantRows) {

        List<String> errors = new ArrayList<>();
        errors.addAll(validatePageTravel(viewModel, destinationCountryControl, destinationControl,
                accommodationLocationControl, departureDateControl, returnDateControl,
                transportModeControl, transportModeValue, customTransportModeControl, customTransportModeText));
        errors.addAll(validatePageAccommodation(viewModel, accommodationType, customAccommodationOption,
                customAccommodationTypeControl, customAccommodationTypeText));
        errors.addAll(validatePagePayment(viewModel, payerNameControl, payerSurnameControl,
                payerStreetControl, payerHouseNumberControl, payerCountryControl,
                payerBirthDateControl, cardNumberControl, cardHolderControl, cardSecurityCodeControl));
        errors.addAll(validatePagePeople(viewModel, participantRows));
        return errors;
    }

    void markInvalid(Control control, String message) {
        if (!control.getStyleClass().contains("field-error")) {
            control.getStyleClass().add("field-error");
        }
        if (!invalidControls.contains(control)) {
            invalidControls.add(control);
            originalTooltips.put(control, control.getTooltip());
        }
        control.setTooltip(new Tooltip(message));
    }

    void removeValidationMark(Control control) {
        control.getStyleClass().remove("field-error");
        if (originalTooltips.containsKey(control)) {
            control.setTooltip(originalTooltips.get(control));
        }
        invalidControls.remove(control);
        originalTooltips.remove(control);
    }

    void clearValidationMarks() {
        for (Control control : invalidControls) {
            control.getStyleClass().remove("field-error");
            control.setTooltip(originalTooltips.get(control));
        }
        invalidControls.clear();
        originalTooltips.clear();
    }

    void configureValidationRecovery(Control... controls) {
        Stream.of(controls).forEach(this::attachValidationRecovery);
    }

    void attachValidationRecovery(Control control) {
        if (control == null) {
            return;
        }
        control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                removeValidationMark(control);
            }
        });
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static boolean isValidPersonName(String name) {
        return name.trim().matches(NAME_REGEX);
    }

    static boolean isParticipantEmpty(ParticipantViewModel participant) {
        return participant == null
                || (isBlank(participant.name.get())
                        && isBlank(participant.surname.get())
                        && participant.birthDate.get() == null);
    }

    static int filledParticipantCount(List<ParticipantViewModel> participants) {
        int count = 0;
        for (ParticipantViewModel participant : participants) {
            if (!isParticipantEmpty(participant)) {
                count++;
            }
        }
        return count;
    }

    private void validateField(Control control, boolean isInvalid, String errorMessage, List<String> errors) {
        if (isInvalid) {
            markInvalid(control, errorMessage);
            if (errors != null)
                errors.add(errorMessage);
        }
    }

    private void requireNonBlank(Control control, String value, String errorMessage, List<String> errors) {
        validateField(control, isBlank(value), errorMessage, errors);
    }

    private void requireFormat(Control control, String value, boolean isValid, String errorMessage,
            List<String> errors) {
        if (!isBlank(value)) {
            validateField(control, !isValid, errorMessage, errors);
        }
    }
}
