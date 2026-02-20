import { Brukerdata, Innsatsgruppe } from "@api-client";
import { Melding } from "@/components/melding/Melding";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukerErSykmeldtMedArbeidsgiverVarsel({ brukerdata }: Props) {
  return brukerdata.erSykmeldtMedArbeidsgiver &&
    brukerdata.innsatsgruppe === Innsatsgruppe.GODE_MULIGHETER ? (
    <Melding header="Bruker er sykmeldt med arbeidsgiver" variant="info">
      Bruker er sykmeldt med arbedsgiver og kan derfor også meldes på Arbeidsrettet rehabilitering.
    </Melding>
  ) : null;
}
