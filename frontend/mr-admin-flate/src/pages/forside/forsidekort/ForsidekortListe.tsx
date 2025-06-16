import {
  ENDRINGSMELDINGER_URL,
  PREVIEW_ARBEIDSMARKEDSTILTAK_URL,
  SANITY_STUDIO_URL,
} from "@/constants";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { EndringsmeldingerIkon } from "@/components/ikoner/EndringsmeldingerIkon";
import { ForhandsvisningIkon } from "@/components/ikoner/ForhandsvisningIkon";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Forsidekort, ForsideKortProps } from "./Forsidekort";
import { BellDotFillIcon } from "@navikt/aksel-icons";

const forsidekortData: ForsideKortProps[] = [
  {
    navn: "Oppgaver",
    ikon: (
      <div className="w-16 h-16 flex items-center justify-center bg-orange-300 rounded-full">
        <BellDotFillIcon title="Oppgaveoversikt" className="text-white w-12 h-12" />
      </div>
    ),
    url: "oppgaveoversikt/oppgaver",
    tekst: "Her finner du en oversikt over enhetens oppgaver",
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
    <div className="flex flex-wrap gap-8">
      {forsidekortData.map((kort) => {
        return <Forsidekort key={kort.navn} {...kort} />;
      })}
    </div>
  );
}
