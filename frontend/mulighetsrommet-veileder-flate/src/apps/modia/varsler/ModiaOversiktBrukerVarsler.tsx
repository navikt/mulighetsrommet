import { Bruker } from "mulighetsrommet-api-client";
import { BrukerIkkeUnderOppfolgingVarsel } from "./BrukerIkkeUnderOppfolgingVarsel";
import { BrukerUnderOppfolgingMenMangler14aVedtakVarsel } from "./BrukerUnderOppfolgingMenMangler14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";

interface Props {
  brukerdata: Bruker;
}

export function ModiaOversiktBrukerVarsler({ brukerdata }: Props) {
  return (
    <>
      <BrukerUnderOppfolgingMenMangler14aVedtakVarsel brukerdata={brukerdata} />
      <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
      <BrukerIkkeUnderOppfolgingVarsel brukerdata={brukerdata} />
    </>
  );
}
