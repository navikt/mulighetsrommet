import { splitNavEnheterByType, TypeSplittedNavEnheter } from "@/api/enhet/helpers";
import {
  AvtaleDto,
  GjennomforingAvtaleDto,
  GjennomforingDto,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  GjennomforingVeilederinfoDto,
  NavAnsattDto,
  OpplaringKategoriseringDetaljer,
  PrismodellDto,
  Tiltakskode,
  TiltakstypeDto,
  Utdanningslop,
  UtdanningslopDetaljer,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { kreverDirekteVedtak } from "@/utils/tiltakstype";
import { GjennomforingFormValues } from "@/pages/gjennomforing/form/validation";
import { toAmoKategoriseringRequest } from "@/pages/avtaler/form/mappers";

export function defaultGjennomforingData(
  ansatt: NavAnsattDto,
  tiltakstype: TiltakstypeDto,
  avtale: AvtaleDto,
  gjennomforing: Partial<GjennomforingAvtaleDto> | null,
  veilederinfo: Partial<GjennomforingVeilederinfoDto> | null,
  prismodell: PrismodellDto | null,
  opplaring: OpplaringKategoriseringDetaljer | null,
): DeepPartial<GjennomforingFormValues> {
  const { navKontorEnheter, navAndreEnheter } = defaultNavEnheter(avtale, veilederinfo);

  const defaultOppstart = getDefaultOppstart(tiltakstype);
  const oppstart = gjennomforing?.oppstart || defaultOppstart;
  const effectiveOpplaring = opplaring ?? avtale.opplaring ?? null;
  return {
    navn: gjennomforing?.navn || avtale.navn,
    administratorer: gjennomforing?.administratorer?.map((admin) => admin.navIdent) || [
      ansatt.navIdent,
    ],
    antallPlasser: gjennomforing?.antallPlasser,
    startDato: gjennomforing
      ? gjennomforing.startDato
      : defaultOppstart === GjennomforingOppstartstype.LOPENDE
        ? avtale.startDato
        : undefined,
    sluttDato: gjennomforing
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
      kontaktpersoner: veilederinfo?.kontaktpersoner ?? [],
    },
    deltidsprosent: gjennomforing?.deltidsprosent ?? 100,
    tilgjengeligForArrangorDato: gjennomforing?.tilgjengeligForArrangorDato ?? null,
    amoKategorisering: toAmoKategoriseringRequest(effectiveOpplaring),
    utdanningslop: effectiveOpplaring?.utdanningslop
      ? toUtdanningslopDbo(effectiveOpplaring.utdanningslop)
      : null,
    oppmoteSted: oppmoteSted(tiltakstype.tiltakskode, veilederinfo),
    pameldingType: gjennomforing?.pameldingType || getDefaultPameldingType(oppstart),
    prismodellId: prismodell?.id ?? avtale.prismodeller[0]?.id,
  };
}

function defaultArrangor(
  avtale: AvtaleDto,
  gjennomforing?: Partial<GjennomforingDto> | null,
): string | undefined {
  if (gjennomforing?.arrangor?.id) {
    return gjennomforing.arrangor.id;
  }

  if (avtale.arrangor?.underenheter.length === 1) {
    return avtale.arrangor.underenheter[0].id;
  }

  return undefined;
}

function oppmoteSted(
  tiltakskode: Tiltakskode,
  veilederinfo: Partial<GjennomforingVeilederinfoDto> | null,
): string | null {
  if (tiltakskode === Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER) {
    return null;
  }
  return veilederinfo?.oppmoteSted ?? null;
}

function toUtdanningslopDbo(data: UtdanningslopDetaljer): Utdanningslop {
  return {
    utdanningsprogram: data.utdanningsprogram.id,
    utdanninger: data.utdanninger.map((utdanning) => utdanning.id),
  };
}

function getDefaultOppstart(tiltakstype: TiltakstypeDto): GjennomforingOppstartstype {
  return kreverDirekteVedtak(tiltakstype)
    ? GjennomforingOppstartstype.LOPENDE
    : GjennomforingOppstartstype.FELLES;
}

function getDefaultPameldingType(oppstart: GjennomforingOppstartstype): GjennomforingPameldingType {
  return oppstart === GjennomforingOppstartstype.FELLES
    ? GjennomforingPameldingType.TRENGER_GODKJENNING
    : GjennomforingPameldingType.DIREKTE_VEDTAK;
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
