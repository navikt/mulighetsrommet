import { Alert } from "@navikt/ds-react";
import { Bruker } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
  brukerHarRettPaaTiltak: boolean;
  brukerErUnderOppfolging: boolean;
}

export function BrukerKvalifisererIkkeVarsel({
  brukerHarRettPaaTiltak,
  brukerdata,
  brukerErUnderOppfolging,
}: Props) {
  return !brukerHarRettPaaTiltak && brukerErUnderOppfolging && brukerdata.innsatsgruppe ? (
    <Alert variant="warning">
      Brukeren tilhører innsatsgruppen{" "}
      <strong>{brukerdata.innsatsgruppe.replaceAll("_", " ").toLocaleLowerCase()}</strong> og
      kvalifiserer derfor ikke for denne tiltaksgjennomføringen.
    </Alert>
  ) : null;
}
