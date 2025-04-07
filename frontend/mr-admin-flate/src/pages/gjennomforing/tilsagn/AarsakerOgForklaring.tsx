import { Alert, BodyShort, Heading, List } from "@navikt/ds-react";

export function AarsakerOgForklaring({
  heading,
  tekst,
  aarsaker,
  forklaring,
}: {
  heading: string;
  tekst?: string;
  aarsaker: string[];
  forklaring?: string;
}) {
  return (
    <Alert size="small" variant="warning">
      <Heading spacing size="small" level="4">
        {heading}
      </Heading>
      {tekst && <BodyShort>{tekst}</BodyShort>}
      <List>
        {aarsaker.map((aarsak) => (
          <List.Item key={aarsak}>{aarsak}</List.Item>
        ))}
      </List>
      {forklaring && (
        <BodyShort>
          <b>Forklaring:</b> {forklaring}
        </BodyShort>
      )}
    </Alert>
  );
}
