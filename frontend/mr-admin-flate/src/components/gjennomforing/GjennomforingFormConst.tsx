import { splitNavEnheterByType, TypeSplittedNavEnheter } from "@/api/enhet/helpers";
import {
  AmoKategorisering,
  AvtaleDto,
  GjennomforingDto,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  GjennomforingRequest,
  GjennomforingVeilederinfoDto,
  NavAnsattDto,
  PrismodellDto,
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

type GjennomforingFormDefaultValues = Partial<{
  gjennomforing: Partial<GjennomforingDto>;
  veilederinfo: Partial<GjennomforingVeilederinfoDto> | null;
  prismodell: PrismodellDto | null;
  amoKategorisering: AmoKategorisering | null;
  utdanningslop: UtdanningslopDto | null;
}>;

export function defaultGjennomforingData(
  ansatt: NavAnsattDto,
  tiltakstype: TiltakstypeDto,
  avtale: AvtaleDto,
  detaljer: GjennomforingFormDefaultValues = {},
): DeepPartial<GjennomforingRequest> {
  const { gjennomforing, veilederinfo, amoKategorisering, utdanningslop, prismodell } = detaljer;
  const { navKontorEnheter, navAndreEnheter } = defaultNavEnheter(avtale, veilederinfo);

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
    kontaktpersoner: veilederinfo?.kontaktpersoner ?? [],
    arrangorKontaktpersoner: gjennomforing?.arrangor?.kontaktpersoner.map((p) => p.id) ?? [],
    veilederinformasjon: {
      navRegioner: defaultNavRegion(avtale, veilederinfo),
      navKontorer: navKontorEnheter.map((enhet) => enhet.enhetsnummer),
      navAndreEnheter: navAndreEnheter.map((enhet) => enhet.enhetsnummer),
      beskrivelse: veilederinfo?.beskrivelse ?? avtale.beskrivelse,
      faneinnhold: veilederinfo?.faneinnhold ?? avtale.faneinnhold,
    },
    deltidsprosent: gjennomforing?.deltidsprosent ?? 100,
    estimertVentetid: veilederinfo?.estimertVentetid || null,
    tilgjengeligForArrangorDato: gjennomforing?.tilgjengeligForArrangorDato ?? null,
    amoKategorisering: amoKategorisering
      ? amoKategoriseringRequest(amoKategorisering)
      : amoKategoriseringRequest(avtale.amoKategorisering),
    utdanningslop: utdanningslop
      ? toUtdanningslopDbo(utdanningslop)
      : avtale.utdanningslop
        ? toUtdanningslopDbo(avtale.utdanningslop)
        : null,
    oppmoteSted: veilederinfo?.oppmoteSted ?? null,
    pameldingType: gjennomforing?.pameldingType || defaultPameldingType(oppstart),
    prismodellId: prismodell?.id ?? avtale.prismodeller[0]?.id,
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

function defaultNavRegion(
  avtale: AvtaleDto,
  veilederinfo?: Partial<GjennomforingVeilederinfoDto> | null,
): string[] {
  if (veilederinfo?.kontorstruktur) {
    return veilederinfo.kontorstruktur.map((struktur) => struktur.region.enhetsnummer);
  }
  if (avtale.kontorstruktur.length === 1) {
    return avtale.kontorstruktur.map((struktur) => struktur.region.enhetsnummer);
  }
  return [];
}

function defaultNavEnheter(
  avtale: AvtaleDto,
  veilederinfo?: Partial<GjennomforingVeilederinfoDto> | null,
): TypeSplittedNavEnheter {
  if (veilederinfo?.kontorstruktur) {
    return splitNavEnheterByType(
      veilederinfo.kontorstruktur.flatMap((struktur) => struktur.kontorer),
    );
  }
  if (avtale.kontorstruktur.length === 1) {
    return splitNavEnheterByType(avtale.kontorstruktur[0].kontorer);
  }
  return { navKontorEnheter: [], navAndreEnheter: [] };
}
