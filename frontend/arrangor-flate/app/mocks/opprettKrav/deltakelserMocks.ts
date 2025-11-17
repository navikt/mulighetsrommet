import {
  DataDrivenTableDtoColumnAlign,
  DataElementTextFormat,
  DetailsFormat,
  OpprettKravDeltakere,
  OpprettKravDeltakereGuidePanelType,
  OpprettKravVeiviserSteg,
} from "@api-client";
import { dataElementText } from "../dataDrivenTableHelpers";
import { gjennomforingIdOppfolging } from "./gjennomforingMocks";

const oppfolgingDeltakere: OpprettKravDeltakere = {
  guidePanel: OpprettKravDeltakereGuidePanelType.TIMESPRIS,
  stengtHosArrangor: [],
  tabell: {
    columns: [
      { key: "navn", label: "Navn", sortable: false, align: DataDrivenTableDtoColumnAlign.LEFT },
      {
        key: "identitetsnummer",
        label: "Fødselsnr.",
        sortable: false,
        align: DataDrivenTableDtoColumnAlign.LEFT,
      },
      {
        key: "tiltakStart",
        label: "Startdato i tiltaket",
        sortable: false,
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
          navn: dataElementText("Nordmann, Ola"),
          identitetsnummer: dataElementText("27017809102"),
          tiltakStart: dataElementText("2024-11-15", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Hansen, Helga"),
          identitetsnummer: dataElementText("27017809103"),
          tiltakStart: dataElementText("2024-11-15", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Pettersen, Asbjørn"),
          identitetsnummer: dataElementText("27017809104"),
          tiltakStart: dataElementText("2024-11-14", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
      {
        cells: {
          navn: dataElementText("Dahl, Solveig"),
          identitetsnummer: dataElementText("27017809105"),
          tiltakStart: dataElementText("2025-01-13", DataElementTextFormat.DATE),
          periodeStart: dataElementText("2025-10-01", DataElementTextFormat.DATE),
          periodeSlutt: dataElementText("2025-11-01", DataElementTextFormat.DATE),
        },
        content: null,
      },
    ],
  },
  tabellFooter: [
    { key: "Antall deltakere", value: "4", format: DetailsFormat.NUMBER },
    {
      key: "Avtalt pris per time oppfølging per deltaker",
      value: "644",
      format: DetailsFormat.NOK,
    },
  ],
  navigering: {
    tilbake: OpprettKravVeiviserSteg.INFORMASJON,
    neste: OpprettKravVeiviserSteg.UTBETALING,
  },
};

export const deltakere: Record<string, OpprettKravDeltakere> = {
  [gjennomforingIdOppfolging]: oppfolgingDeltakere,
};
