import {
  ArrangorKontaktperson,
  AvtaleDto,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Utdanningslop,
  UtdanningslopDbo,
} from "@mr/api-client-v2";
import { DeepPartial } from "react-hook-form";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";

export function getLokaleUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
) {
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
}

export function defaultAvtaleData(
  ansatt: NavAnsatt,
  avtale?: AvtaleDto,
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
    arrangorHovedenhet: avtale?.arrangor?.organisasjonsnummer ?? "",
    arrangorUnderenheter: !avtale?.arrangor?.underenheter
      ? []
      : avtale.arrangor.underenheter.map((underenhet) => underenhet.organisasjonsnummer),
    arrangorKontaktpersoner:
      avtale?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    startOgSluttDato: {
      startDato: avtale?.startDato ? avtale.startDato : undefined,
      sluttDato: avtale?.sluttDato ? avtale.sluttDato : undefined,
    },
    sakarkivNummer: avtale?.sakarkivNummer,
    prisbetingelser: avtale?.prisbetingelser ?? undefined,
    beskrivelse: avtale?.beskrivelse ?? null,
    faneinnhold: avtale?.faneinnhold ?? null,
    personvernBekreftet: avtale?.personvernBekreftet,
    personopplysninger: avtale?.personopplysninger ?? [],
    amoKategorisering: avtale?.amoKategorisering ?? null,
    opsjonsmodell: {
      type: avtale?.opsjonsmodell?.type ?? undefined,
      opsjonMaksVarighet: avtale?.opsjonsmodell?.opsjonMaksVarighet ?? undefined,
      customOpsjonsmodellNavn: avtale?.opsjonsmodell?.customOpsjonsmodellNavn ?? undefined,
    },
    utdanningslop: avtale?.utdanningslop ? toUtdanningslopDbo(avtale.utdanningslop) : undefined,
    prismodell: avtale?.prismodell ?? null,
  };
}

function toUtdanningslopDbo(data: Utdanningslop): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}
