import { Shortcut } from "./components/navbar/Navbar";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 15;
export const AVTALE_PAGE_SIZE = 15;

export const shortcuts: Shortcut[] = [
  {
    url: "/tiltakstyper",
    navn: "Tiltakstyper",
  },
  { url: "/avtaler", navn: "Avtaler" },
  {
    url: "/tiltaksgjennomforinger",
    navn: "Tiltaksgjennomføringer",
  },
];

export const forsideKort: {
  navn: string;
  url: string;
  tekst?: string;
}[] = [
  {
    navn: "Tiltakstyper",
    url: "tiltakstyper",
    tekst: "Her finner du informasjon om tiltakstyper",
  },
  {
    navn: "Avtaler",
    url: "avtaler",
    tekst: "Her finner du informasjon om avtaler",
  },
  {
    navn: "Tiltaksgjennomføringer",
    url: "tiltaksgjennomforinger",
    tekst: "Her finner du informasjon om tiltaksgjennomføringer",
  },
];

export const ANSKAFFEDE_TILTAK = [
  "ARBRRHDAG",
  "AVKLARAG",
  "GRUPPEAMO",
  "INDOPPFAG",
  "DIGIOPPARB",
  "JOBBK",
  "GRUFAGYRKE",
];
