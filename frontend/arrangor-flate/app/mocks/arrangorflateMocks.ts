import { ArrangorflateTilsagn, TilsagnStatus, TilsagnType } from "api-client";
import { v4 as uuid } from "uuid";

// Mock data with all TilsagnStatus values
export const mockTilsagn: ArrangorflateTilsagn[] = [
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-07-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.GODKJENT },
    bestillingsnummer: "A-2024/10354-2",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2024-07-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 15,
        sats: 20205,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 150_000,
      },
    },
    bruktBelop: 50_000,
    gjenstaendeBelop: 100_000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Halden",
    },
    type: TilsagnType.TILSAGN,
  },

  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-09-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.TIL_ANNULLERING },
    bestillingsnummer: "A-2024/10354-4",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2024-09-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 5,
        sats: 20205,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 50_000,
      },
    },
    bruktBelop: 0,
    gjenstaendeBelop: 50_000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Sarpsborg",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-10-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.ANNULLERT },
    bestillingsnummer: "A-2024/10354-5",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2024-10-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 3,
        sats: 20205,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 30_000,
      },
    },
    bruktBelop: 30_000,
    gjenstaendeBelop: 0,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Råde",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-11-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.TIL_OPPGJOR },
    bestillingsnummer: "A-2024/10354-6",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2024-11-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 2,
        sats: 20205,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 20_000,
      },
    },
    bruktBelop: 0,
    gjenstaendeBelop: 20_000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Våler",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-12-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.OPPGJORT },
    bestillingsnummer: "A-2024/10354-7",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2024-12-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 1,
        sats: 20205,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 10_000,
      },
    },
    bruktBelop: 10_000,
    gjenstaendeBelop: 0,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Indre Østfold",
    },
    type: TilsagnType.TILSAGN,
  },
];
