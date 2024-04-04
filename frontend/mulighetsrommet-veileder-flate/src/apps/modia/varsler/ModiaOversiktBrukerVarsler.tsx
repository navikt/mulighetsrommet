import { Bruker } from "mulighetsrommet-api-client";
import { BrukerErIkkeUnderOppfolgingVarsel } from "./BrukerErIkkeUnderOppfolgingVarsel";
import { BrukerHarIkke14aVedtakVarsel } from "./BrukerHarIkke14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";

interface Props {
  brukerdata: Bruker;
}

export function ModiaOversiktBrukerVarsler({ brukerdata }: Props) {
  return (
    <>
      <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
      <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
      <BrukerErIkkeUnderOppfolgingVarsel brukerdata={brukerdata} />
    </>
  );
}
