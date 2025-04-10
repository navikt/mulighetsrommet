import { Alert, BodyShort, Heading, List } from "@navikt/ds-react";

export function AarsakerOgForklaring({
  heading,
  tekster,
  aarsaker,
  forklaring,
}: {
  heading: string;
  tekster?: string[];
  aarsaker: string[];
  forklaring?: string;
}) {
  return (
    <Alert size="small" variant="warning">
      <Heading spacing size="small" level="4">
        {heading}
      </Heading>
      {tekster && tekster.map((tekst, index) => <BodyShort key={index}>{tekst}</BodyShort>)}
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
