import { Shortcut } from "./components/navbar/Navbar";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 15;

export const shortcuts: Shortcut[] = [
  { url: "/tiltakstyper", navn: "Tiltakstyper" },
];

export const forsideKort: { navn: string; url: string; tekst?: string }[] = [
  {
    navn: "Tiltakstyper",
    url: "tiltakstyper",
    tekst: "Her finner du informasjon om tiltakstyper",
  },
];
