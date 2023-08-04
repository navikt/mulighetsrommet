import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import {
  Avtale,
  NavEnhet,
  Opphav,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktpersoner,
  TiltaksgjennomforingOppstartstype,
  Virksomhet,
} from "mulighetsrommet-api-client";

export function defaultOppstartType(
  avtale?: Avtale,
): TiltaksgjennomforingOppstartstype {
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

export type UtkastData = Pick<
  Tiltaksgjennomforing,
  | "navn"
  | "antallPlasser"
  | "startDato"
  | "sluttDato"
  | "navEnheter"
  | "stengtFra"
  | "stengtTil"
  | "arrangor"
  | "kontaktpersoner"
  | "estimertVentetid"
  | "lokasjonArrangor"
> & {
  tiltakstypeId: string;
  avtaleId: string;
  arrangorKontaktpersonId?: { id?: string };
  id: string;
};

export const arenaOpphav = (
  tiltaksgjennomforing: Tiltaksgjennomforing | undefined,
) => {
  return tiltaksgjennomforing?.opphav === Opphav.ARENA;
};

export const enheterOptions = (
  enheter: NavEnhet[],
  avtale: Avtale | undefined,
) => {
  const options = enheter!
    .filter(
      (enhet: NavEnhet) =>
        avtale?.navRegion?.enhetsnummer === enhet.overordnetEnhet,
    )
    .filter(
      (enhet: NavEnhet) =>
        avtale?.navEnheter?.length === 0 ||
        avtale?.navEnheter.find(
          (e: any) => e.enhetsnummer === enhet.enhetsnummer,
        ),
    )
    .map((enhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));

  return options || [];
};

export const arrangorUnderenheterOptions = (
  avtale: Avtale,
  virksomhet: Virksomhet | undefined,
) => {
  const options =
    avtale?.leverandorUnderenheter.map((lev: any) => {
      return {
        label: `${lev.navn} - ${lev.organisasjonsnummer}`,
        value: lev.organisasjonsnummer,
      };
    }) || [];

  // Ingen underenheter betyr at alle er valgt, må gi valg om alle underenheter fra virksomhet
  if (options?.length === 0) {
    const enheter = virksomhet?.underenheter || [];
    return enheter.map((enhet) => ({
      value: enhet.organisasjonsnummer,
      label: `${enhet?.navn} - ${enhet?.organisasjonsnummer}`,
    }));
  }
  return options;
};
