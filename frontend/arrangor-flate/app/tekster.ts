import { TilsagnType, ArrFlateBeregning } from "api-client";
import { formaterDato } from "./utils/date";

export const tekster = {
  bokmal: {
    tilsagn: {
      detaljer: {
        headingTitle: "Detaljer for tilsagn",
        tilbakeLenke: "Tilbake til tilsagnsoversikt",
      },
      tilsagntype: (type: TilsagnType) => {
        switch (type) {
          case TilsagnType.TILSAGN:
            return "Tilsagn";
          case TilsagnType.EKSTRATILSAGN:
            return "Ekstratilsagn";
          case TilsagnType.INVESTERING:
            return "Tilsagn for investeringer";
        }
      },
    },

    utbetaling: {
      headingTitle: "Oversikt over innsendinger",
      oversiktFaner: {
        aktive: "Aktive",
        historiske: "Historiske",
        tilsagnsoversikt: "Tilsagnsoversikt",
      },
      opprettUtbetaling: {
        actionLabel: "Opprett krav om utbetaling",
        investering: "Opprett investeringstilskudd",
        driftstilskudd: "Opprett driftstilskudd",
      },
      tilbakeTilBeregning: "Tilbake til beregning",
      beregning: {
        infotekstDeltakerliste: {
          intro:
            "Hvis noen av opplysningene om deltakerne ikke stemmer må dere sende forslag til Nav om endring via",
          utro: " Opplysninger om deltakerne må være riktig oppdatert før dere sender inn kravet.",
        },
        stengtHosArrangor: "Det er registrert stengt hos arrangør i følgende perioder:",
        ubehandledeDeltakerforslag:
          "Det finnes ubehandlede forslag som påvirker utbetalinger på følgende personer. Disse må først godkjennes av Nav-veileder før utbetalingen oppdaterer seg.",
        deltakelsesfaktor: (type: ArrFlateBeregning["type"]) => {
          switch (type) {
            case "PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER":
            case "PRIS_PER_MANEDSVERK":
              return "Månedsverk";
            case "PRIS_PER_UKESVERK":
              return "Ukesverk";
            case "FRI":
              throw new Error("Deltakelsesfaktor ikke støttet for type FRI");
          }
        },
      },
      oppsummering: {
        bekreftelse:
          "Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold",
      },
      kvittering: {
        headingTitle: "Innsendingen er mottatt",
        successMelding:
          "Vi har mottatt ditt krav om utbetaling, og utbetalingen er nå til behandling hos Nav. Vi vil ta kontakt med deg dersom vi trenger mer informasjon.",
        kvitteringTitle: "Kvittering for innsending",
        mottattAv: (tidspunkt: Date | string) => `Mottatt av Nav: ${formaterDato(tidspunkt)}`,
        orgnr: (orgnr: string) => `Organisasjonsnummer: ${orgnr}`,
        statusLenkeIntro: "Her kan du se",
        statusLenkeTekst: "Status på utbetalingen",
        pdfKvitteringLenke: "Innsendingskvittering (åpnes i ny fane)",
        kontoTitle: "Konto for utbetaling",
        kontonummerRegistrert: "Vi har registrert følgende kontonummer: ",
      },
      pdfNavn: (tidspunkt: Date | string) => `utbetaling-${formaterDato(tidspunkt)}.pdf`,
    },
    tilbakeTilOversikt: "Tilbake til oversikten",
  },
};
