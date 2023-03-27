import { Alert } from "@navikt/ds-react";

export const NokkeltallAlert = () => {
  return (
    <Alert variant="warning" style={{ marginBottom: "1rem" }}>
      Tjenesten er under utvikling og tallene som vises her under nøkkeltall kan
      være feil eller misvisende pga. feil eller for dårlig datagrunnlag.
    </Alert>
  );
};
