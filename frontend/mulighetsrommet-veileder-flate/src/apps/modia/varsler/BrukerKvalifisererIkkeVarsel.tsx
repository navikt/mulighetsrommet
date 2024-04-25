import { Alert } from "@navikt/ds-react";
import { Bruker, VeilederflateTiltakstype } from "mulighetsrommet-api-client";

interface Props {
  brukerdata: Bruker;
  brukerHarRettPaaTiltak: boolean;
  tiltakstype: VeilederflateTiltakstype;
  brukerErUnderOppfolging: boolean;
}

export function BrukerKvalifisererIkkeVarsel({
  brukerHarRettPaaTiltak,
  brukerdata,
  tiltakstype,
  brukerErUnderOppfolging,
}: Props) {
  const innsatsgruppeEllerTiltakstype = tiltakstype.innsatsgruppe?.tittel ?? tiltakstype.navn;

  return !brukerHarRettPaaTiltak && brukerErUnderOppfolging && brukerdata.innsatsgruppe ? (
    <Alert variant="warning">
      Brukeren tilhører innsatsgruppen{" "}
      <strong>{brukerdata.innsatsgruppe.replaceAll("_", " ").toLocaleLowerCase()}</strong>, men
      tiltaksgjennomføringen gjelder for{" "}
      <strong>{innsatsgruppeEllerTiltakstype.toLocaleLowerCase()}</strong>.
    </Alert>
  ) : null;
}
