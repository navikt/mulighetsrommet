import {
  Betalingsinformasjon,
  RefusjonKravAftBeregning,
  RefusjonKravDeltakelse,
} from "@mr/api-client";

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
