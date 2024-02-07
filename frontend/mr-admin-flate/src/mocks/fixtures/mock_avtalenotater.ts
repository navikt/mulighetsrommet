import { AvtaleNotat } from "mulighetsrommet-api-client";
import { faker } from "@faker-js/faker";

export const mockAvtalenotater: AvtaleNotat[] = [
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    createdAt: new Date("2023-07-20 11:47:53").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "B123456",
      navn: "Bertil Bengtson",
    },
    innhold: "Dette er den beste avtalen i verden!",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2021-08-29 15:33:05").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "H987654",
      navn: "Captain Jack Sparrow",
    },
    innhold:
      "I’m dishonest, and a dishonest man you can always trust to be dishonest. Honestly. It’s the honest ones you want to watch out for, because you can never predict when they’re going to do something incredibly … stupid.",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2021-05-22 05:45:11").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "H060500",
      navn: "Hodor",
    },
    innhold:
      "Hodor. Hodor hodor, hodor. Hodor hodor hodor hodor hodor. Hodor. Hodor! Hodor hodor, hodor; hodor hodor hodor. Hodor. Hodor hodor; hodor hodor - hodor, hodor, hodor hodor. Hodor, hodor. Hodor. Hodor, hodor hodor hodor; hodor hodor; hodor hodor hodor! Hodor hodor HODOR! Hodor hodor... Hodor hodor hodor...",
  },
];
