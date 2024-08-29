import { Alert } from "@navikt/ds-react";
import { Bruker } from "@mr/api-client";

interface Props {
  brukerdata: Bruker;
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
