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
