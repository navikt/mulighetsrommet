import { DelutbetalingReturnertAarsak, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

export const utbetalingTekster = {
  title: "Utbetalinger",
  header: (gjennomforingNavn: string) => `Utbetaling for ${gjennomforingNavn}`,
  metadata: {
    header: "Til utbetaling",
    status: "Status",
    periode: "Utbetalingsperiode",
    type: "Type",
    innsendtDato: "Dato innsendt",
    innsendtAv: "Innsendt av",
    beskrivelse: "Begrunnelse for utbetaling",
    begrunnelseMindreBetalt: "Begrunnelse for mindre utbetalt",
  },
  periode: {
    label: "Periode",
    start: {
      label: "Periodestart",
    },
    slutt: {
      label: "Periodeslutt",
    },
  },
  beregning: {
    belop: {
      label: "Beløp",
    },
    utbetales: {
      label: "Utbetales",
    },
  },
  delutbetaling: {
    header: "Utbetalingslinjer",
    alert: {
      ingenTilsagn: "Det finnes ingen godkjente tilsagn for utbetalingsperioden",
    },
    handlinger: {
      button: {
        label: "Handlinger",
      },
      opprettTilsagn: (tilsagnsType: TilsagnType) => {
        const typeTekst = avtaletekster.tilsagn.type(tilsagnsType);
        return `Opprett ${typeTekst}`;
      },
      hentGodkjenteTilsagn: "Hent godkjente tilsagn",
      sendTilAttestering: "Send til attestering",
      fjern: "Fjern",
      returner: "Send i retur",
      attester: "Attester",
    },
    aarsak: {
      modal: {
        header: "Send i retur med forklaring",
        ingress: "Automatisk returnert som følge av at en annen utbetalingslinje ble returnert",
        button: {
          label: "Send i retur",
        },
      },
      fraRetunertAarsak: (aarsak: DelutbetalingReturnertAarsak): string => {
        switch (aarsak) {
          case DelutbetalingReturnertAarsak.FEIL_BELOP:
            return "Feil beløp";
          case DelutbetalingReturnertAarsak.ANNET:
            return "Annet";
          case DelutbetalingReturnertAarsak.PROPAGERT_RETUR:
            return "Automatisk returnert som følge av at en annen utbetalingslinje ble returnert";
          case DelutbetalingReturnertAarsak.TILSAGN_FEIL_STATUS:
            return "Tilsagnet har ikke lenger status godkjent og kan derfor ikke benyttes for utbetaling";
        }
      },
    },
    gjorOpp: {
      checkbox: {
        label: "Gjør opp tilsagn",
        helpText:
          "Hvis du huker av for å gjøre opp tilsagnet, betyr det at det ikke kan gjøres flere utbetalinger på tilsagnet etter at denne utbetalingen er attestert",
      },
    },
    belop: {
      label: "Utbetales",
    },
  },
} as const;
