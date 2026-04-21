package com.uv.naloge.naloga3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

class ReservationReportBuilder {

    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;

    ReservationReportBuilder(DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter) {
        this.dateFormatter = dateFormatter;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    String buildReservationReport(ReservationViewModel data) {
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
                ReservationValidator.filledParticipantCount(data.participants),
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

    String buildParticipantsList(List<ParticipantViewModel> participants) {
        StringBuilder list = new StringBuilder();
        int listedIndex = 1;
        for (int index = 0; index < participants.size(); index++) {
            ParticipantViewModel p = participants.get(index);
            if (ReservationValidator.isParticipantEmpty(p)) {
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

    static String nonNullText(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(dateFormatter);
    }
}
