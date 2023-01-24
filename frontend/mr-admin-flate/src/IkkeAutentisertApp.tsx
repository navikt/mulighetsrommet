import { Alert, BodyShort, Heading } from "@navikt/ds-react";

export default function IkkeAutentisertApp() {
  return (
    <Alert variant="error">
      <Heading size="large">Ingen tilgang</Heading>
      <BodyShort>Din bruker har ikke tilgang til denne løsningen.</BodyShort>
    </Alert>
  );
}
