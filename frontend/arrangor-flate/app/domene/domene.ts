import { RefusjonKravAftBeregning, RefusjonKravDeltakelsePeriode } from "@mr/api-client";

export interface Krav {
  id: string;
  kravnr: string;
  periode: string;
  belop: string;
  fristForGodkjenning: string;
}

export enum KravStatus {
  Attestert,
  KlarForInnsending,
  NarmerSegFrist,
}

export interface Refusjonskrav {
  id: string;
  detaljer: {
    tiltaksnavn: string;
    tiltakstype: string;
    refusjonskravperiode: string;
  };
  beregning: RefusjonKravAftBeregning;
  deltakere: Deltaker[];
}

export type TilsagnDetaljer = {
  antallPlasser: number;
  prisPerPlass: number;
  tilsagnsBelop: number;
  tilsagnsPeriode: string;
  sum: number;
};

export interface Deltaker {
  id: string;
  navn: string;
  norskIdent: string;
  veileder?: string;
  startDatoTiltaket?: string;
  startDatoPerioden?: string;
  sluttDatoPerioden?: string;
  stillingsprosent?: number;
  maanedsverk: number;
  perioder: RefusjonKravDeltakelsePeriode[];
}

export interface RolleTilgangRequest {
  personident: string;
}

export interface Rolletilgang {
  roller: TiltaksarrangorRoller[];
}

interface TiltaksarrangorRoller {
  organisasjonsnummer: string;
  roller: RolleType[];
}

export enum RolleType {
  "TILTAK_ARRANGOR_REFUSJON",
}
