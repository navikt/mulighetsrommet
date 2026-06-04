import { BrukerErSykmeldtMedArbeidsgiverVarsel } from "./BrukerErSykmeldtMedArbeidsgiverVarsel";
import { BrukerIkkeUnderOppfolgingVarsel } from "./BrukerIkkeUnderOppfolgingVarsel";
import { BrukerUnderOppfolgingMenMangler14aVedtakVarsel } from "./BrukerUnderOppfolgingMenMangler14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";

export function ModiaOversiktBrukerVarsler() {
  const { data: brukerdata } = useBrukerdata();
  return (
    <>
      <BrukerUnderOppfolgingMenMangler14aVedtakVarsel brukerdata={brukerdata} />
      <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
      <BrukerIkkeUnderOppfolgingVarsel brukerdata={brukerdata} />
      <BrukerErSykmeldtMedArbeidsgiverVarsel brukerdata={brukerdata} />
    </>
  );
}
