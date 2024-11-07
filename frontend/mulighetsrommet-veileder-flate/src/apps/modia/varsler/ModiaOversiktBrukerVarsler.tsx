import { BrukerErSykmeldtMedArbeidsgiverVarsel } from "./BrukerErSykmeldtMedArbeidsgiverVarsel";
import { BrukerIkkeUnderOppfolgingVarsel } from "./BrukerIkkeUnderOppfolgingVarsel";
import { BrukerUnderOppfolgingMenMangler14aVedtakVarsel } from "./BrukerUnderOppfolgingMenMangler14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "./BrukersOppfolgingsenhetVarsel";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";

export function ModiaOversiktBrukerVarsler() {
  const { data: brukerdata } = useHentBrukerdata();
  return (
    <>
      <BrukerUnderOppfolgingMenMangler14aVedtakVarsel brukerdata={brukerdata} />
      <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
      <BrukerIkkeUnderOppfolgingVarsel brukerdata={brukerdata} />
      <BrukerErSykmeldtMedArbeidsgiverVarsel brukerdata={brukerdata} />
    </>
  );
}
