import { Shortcut } from "./components/shortcuts/Shortcuts";

export const APPLICATION_NAME = "mr-admin-flate";

export const PAGE_SIZE = 15;

export const shortcutsForTiltaksansvarlig: Shortcut[] = [
  { url: "/oversikt", navn: "Tiltaksgjennomføringer" },
  { url: "/enhet", navn: "Min enhets tiltaksgjennomføringer" },
];
export const shortcutsForFagansvarlig: Shortcut[] = [
  { url: "/tiltakstyper", navn: "Tiltakstyper" },
];
