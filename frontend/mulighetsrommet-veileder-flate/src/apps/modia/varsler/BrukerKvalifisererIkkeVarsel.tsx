import { Brukerdata } from "@api-client";
import { Melding } from "@/components/melding/Melding";

interface Props {
  brukerdata: Brukerdata;
  brukerHarRettPaaTiltak: boolean;
}

export function BrukerKvalifisererIkkeVarsel({ brukerHarRettPaaTiltak, brukerdata }: Props) {
  return !brukerHarRettPaaTiltak && brukerdata.erUnderOppfolging && brukerdata.innsatsgruppe ? (
    <Melding header="Bruker kvalifiserer ikke til tiltaket" variant="warning">
      Brukeren tilhører innsatsgruppen{" "}
      <strong>{brukerdata.innsatsgruppe.replaceAll("_", " ").toLocaleLowerCase()}</strong> og kan
      ikke delta på dette tiltaket uten at det gjøres en ny behovsvurdering.
    </Melding>
  ) : null;
}
