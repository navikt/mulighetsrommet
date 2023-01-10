import { Shortcut } from "./components/shortcuts/Shortcuts";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 15;

export const shortcutsForTiltaksansvarlig: Shortcut[] = [
  { url: "/mine", navn: "Mine tiltaksgjennomføringer" },
  { url: "/enhet", navn: "Enhetens tiltaksgjennomføringer" },
  {
    url: "/oversikt",
    navn: "Alle tiltaksgjennomføringer",
  },
];
export const shortcutsForFagansvarlig: Shortcut[] = [
  { url: "/tiltakstyper", navn: "Tiltakstyper" },
];
