import { AvtaleNotat } from "mulighetsrommet-api-client";
import { faker } from "@faker-js/faker";

export const mockAvtalenotater: AvtaleNotat[] = [
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    createdAt: new Date("07.20.2023 11:47:53").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: { navIdent: "B99876", navn: "Bertil Betabruker" },
    innhold: "Dette er den beste avtalen i verden!",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("07.09.2023 02:15:14").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: { navIdent: "B123456", navn: "Solo Polo" },
    innhold:
      "Dog bone puppy bark bell, stinky bark bell puppies heel dobie tails tennis ball. Leap floppy ears wiggle wiggle dobie tails bark. Paw puppies fluffy chase tail stand, lab tail vet squirrel tail dachshund wiggle dachshund dog bowl. Chase tail rottie leash release collar shepherd fetch. Stay fluffy tail pittie puppies puppies squeak toy, leave it shake stand bark roll over vet.",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("12.26.2022 19:00:02").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: { navIdent: "C300000", navn: "C3PO" },
    innhold:
      "Did you hear that? They've shut down the main reactor. We'll be destroyed for sure. This is madness! We're doomed! There'll be no escape for the Princess this time. What's that? Artoo! Artoo-Detoo, where are you? At last! Where have you been? They're heading in this direction.",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("08.29.2021 15:33:05").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: { navIdent: "H987654", navn: "Captain Jack Sparrow" },
    innhold:
      "Prow scuttle parrel provost Sail ho shrouds spirits boom mizzenmast yardarm. Pinnace holystone mizzenmast quarter crow's nest nipperkin grog yardarm hempen halter furl. Swab barque interloper chantey doubloon starboard grog black jack gangway rutters.",
  },
  {
    id: faker.string.uuid(),
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("05.22.2021 05:45:11").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: { navIdent: "H060500", navn: "Hodor" },
    innhold:
      "Hodor. Hodor hodor, hodor. Hodor hodor hodor hodor hodor. Hodor. Hodor! Hodor hodor, hodor; hodor hodor hodor. Hodor. Hodor hodor; hodor hodor - hodor, hodor, hodor hodor. Hodor, hodor. Hodor. Hodor, hodor hodor hodor; hodor hodor; hodor hodor hodor! Hodor hodor HODOR! Hodor hodor... Hodor hodor hodor...",
  },
];
