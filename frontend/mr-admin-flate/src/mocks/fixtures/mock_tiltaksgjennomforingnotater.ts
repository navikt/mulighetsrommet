import { TiltaksgjennomforingNotat } from "mulighetsrommet-api-client";
import { faker } from "@faker-js/faker";

export const mockTiltaksgjennomforingnotater: TiltaksgjennomforingNotat[] = [
  {
    id: faker.string.uuid(),
    tiltaksgjennomforingId: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    createdAt: new Date("2023-07-20 11:47:53").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "B123456",
      navn: "Bertil Bengtson",
    },
    innhold: "Dette er den beste tiltaksgjennomføringen i verden!",
  },
  {
    id: faker.string.uuid(),
    tiltaksgjennomforingId: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    createdAt: new Date("2023-07-09 02:15:14").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "B123456",
      navn: "Solo Polo",
    },
    innhold:
      "Dog bone puppy bark bell, stinky bark bell puppies heel dobie tails tennis ball. Leap floppy ears wiggle wiggle dobie tails bark. Paw puppies fluffy chase tail stand, lab tail vet squirrel tail dachshund wiggle dachshund dog bowl. Chase tail rottie leash release collar shepherd fetch. Stay fluffy tail pittie puppies puppies squeak toy, leave it shake stand bark roll over vet.",
  },
  {
    id: faker.string.uuid(),
    tiltaksgjennomforingId: "a7d63fb0-4366-412c-84b7-7c15518ee361",
    createdAt: new Date("2022-12-26 19:00:02").toISOString(),
    updatedAt: new Date().toISOString(),
    opprettetAv: {
      navIdent: "C300000",
      navn: "C3PO",
    },
    innhold:
      "Did you hear that? They've shut down the main reactor. We'll be destroyed for sure. This is madness! We're doomed! There'll be no escape for the Princess this time. What's that? Artoo! Artoo-Detoo, where are you? At last! Where have you been? They're heading in this direction.",
  },
];
