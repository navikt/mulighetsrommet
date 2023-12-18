import {
  Avtale,
  Avtaletype,
  LeverandorUnderenhet,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Opphav,
  Virksomhet,
} from "mulighetsrommet-api-client";
import { DeepPartial } from "react-hook-form";
import { InferredAvtaleSchema } from "./AvtaleSchema";

export const getLokaleUnderenheterAsSelectOptions = (
  navRegioner: string[],
  enheter: NavEnhet[],
) => {
  return enheter
    .filter((enhet: NavEnhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet.overordnetEnhet) &&
        (enhet.type === NavEnhetType.LOKAL || enhet.type === NavEnhetType.KO)
      );
    })
    .map((enhet: NavEnhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
};

export const underenheterOptions = (underenheterForLeverandor: Virksomhet[]) =>
  underenheterForLeverandor.map((leverandor: LeverandorUnderenhet) => ({
    value: leverandor.organisasjonsnummer,
    label: `${leverandor.navn} - ${leverandor.organisasjonsnummer}`,
  }));

export function defaultAvtaleData(
  ansatt: NavAnsatt,
  avtale?: Avtale,
): DeepPartial<InferredAvtaleSchema> {
  const navRegioner = avtale?.kontorstruktur.map((struktur) => struktur.region.enhetsnummer) ?? [];
  const navEnheter =
    avtale?.kontorstruktur
      .flatMap((struktur) => struktur.kontorer)
      .map((enhet) => enhet.enhetsnummer) ?? [];
  return {
    tiltakstype: avtale?.tiltakstype,
    navRegioner,
    navEnheter,
    administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
    navn: avtale?.navn ?? "",
    avtaletype: avtale?.avtaletype ?? Avtaletype.AVTALE,
    leverandor: avtale?.leverandor?.organisasjonsnummer ?? "",
    leverandorUnderenheter:
      avtale?.leverandorUnderenheter?.length === 0 || !avtale?.leverandorUnderenheter
        ? []
        : avtale?.leverandorUnderenheter?.map(
            (leverandor: LeverandorUnderenhet) => leverandor.organisasjonsnummer,
          ),
    leverandorKontaktpersonId: avtale?.leverandorKontaktperson?.id,
    startOgSluttDato: {
      startDato: avtale?.startDato ? avtale.startDato : undefined,
      sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    },
    url: avtale?.url ?? "",
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    opphav: avtale?.opphav ?? Opphav.MR_ADMIN_FLATE,
  };
}
