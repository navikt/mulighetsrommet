import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { isKursTiltak } from "@/utils/Utils";
import { splitNavEnheterByType, TypeSplittedNavEnheter } from "@/api/enhet/helpers";
import {
  AmoKategorisering,
  ArrangorKontaktperson,
  AvtaleDto,
  GjennomforingDto,
  GjennomforingOppstartstype,
  NavAnsattDto,
  Faneinnhold as NyFaneInnhold,
  UtdanningslopDbo,
} from "@tiltaksadministrasjon/api-client";
import { slateFaneinnholdToPortableText } from "../portableText/helper";

export function defaultOppstartType(avtale?: AvtaleDto): GjennomforingOppstartstype {
  if (!avtale) {
    return GjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.tiltakskode;
  return isKursTiltak(tiltakskode)
    ? GjennomforingOppstartstype.FELLES
    : GjennomforingOppstartstype.LOPENDE;
}

function defaultNavRegion(avtale: AvtaleDto, gjennomforing?: Partial<GjennomforingDto>): string[] {
  if (gjennomforing?.kontorstruktur) {
    return gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer);
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur.map((struktur) => struktur.region.enhetsnummer);
  }
  return [];
}

function defaultNavEnheter(
  avtale: AvtaleDto,
  gjennomforing?: Partial<GjennomforingDto>,
): TypeSplittedNavEnheter {
  if (gjennomforing?.kontorstruktur) {
    return splitNavEnheterByType(
      gjennomforing.kontorstruktur.flatMap((struktur) => struktur.kontorer),
    );
  }
  if (avtale.kontorstruktur.length === 1) {
    return splitNavEnheterByType(avtale.kontorstruktur[0].kontorer);
  }
  return { navKontorEnheter: [], navAndreEnheter: [] };
}

function defaultArrangor(
  avtale: AvtaleDto,
  gjennomforing?: Partial<GjennomforingDto>,
): string | undefined {
  if (gjennomforing?.arrangor?.id) {
    return gjennomforing.arrangor.id;
  }

  if (avtale.arrangor?.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return undefined;
}

export function defaultGjennomforingData(
  ansatt: NavAnsattDto,
  avtale: AvtaleDto,
  gjennomforing?: Partial<GjennomforingDto>,
): Partial<InferredGjennomforingSchema> {
  const { navKontorEnheter, navAndreEnheter } = defaultNavEnheter(avtale, gjennomforing);

  // TODO: Fjern casting når avtaler er migrert til @tiltaksadministrasjon/api-client
  const faneInnhold = slateFaneinnholdToPortableText(
    (gjennomforing?.faneinnhold ?? avtale.faneinnhold) as NyFaneInnhold | null,
  );
  return {
    navn: gjennomforing?.navn || avtale.navn,
    avtaleId: avtale.id,
    navRegioner: defaultNavRegion(avtale, gjennomforing),
    navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
    navEnheterAndre: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
    administratorer: gjennomforing?.administratorer?.map((admin) => admin.navIdent) || [
      ansatt.navIdent,
    ],
    antallPlasser: gjennomforing?.antallPlasser,
    startOgSluttDato: {
      startDato: gjennomforing?.startDato
        ? gjennomforing.startDato
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.startDato
          : "",
      sluttDato: gjennomforing
        ? gjennomforing.sluttDato
        : defaultOppstartType(avtale) === GjennomforingOppstartstype.LOPENDE
          ? avtale.sluttDato
          : undefined,
    },
    arrangorId: defaultArrangor(avtale, gjennomforing),
    oppstart: gjennomforing?.oppstart || defaultOppstartType(avtale),
    kontaktpersoner: gjennomforing?.kontaktpersoner ?? [],
    stedForGjennomforing: gjennomforing?.stedForGjennomforing ?? null,
    arrangorKontaktpersoner:
      gjennomforing?.arrangor?.kontaktpersoner.map((p: ArrangorKontaktperson) => p.id) ?? [],
    beskrivelse: gjennomforing?.beskrivelse ?? avtale.beskrivelse,
    faneinnhold: faneInnhold,
    opphav: gjennomforing?.opphav ?? Opphav.TILTAKSADMINISTRASJON,
    deltidsprosent: gjennomforing?.deltidsprosent ?? 100,
    visEstimertVentetid: !!gjennomforing?.estimertVentetid?.enhet,
    estimertVentetid: gjennomforing?.estimertVentetid ?? null,
    tilgjengeligForArrangorDato: gjennomforing?.tilgjengeligForArrangorDato ?? null,
    amoKategorisering:
      gjennomforing?.amoKategorisering ??
      (avtale.amoKategorisering as AmoKategorisering | undefined) ??
      undefined,
    utdanningslop: gjennomforing?.utdanningslop
      ? toUtdanningslopDbo(gjennomforing.utdanningslop)
      : avtale.utdanningslop
        ? toUtdanningslopDbo(avtale.utdanningslop)
        : undefined,
  };
}

function toUtdanningslopDbo(data: Utdanningslop): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}
