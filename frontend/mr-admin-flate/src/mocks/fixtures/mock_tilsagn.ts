import {
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataElementStatusVariant,
  DataElementTextFormat,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
} from "@tiltaksadministrasjon/api-client";

export const mockTilsagn: TilsagnDto[] = [
  {
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    type: TilsagnType.TILSAGN,
    periode: { start: "2025-06-01", slutt: "2025-12-01" },
    belop: 12207450,
    belopBrukt: 0,
    belopGjenstaende: 12207450,
    kostnadssted: { navn: "Nav tiltak Oslo", enhetsnummer: "0387" },
    bestillingsnummer: "A-2025/11133-23",
    status: {
      type: TilsagnStatus.TIL_GODKJENNING,
      status: {
        value: "Til godkjenning",
        variant: DataElementStatusVariant.WARNING,
        description: null,
      },
    },
    kommentar: null,
  },
  {
    type: TilsagnType.TILSAGN,
    belop: 14_000,
    belopBrukt: 4_000,
    belopGjenstaende: 10_000,
    id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
    },
    periode: {
      start: "2024-01-03",
      slutt: "2024-01-04",
    },
    status: {
      type: TilsagnStatus.TIL_GODKJENNING,
      status: {
        value: "Til annullering",
        variant: DataElementStatusVariant.ERROR_BORDER,
        description: null,
      },
    },
    bestillingsnummer: "A-2024/123",
    kommentar: null,
  },
  {
    type: TilsagnType.TILSAGN,
    belop: 14_000,
    belopBrukt: 4_000,
    belopGjenstaende: 10_000,
    id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: {
      type: TilsagnStatus.GODKJENT,
      status: { value: "Godkjent", variant: DataElementStatusVariant.SUCCESS, description: null },
    },
    bestillingsnummer: "A-2024/123",
    kommentar: "min kommentar",
  },
  {
    type: TilsagnType.TILSAGN,
    belop: 14_000,
    belopBrukt: 4_000,
    belopGjenstaende: 10_000,
    id: "c7cd1ac0-34cd-46f2-b441-6d8c7318ee05",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: {
      type: TilsagnStatus.ANNULLERT,
      status: {
        value: "Annullert",
        variant: DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH,
        description: null,
      },
    },
    bestillingsnummer: "A-2024/123",
    kommentar: "min kommentar",
  },
  {
    type: TilsagnType.TILSAGN,
    belop: 14_000,
    belopBrukt: 4_000,
    belopGjenstaende: 10_000,
    id: "5950e714-95bc-4d4c-b52e-c75fde749056",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: {
      type: TilsagnStatus.RETURNERT,
      status: { value: "Returnert", variant: DataElementStatusVariant.ERROR, description: null },
    },
    bestillingsnummer: "A-2024/123",
    kommentar: "min kommentar",
  },
];

export const mockTilsagnTable: DataDrivenTableDto = {
  columns: [
    {
      key: "bestillingsnummer",
      label: "Tilsagnsnummer",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "periodeStart",
      label: "Periodestart",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "periodeSlutt",
      label: "Periodeslutt",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "type",
      label: "Tilsagntype",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "kostnadssted",
      label: "Kostnadssted",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "antallPlasser",
      label: "Antall plasser",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.RIGHT,
    },
    {
      key: "belop",
      label: "Totalbeløp",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.RIGHT,
    },
    { key: "status", label: "Status", sortable: true, align: DataDrivenTableDtoColumnAlign.RIGHT },
    { key: "action", label: null, sortable: false, align: DataDrivenTableDtoColumnAlign.LEFT },
  ],
  rows: [
    {
      cells: {
        bestillingsnummer: {
          type: "DATA_ELEMENT_TEXT",
          value: "A-2025/11133-23",
          format: null,
        },
        periodeStart: {
          type: "DATA_ELEMENT_TEXT",
          value: "2025-06-01",
          format: DataElementTextFormat.DATE,
        },
        periodeSlutt: {
          type: "DATA_ELEMENT_TEXT",
          value: "2025-11-30",
          format: DataElementTextFormat.DATE,
        },
        type: {
          type: "DATA_ELEMENT_TEXT",
          value: "Tilsagn",
          format: null,
        },
        kostnadssted: {
          type: "DATA_ELEMENT_TEXT",
          value: "Nav tiltak Oslo",
          format: null,
        },
        antallPlasser: {
          type: "DATA_ELEMENT_TEXT",
          value: "97",
          format: DataElementTextFormat.NUMBER,
        },
        belop: {
          type: "DATA_ELEMENT_TEXT",
          value: "12207450",
          format: DataElementTextFormat.NOK,
        },
        status: {
          type: "DATA_ELEMENT_STATUS",
          value: "Til godkjenning",
          variant: DataElementStatusVariant.WARNING,
          description: null,
        },
        action: {
          type: "DATA_ELEMENT_LINK",
          digest: window.crypto.randomUUID().slice(0, 8),
          text: "Behandle",
          href: "/gjennomforinger/a7d63fb0-4366-412c-84b7-7c15518ee362/tilsagn/10e393b0-1b7c-4c68-9a42-b541b2f114b8",
        },
      },
      content: null,
    },
    {
      cells: {
        bestillingsnummer: {
          type: "DATA_ELEMENT_TEXT",
          value: "A-2025/11133-22",
          format: null,
        },
        periodeStart: {
          type: "DATA_ELEMENT_TEXT",
          value: "2025-08-28",
          format: DataElementTextFormat.DATE,
        },
        periodeSlutt: {
          type: "DATA_ELEMENT_TEXT",
          value: "2025-08-29",
          format: DataElementTextFormat.DATE,
        },
        type: {
          type: "DATA_ELEMENT_TEXT",
          value: "Ekstratilsagn",
          format: null,
        },
        kostnadssted: {
          type: "DATA_ELEMENT_TEXT",
          value: "Nav Alta-Kvænangen-Loppa",
          format: null,
        },
        antallPlasser: {
          type: "DATA_ELEMENT_TEXT",
          value: "1",
          format: DataElementTextFormat.NUMBER,
        },
        belop: {
          type: "DATA_ELEMENT_TEXT",
          value: "1353",
          format: DataElementTextFormat.NOK,
        },
        status: {
          type: "DATA_ELEMENT_STATUS",
          value: "Returnert",
          variant: DataElementStatusVariant.ERROR,
          description: null,
        },
        action: {
          type: "DATA_ELEMENT_LINK",
          digest: window.crypto.randomUUID().slice(0, 8),
          text: "Behandle",
          href: "/gjennomforinger/a7d63fb0-4366-412c-84b7-7c15518ee362/tilsagn/fd1825aa-1951-4de2-9b72-12d22f121e92",
        },
      },
      content: null,
    },
  ],
};
