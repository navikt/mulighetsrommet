import { BrukerIkkeUnderOppfolgingVarsel } from "./BrukerIkkeUnderOppfolgingVarsel";
import { BrukerUnderOppfolgingMenMangler14aVedtakVarsel } from "./BrukerUnderOppfolgingMenMangler14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";

export function ModiaOversiktBrukerVarsler() {
  const bruker = useHentBrukerdata();
  return (
    <>
      <BrukerUnderOppfolgingMenMangler14aVedtakVarsel brukerdata={bruker.data} />
      <BrukersOppfolgingsenhetVarsel brukerdata={bruker.data} />
      <BrukerIkkeUnderOppfolgingVarsel brukerdata={bruker.data} />
    </>
  );
}
