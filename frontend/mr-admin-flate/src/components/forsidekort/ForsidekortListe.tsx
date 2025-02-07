import {
  ENDRINGSMELDINGER_URL,
  PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
  SANITY_STUDIO_URL,
} from "@/constants";
import { AvtaleIkon } from "../ikoner/AvtaleIkon";
import { EndringsmeldingerIkon } from "../ikoner/EndringsmeldingerIkon";
import { ForhandsvisningIkon } from "../ikoner/ForhandsvisningIkon";
import { GjennomforingIkon } from "../ikoner/GjennomforingIkon";
import { TiltakstypeIkon } from "../ikoner/TiltakstypeIkon";
import { Forsidekort, ForsideKortProps } from "./Forsidekort";

const forsidekortData: ForsideKortProps[] = [
  {
    navn: "Tiltakstyper",
    ikon: <TiltakstypeIkon inkluderBakgrunn aria-label="Tiltakstyper" />,
    url: "tiltakstyper",
    tekst: "Her finner du informasjon om tiltakstyper for gruppetiltak",
  },
  {
    navn: "Avtaler",
    ikon: <AvtaleIkon inkluderBakgrunn aria-label="Avtaler" />,
    url: "avtaler",
    tekst: "Her finner du informasjon om avtaler for gruppetiltak",
  },
  {
    navn: "Gjennomføringer",
    ikon: <GjennomforingIkon inkluderBakgrunn aria-label="Gjennomføringer for gruppetiltak" />,
    url: "gjennomforinger",
    tekst: "Her finner du informasjon om Gjennomføringer for gruppetiltak",
  },
  {
    navn: "Individuelle gjennomføringer",
    ikon: <img className="h-16 w-16 rounded-full" src="./sanity_logo.png" alt="Sanity-logo" />,
    url: SANITY_STUDIO_URL,
    apneINyTab: true,
    tekst: "Her administrerer du individuelle gjennomføringer",
  },
  {
    navn: "Veilederflate forhåndsvisning",
    ikon: <ForhandsvisningIkon inkluderBakgrunn aria-label="Preview" />,
    url: PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
    tekst: "Her kan du se hvordan tiltakene vises for veileder i Modia",
  },
  {
    navn: "Endringsmeldinger",
    ikon: <EndringsmeldingerIkon inkluderBakgrunn aria-label="Endringsmeldinger" />,
    url: ENDRINGSMELDINGER_URL,
    tekst: "Her finner du endringsmeldinger fra tiltaksarrangør",
  },
];

export function ForsidekortListe() {
  return (
    <div className="flex justify-around  flex-wrap gap-8">
      {forsidekortData.map((kort) => (
        <Forsidekort key={kort.navn} {...kort} />
      ))}
    </div>
  );
}
