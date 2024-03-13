import { Avtale, NavAnsatt, NavEnhet, NavEnhetType } from "mulighetsrommet-api-client";
import { DeepPartial } from "react-hook-form";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";

export const getLokaleUnderenheterAsSelectOptions = (
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
) => {
  return enheter
    .filter((enhet: NavEnhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet?.overordnetEnhet) &&
        (enhet.type === NavEnhetType.LOKAL || enhet.type === NavEnhetType.KO)
      );
    })
    .map((enhet: NavEnhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
};

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
    avtaletype: avtale?.avtaletype,
    leverandor: avtale?.leverandor?.organisasjonsnummer ?? "",
    leverandorUnderenheter: !avtale?.leverandor?.underenheter
      ? []
      : avtale.leverandor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
    leverandorKontaktpersonId: avtale?.leverandor?.kontaktperson?.id,
    startOgSluttDato: {
      startDato: avtale?.startDato ? avtale.startDato : undefined,
      sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    },
    url: avtale?.url ?? "",
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
  };
}
