import {
  Betalingsinformasjon,
  RefusjonKravAftBeregning,
  RefusjonKravDeltakelse,
} from "@mr/api-client";

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
  deltakere: RefusjonKravDeltakelse[];
  betalingsinformasjon: Betalingsinformasjon;
}

export type TilsagnDetaljer = {
  antallPlasser: number;
  prisPerPlass: number;
  tilsagnsBelop: number;
  tilsagnsPeriode: string;
  sum: number;
};

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
