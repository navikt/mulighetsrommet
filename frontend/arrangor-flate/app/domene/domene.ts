import {
  Betalingsinformasjon,
  RefusjonKravAftBeregning,
  RefusjonKravDeltakelse,
} from "@mr/api-client-v2";

export interface Refusjonskrav {
  id: string;
  fristForGodkjenning: string;
  detaljer: {
    tiltaksnavn: string;
    tiltakstype: string;
    refusjonskravperiode: string;
  };
  beregning: RefusjonKravAftBeregning;
  deltakere: RefusjonKravDeltakelse[];
  betalingsinformasjon: Betalingsinformasjon;
}
