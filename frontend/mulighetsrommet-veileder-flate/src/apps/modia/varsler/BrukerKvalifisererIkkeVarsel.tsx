import { Alert } from "@navikt/ds-react";
import { Brukerdata } from "@api-client";

interface Props {
  brukerdata: Brukerdata;
  brukerHarRettPaaTiltak: boolean;
}

export function BrukerKvalifisererIkkeVarsel({ brukerHarRettPaaTiltak, brukerdata }: Props) {
  return !brukerHarRettPaaTiltak && brukerdata.erUnderOppfolging && brukerdata.innsatsgruppe ? (
    <Alert variant="warning">
      Brukeren tilhører innsatsgruppen{" "}
      <strong>{brukerdata.innsatsgruppe.replaceAll("_", " ").toLocaleLowerCase()}</strong> og kan
      ikke delta på dette tiltaket uten at det gjøres en ny behovsvurdering.
    </Alert>
  ) : null;
}
