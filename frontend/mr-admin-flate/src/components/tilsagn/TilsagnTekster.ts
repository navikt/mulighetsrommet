export const tilsagnTekster = {
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
  prismodell: {
    label: "Prismodell",
    fastSatsManed: {
      label: "Fast sats per tiltaksplass per måned",
    },
    manedsPris: {
      label: "Avtalt månedspris per tiltaksplass",
    },
    ukesPris: {
      label: "Avtalt ukespris per tiltaksplass",
    },
    annenPris: {
      label: "Annen avtalt pris",
    },
  },
  sats: {
    label: "Sats",
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
