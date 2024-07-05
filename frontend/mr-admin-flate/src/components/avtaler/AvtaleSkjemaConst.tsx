import {
  ArrangorKontaktperson,
  Avtale,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
} from "mulighetsrommet-api-client";
import { DeepPartial } from "react-hook-form";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";

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
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];
  const navEnheter =
    avtale?.kontorstruktur
      ?.flatMap((struktur) => struktur.kontorer)
      ?.map((enhet) => enhet.enhetsnummer) ?? [];
  return {
    tiltakstype: avtale?.tiltakstype,
    navRegioner,
    navEnheter,
    administratorer: avtale?.administratorer?.map((admin) => admin.navIdent) || [ansatt.navIdent],
    navn: avtale?.navn ?? "",
    avtaletype: avtale?.avtaletype,
    arrangorOrganisasjonsnummer: avtale?.arrangor?.organisasjonsnummer ?? "",
    arrangorUnderenheter: !avtale?.arrangor?.underenheter
      ? []
      : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
    arrangorKontaktpersoner:
      avtale?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    startOgSluttDato: {
      startDato: avtale?.startDato ? avtale.startDato : undefined,
      sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    },
    websaknummer: avtale?.websaknummer,
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    personvernBekreftet: avtale?.personvernBekreftet,
    personopplysninger: avtale?.personopplysninger ?? [],
    amoKategorisering: avtale?.amoKategorisering ?? undefined,
    opsjonsmodellData: {
      opsjonMaksVarighet: avtale?.opsjonsmodellData?.opsjonMaksVarighet ?? undefined,
      opsjonsmodell: avtale?.opsjonsmodellData?.opsjonsmodell ?? undefined,
      customOpsjonsmodellNavn: avtale?.opsjonsmodellData?.customOpsjonsmodellNavn ?? undefined,
    },
  };
}
