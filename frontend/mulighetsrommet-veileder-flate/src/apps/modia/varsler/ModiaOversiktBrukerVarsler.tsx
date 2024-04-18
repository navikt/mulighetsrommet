import { Bruker } from "mulighetsrommet-api-client";
import { BrukerHarVaertUnderOppfolgingVarsel } from "./BrukerHarVaertUnderOppfolgingVarsel";
import { BrukerIkkeUnderOppfolgingVarsel } from "./BrukerIkkeUnderOppfolgingVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";

interface Props {
  brukerdata: Bruker;
}

export function ModiaOversiktBrukerVarsler({ brukerdata }: Props) {
  return (
    <>
      <BrukerIkkeUnderOppfolgingVarsel brukerdata={brukerdata} />
      <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
      <BrukerHarVaertUnderOppfolgingVarsel brukerdata={brukerdata} />
    </>
  );
}
