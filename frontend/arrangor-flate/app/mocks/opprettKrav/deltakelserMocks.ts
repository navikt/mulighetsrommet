import {
  DataDrivenTableDtoColumnAlign,
  DataElementTextFormat,
  LabeledDataElementType,
  OpprettKravDeltakere,
  OpprettKravDeltakereGuidePanelType,
} from "@api-client";
import { dataElementText } from "../dataDrivenTableHelpers";
import { gjennomforingIdOppfolging } from "./gjennomforingMocks";

const oppfolgingDeltakere: OpprettKravDeltakere = {
  guidePanel: OpprettKravDeltakereGuidePanelType.TIMESPRIS,
  stengtHosArrangor: [],
  tabell: {
    columns: [
      { key: "navn", label: "Navn", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
      {
        key: "identitetsnummer",
        label: "Fødselsnr.",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "tiltakStart",
        label: "Startdato i tiltaket",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "periodeStart",
        label: "Startdato i perioden",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "periodeSlutt",
        label: "Sluttdato i perioden",
        sortable: true,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
    ],
    rows: [
      {
        cells: {
          navn: dataElementText("Barberskum, Muskuløs"),
          identitetsnummer: dataElementText("21896896757"),
          tiltakStart: dataElementText("2024-11-15", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Bjørk, Subtil"),
          identitetsnummer: dataElementText("06438749665"),
          tiltakStart: dataElementText("2024-11-15", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Krem, Kry"),
          identitetsnummer: dataElementText("16428500745"),
          tiltakStart: dataElementText("2024-11-14", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Mandarin, Akademisk"),
          identitetsnummer: dataElementText("22487805126"),
          tiltakStart: dataElementText("2025-01-13", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
    ],
  },
  tabellFooter: [
    {
      header: null,
      entries: [
        {
          label: "Antall deltakere",
          type: LabeledDataElementType.INLINE,
          value: {
            value: "4",
            type: "DATA_ELEMENT_TEXT",
            format: null,
          },
        },
      ],
    },
    {
      header: null,
      entries: [
        {
          label: "Avtalt pris per time oppfølging per deltaker",
          type: LabeledDataElementType.INLINE,
          value: {
            value: "768",
            type: "DATA_ELEMENT_MONEY_AMOUNT",
            currency: "NOK",
          },
        },
      ],
    },
  ],
};

export const deltakere: Record<string, OpprettKravDeltakere> = {
  [gjennomforingIdOppfolging]: oppfolgingDeltakere,
};
