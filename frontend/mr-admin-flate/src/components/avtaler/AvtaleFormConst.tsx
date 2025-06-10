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
import { splitNavEnheterByType } from "../navEnheter/helper";

function getUnderenheterAsSelectOptionsBy(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
  predicate: (item: NavEnhet) => boolean,
) {
  return enheter
    .filter((enhet: NavEnhet) => {
      return (
        enhet.overordnetEnhet != null &&
        navRegioner.includes(enhet?.overordnetEnhet) &&
        predicate(enhet)
      );
    })
    .map((enhet: NavEnhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
}

export function getLokaleUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
) {
  return getUnderenheterAsSelectOptionsBy(
    navRegioner,
    enheter,
    (enhet) => enhet.type === NavEnhetType.LOKAL,
  );
}

const spesialEnheter = [NavEnhetType.KO, NavEnhetType.ARK];
export function getSpesialUnderenheterAsSelectOptions(
  navRegioner: (string | undefined)[],
  enheter: NavEnhet[],
) {
  return getUnderenheterAsSelectOptionsBy(navRegioner, enheter, (enhet) =>
    spesialEnheter.includes(enhet.type),
  );
}

export function defaultAvtaleData(
  ansatt: NavAnsatt,
  avtale?: AvtaleDto,
): DeepPartial<InferredAvtaleSchema> {
  const navRegioner = avtale?.kontorstruktur?.map((struktur) => struktur.region.enhetsnummer) ?? [];

  const navEnheter = avtale?.kontorstruktur?.flatMap((struktur) => struktur.kontorer);
  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter || []);

  return {
    tiltakstype: avtale?.tiltakstype,
    navRegioner,
    navEnheter: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
    navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
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
    opsjonsmodell: avtale?.opsjonsmodell,
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
