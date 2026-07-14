import {
  ArrangorDto,
  ArrangorHovedenhetDto,
  PaginatedResponseArrangorDto,
} from "@tiltaksadministrasjon/api-client";

export const mockFretexUnderenheter: ArrangorDto[] = [
  {
    id: "d9d4db51-3564-4493-b897-4fc38dc48965",
    organisasjonsnummer: "992943084",
    organisasjonsform: "BEDR",
    navn: "FRETEX AS AVD OSLO",
    overordnetEnhet: "123456789",
    slettetDato: null,
    erUtenlandsk: false,
  },
  {
    id: "d9d4db51-3564-4493-b897-4fc38dc48963",
    organisasjonsnummer: "984013922",
    organisasjonsform: "BEDR",
    erUtenlandsk: false,
    navn: "FRETEX AS AVD SANDNES",
    overordnetEnhet: "123456789",
    slettetDato: null,
  },
  {
    id: "d9d4db51-3564-4493-b897-4ec38dc48963",
    organisasjonsnummer: "916281676",
    erUtenlandsk: false,
    organisasjonsform: "BEDR",
    navn: "FRETEX AS AVD TRONDHEIM",
    overordnetEnhet: "123456789",
    slettetDato: null,
  },
];

export const mockFretexHovedenhet: ArrangorHovedenhetDto = {
  id: "c95e836f-a381-4d82-b8e3-74257b14f26c",
  organisasjonsnummer: "123456789",
  organisasjonsform: "AS",
  navn: "FRETEX AS",
  slettetDato: null,
  erUtenlandsk: false,
  underenheter: mockFretexUnderenheter,
};

export const mockArrangorer: PaginatedResponseArrangorDto = {
  pagination: {
    pageSize: 6,
    totalCount: 6,
    totalPages: 1,
  },
  data: [
    {
      id: "c95e836f-a381-4d82-b8e3-74257b14f26c",
      organisasjonsnummer: "123456789",
      organisasjonsform: "AS",
      navn: "FRETEX AS",
      overordnetEnhet: null,
      slettetDato: null,
      erUtenlandsk: false,
    },
    ...mockFretexUnderenheter,
    {
      id: "2dd86798-7a93-4d51-80ce-3e63f8e2daf3",
      organisasjonsnummer: "987654321",
      organisasjonsform: "AS",
      navn: "Ikea AS",
      overordnetEnhet: null,
      slettetDato: null,
      erUtenlandsk: false,
    },
    {
      id: "a1a5191f-1478-4ad8-9a82-47182a28a19d",
      organisasjonsnummer: "134256789",
      erUtenlandsk: false,
      organisasjonsform: "AS",
      navn: "Røde Kors AS",
      overordnetEnhet: null,
      slettetDato: null,
    },
  ],
};
