import { Alert, Heading } from "@navikt/ds-react";

export function IkkeAutentisertApp() {
  return (
    <Alert variant="error">
      <Heading size="large">Ingen tilgang</Heading>
      <p>Din bruker har ikke tilgang til denne løsningen.</p>
    </Alert>
  );
}
