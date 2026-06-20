# Fakturaering

## Titel

Som forretningsansvarlig for PetClinic ønsker jeg, at mit system automatisk kan oprette og håndtere fakturaer for gennemførte besøg, så klinikken kan administrere betalinger og fakturahistorik uden manuelle arbejdsgange.

## Pitch

PetClinic understøtter i dag registrering af ejere, dyr, dyrlæger og besøg, men der mangler en egentlig faktureringsproces. Når et besøg registreres i `spring-petclinic-visits-service`, skal systemet automatisk kunne oprette en faktura i en ny Billing Service.

## Forretningsmæssige acceptkriterier

- Klinikken kan se fakturahistorik for en ejer.
- En faktura oprettes uden manuel handling, når et besøg registreres.
- En faktura kan kun betales én gang.
- Duplikerede visit-events må ikke skabe duplikerede fakturaer.
- Fejl i fakturering må ikke forhindre registrering af et visit.
- Systemet skal kunne demonstreres med docker compose.
