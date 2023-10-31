import { Shortcut } from "./components/navbar/Navbar";
import { erProdMiljo } from "./utils/Utils";

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

const forhandsvisningMiljo = import.meta.env.dev || erProdMiljo() ? "nav.no" : "dev.nav.no";

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
    tekst: "Her finner du informasjon om tiltaksgjennomføringer for gruppetiltak",
  },
  {
    navn: "Individuelle tiltaksgjennomføringer",
    url: "https://mulighetsrommet-sanity-studio.intern.nav.no/prod/desk",
    tekst: "Her administrerer du individuelle tiltaksgjennomføringer",
  },
  {
    navn: "Veilederflate forhåndsvisning",
    url: `https://mulighetsrommet-veileder-flate.intern.${forhandsvisningMiljo}/preview`,
    tekst: "Her kan du se hvordan tiltakene vises for veileder i Modia.",
  },
];
