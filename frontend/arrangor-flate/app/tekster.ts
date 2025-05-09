import { TilsagnType } from "api-client";
import { formaterDato } from "./utils";

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
      opprettUtbetalingKnapp: "Opprett krav om utbetaling",
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
    },
    tilbakeTilOversikt: "Tilbake til oversikten",
  },
};
