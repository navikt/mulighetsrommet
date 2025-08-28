import { TilsagnBeregningDto, TilsagnBeregningType } from "@mr/api-client-v2";

export const tilsagnTekster = {
  kommentar: {
    label: "Kommentar",
  },
  bestillingsnummer: {
    label: "Tilsagnsnummer",
  },
  belopGjenstaende: {
    label: "Gjenstående beløp",
  },
  belopBrukt: {
    label: "Brukt beløp",
  },
  periode: {
    label: "Tilsagnsperiode",
    start: {
      label: "Periodestart",
    },
    slutt: {
      label: "Periodeslutt",
    },
  },
  type: {
    label: "Tilsagnstype",
  },
  status: {
    label: "Status",
  },
  kostnadssted: {
    label: "Kostnadssted",
  },
  antallPlasser: {
    label: "Antall plasser",
  },
  antallTimerOppfolgingPerDeltaker: {
    label: "Antall oppfølgingstimer per deltaker",
  },
  prismodell: {
    label: "Prismodell",
    sats: {
      label: (type: TilsagnBeregningType | TilsagnBeregningDto["type"]) => {
        switch (type) {
          case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
          case TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED:
            return "Fast sats per tiltaksplass per måned";
          case "PRIS_PER_MANEDSVERK":
          case TilsagnBeregningType.PRIS_PER_MANEDSVERK:
            return "Avtalt månedspris per tiltaksplass";
          case "PRIS_PER_UKESVERK":
          case TilsagnBeregningType.PRIS_PER_UKESVERK:
            return "Avtalt ukespris per tiltaksplass";
          case "PRIS_PER_TIME_OPPFOLGING":
          case TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING:
            return "Avtalt pris per time oppfølging per deltaker";
          case "FRI":
          case TilsagnBeregningType.FRI:
            return "Annen avtalt pris";
        }
      },
    },
  },
  sats: {
    label: (type: TilsagnBeregningDto["type"]) => {
      switch (type) {
        case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
          return "Sats";
        case "PRIS_PER_TIME_OPPFOLGING":
          return "Avtalt pris per oppfølgingstime";
        case "PRIS_PER_UKESVERK":
        case "FRI":
        case "PRIS_PER_MANEDSVERK":
          return "Avtalt pris";
      }
    },
  },
  beregning: {
    belop: {
      label: "Totalbeløp",
    },
    prisbetingelser: {
      label: "Pris- og betalingsbetingelser",
    },
    input: {
      label: "Utregning",
      linjer: {
        rad: {
          label: "Rad",
        },
        beskrivelse: {
          label: "Beskrivelse",
        },
        belop: {
          label: "Beløp",
        },
        antall: {
          label: "Antall",
        },
        delsum: {
          label: "Delsum",
        },
      },
    },
  },
  totrinn: {
    behandletAv: "Behandlet av",
    besluttetAv: "Besluttet av",
  },
  manglerStartdato: "Du må velge en startdato",
  manglerSluttdato: "Du må velge en sluttdato",
  manglerKostnadssted: "Du må velge et kostnadssted",
  manglerBelop: "Du må skrive inn et beløp for tilsagnet",
} as const;
