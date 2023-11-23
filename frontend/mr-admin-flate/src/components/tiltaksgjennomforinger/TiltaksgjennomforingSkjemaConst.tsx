import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import {
  Avtale,
  Opphav,
  Tilgjengelighetsstatus,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktpersoner,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import { TiltaksgjennomforingUtkastData } from "./TiltaksgjennomforingSkjemaPage";
import { InferredTiltaksgjennomforingSchema } from "./TiltaksgjennomforingSchema";
import { DeepPartial } from "react-hook-form";

export function defaultOppstartType(avtale?: Avtale): TiltaksgjennomforingOppstartstype {
  if (!avtale) {
    return TiltaksgjennomforingOppstartstype.LOPENDE;
  }

  const tiltakskode = avtale.tiltakstype.arenaKode;
  return isTiltakMedFellesOppstart(tiltakskode)
    ? TiltaksgjennomforingOppstartstype.FELLES
    : TiltaksgjennomforingOppstartstype.LOPENDE;
}

export function defaultValuesForKontaktpersoner(
  kontaktpersoner?: TiltaksgjennomforingKontaktpersoner[],
): TiltaksgjennomforingKontaktpersoner[] {
  if (!kontaktpersoner) return [{ navIdent: "", navEnheter: [] }];

  return kontaktpersoner?.map((person) => ({
    navIdent: person.navIdent,
    navEnheter: person.navEnheter,
  }));
}

export const erArenaOpphav = (tiltaksgjennomforing: Tiltaksgjennomforing | undefined) => {
  return tiltaksgjennomforing?.opphav === Opphav.ARENA;
};

export const arrangorUnderenheterOptions = (avtale: Avtale) => {
  return (avtale?.leverandorUnderenheter ?? []).map((lev) => {
    return {
      label: `${lev.navn} - ${lev.organisasjonsnummer}`,
      value: lev.organisasjonsnummer,
    };
  });
};

export function utkastDataEllerDefault(
  avtale: Avtale,
  utkast?: TiltaksgjennomforingUtkastData,
  tiltaksgjennomforing?: Tiltaksgjennomforing,
): DeepPartial<InferredTiltaksgjennomforingSchema> {
  return {
    navn: tiltaksgjennomforing?.navn,
    avtaleId: avtale.id,
    navRegion: tiltaksgjennomforing?.navRegion?.enhetsnummer,
    navEnheter: (tiltaksgjennomforing?.navEnheter?.map((enhet) => enhet.enhetsnummer) || []) as [
      string,
      ...string[],
    ],
    administratorer: tiltaksgjennomforing?.administratorer?.map((admin) => admin.navIdent),
    antallPlasser: tiltaksgjennomforing?.antallPlasser,
    startOgSluttDato: {
      startDato: tiltaksgjennomforing?.startDato,
      sluttDato: tiltaksgjennomforing?.sluttDato,
    },
    tiltaksArrangorUnderenhetOrganisasjonsnummer:
      tiltaksgjennomforing?.arrangor?.organisasjonsnummer || "",
    midlertidigStengt: {
      erMidlertidigStengt: Boolean(tiltaksgjennomforing?.stengtFra),
      stengtFra: tiltaksgjennomforing?.stengtFra
        ? new Date(tiltaksgjennomforing.stengtFra)
        : undefined,
      stengtTil: tiltaksgjennomforing?.stengtTil
        ? new Date(tiltaksgjennomforing.stengtTil)
        : undefined,
    },
    oppstart: tiltaksgjennomforing?.oppstart || defaultOppstartType(avtale),
    apenForInnsok: tiltaksgjennomforing?.tilgjengelighet !== Tilgjengelighetsstatus.STENGT,
    kontaktpersoner: defaultValuesForKontaktpersoner(tiltaksgjennomforing?.kontaktpersoner),
    stedForGjennomforing: tiltaksgjennomforing?.stedForGjennomforing,
    arrangorKontaktpersonId: tiltaksgjennomforing?.arrangor?.kontaktperson?.id,
    beskrivelse: tiltaksgjennomforing?.beskrivelse ?? null,
    faneinnhold: tiltaksgjennomforing?.faneinnhold ?? {
      forHvem: null,
      forHvemInfoboks: null,
      pameldingOgVarighet: null,
      pameldingOgVarighetInfoboks: null,
      detaljerOgInnhold: null,
      detaljerOgInnholdInfoboks: null,
    },
    opphav: tiltaksgjennomforing?.opphav ?? Opphav.MR_ADMIN_FLATE,
    ...utkast,
  };
}
