import { PaginertArrangor } from "@mr/api-client";

export const mockArrangorer: PaginertArrangor = {
  pagination: {
    pageSize: 3,
    totalCount: 3,
    totalPages: 1,
  },
  data: [
    {
      id: "c95e836f-a381-4d82-b8e3-74257b14f26c",
      organisasjonsnummer: "123456789",
      navn: "FRETEX AS",
      overordnetEnhet: null,
      underenheter: [
        {
          id: "d9d4db51-3564-4493-b897-4fc38dc48965",
          organisasjonsnummer: "992943084",
          navn: "FRETEX AS AVD OSLO",
          overordnetEnhet: "123456789",
        },
        {
          id: "d9d4db51-3564-4493-b897-4fc38dc48963",
          organisasjonsnummer: "984013922",
          navn: "FRETEX AS AVD SANDNES",
          overordnetEnhet: "123456789",
        },
        {
          id: "d9d4db51-3564-4493-b897-4ec38dc48963",
          organisasjonsnummer: "916281676",
          navn: "FRETEX AS AVD TRONDHEIM",
          overordnetEnhet: "123456789",
        },
      ],
    },
    {
      id: "2dd86798-7a93-4d51-80ce-3e63f8e2daf3",
      organisasjonsnummer: "987654321",
      navn: "Ikea AS",
      overordnetEnhet: null,
    },
    {
      id: "a1a5191f-1478-4ad8-9a82-47182a28a19d",
      organisasjonsnummer: "134256789",
      navn: "Røde Kors AS",
      overordnetEnhet: null,
    },
  ],
};
