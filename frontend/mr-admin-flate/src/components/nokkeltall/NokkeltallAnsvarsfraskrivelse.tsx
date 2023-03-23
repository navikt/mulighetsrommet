import { Alert, BodyShort } from "@navikt/ds-react";

export function NokkeltallAnsvarsfraskrivelse() {
  return (
    <Alert style={{ marginBottom: "1rem" }} variant="warning">
      <BodyShort>
        Tjenesten er under utvikling og tallene som vises her under nøkkeltall
        kan være feil eller misvisende pga. feil eller for dårlig datagrunnlag
      </BodyShort>
    </Alert>
  );
}
