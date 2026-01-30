import { splitNavEnheterByType, TypeSplittedNavEnheter } from "@/api/enhet/helpers";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  GjennomforingRequest,
  NavAnsattDto,
  TiltakstypeDto,
  UtdanningslopDbo,
  UtdanningslopDto,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { amoKategoriseringRequest } from "@/schemas/avtale";
import { kanEndreOppstartOgPamelding } from "@/utils/tiltakstype";

export function defaultPameldingType(
  oppstart: GjennomforingOppstartstype,
): GjennomforingPameldingType {
  return oppstart === GjennomforingOppstartstype.FELLES
    ? GjennomforingPameldingType.TRENGER_GODKJENNING
    : GjennomforingPameldingType.DIREKTE_VEDTAK;
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
): string | null {
  if (gjennomforing?.arrangor?.id) {
    return gjennomforing.arrangor.id;
  }

  if (avtale.arrangor?.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return null;
}

export function defaultGjennomforingData(
  ansatt: NavAnsattDto,
  tiltakstype: TiltakstypeDto,
  avtale: AvtaleDto,
  gjennomforing?: Partial<GjennomforingDto>,
): DeepPartial<GjennomforingRequest> {
  const { navKontorEnheter, navAndreEnheter } = defaultNavEnheter(avtale, gjennomforing);

  const faneInnhold = gjennomforing?.faneinnhold ?? avtale.faneinnhold;

  const defaultOppstart = getDefaultOppstart(tiltakstype);
  const oppstart = gjennomforing?.oppstart || defaultOppstart;
  return {
    navn: gjennomforing?.navn || avtale.navn,
    avtaleId: avtale.id,
    administratorer: gjennomforing?.administratorer?.map((admin) => admin.navIdent) || [
      ansatt.navIdent,
    ],
    antallPlasser: gjennomforing?.antallPlasser ?? null,
    startDato: gjennomforing?.startDato
      ? gjennomforing.startDato
      : defaultOppstart === GjennomforingOppstartstype.LOPENDE
        ? avtale.startDato
        : null,
    sluttDato: gjennomforing?.sluttDato
      ? gjennomforing.sluttDato
      : defaultOppstart === GjennomforingOppstartstype.LOPENDE
        ? avtale.sluttDato
        : null,
    arrangorId: defaultArrangor(avtale, gjennomforing),
    oppstart,
    kontaktpersoner: gjennomforing?.kontaktpersoner ?? [],
    arrangorKontaktpersoner: gjennomforing?.arrangor?.kontaktpersoner.map((p) => p.id) ?? [],
    veilederinformasjon: {
      navRegioner: defaultNavRegion(avtale, gjennomforing),
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      beskrivelse: gjennomforing?.beskrivelse ?? avtale.beskrivelse,
      faneinnhold: faneInnhold,
    },
    deltidsprosent: gjennomforing?.deltidsprosent ?? 100,
    estimertVentetid: gjennomforing?.estimertVentetid || null,
    tilgjengeligForArrangorDato: gjennomforing?.tilgjengeligForArrangorDato ?? null,
    amoKategorisering: gjennomforing?.amoKategorisering
      ? amoKategoriseringRequest(gjennomforing.amoKategorisering)
      : amoKategoriseringRequest(avtale.amoKategorisering),
    utdanningslop: gjennomforing?.utdanningslop
      ? toUtdanningslopDbo(gjennomforing.utdanningslop)
      : avtale.utdanningslop
        ? toUtdanningslopDbo(avtale.utdanningslop)
        : null,
    oppmoteSted: gjennomforing?.oppmoteSted ?? null,
    pameldingType: gjennomforing?.pameldingType || defaultPameldingType(oppstart),
    prismodellId: gjennomforing?.prismodell?.id ?? avtale.prismodeller[0]?.id,
  };
}

function toUtdanningslopDbo(data: UtdanningslopDto): UtdanningslopDbo {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}

function getDefaultOppstart(tiltakstype: TiltakstypeDto): GjennomforingOppstartstype {
  return kanEndreOppstartOgPamelding(tiltakstype)
    ? GjennomforingOppstartstype.FELLES
    : GjennomforingOppstartstype.LOPENDE;
}
