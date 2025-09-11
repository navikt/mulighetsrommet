import { DelutbetalingReturnertAarsak } from "@tiltaksadministrasjon/api-client";

export const utbetalingTekster = {
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
