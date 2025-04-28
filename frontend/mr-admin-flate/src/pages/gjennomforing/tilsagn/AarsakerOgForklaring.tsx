import { Alert, BodyShort, Heading, List } from "@navikt/ds-react";

type Props = {
  heading: string;
  ingress?: string;
  tekster?: string[];
  aarsaker: string[];
  forklaring?: string;
};

export function AarsakerOgForklaring({ heading, ingress, tekster, aarsaker, forklaring }: Props) {
  return (
    <Alert size="small" variant="warning">
      <Heading size="small" level="4" className="mb-1">
        {heading}
      </Heading>
      {ingress && <BodyShort className="mb-6">{ingress}</BodyShort>}
      <div className="mb-6">
        {tekster && tekster.map((tekst, index) => <BodyShort key={index}>{tekst}</BodyShort>)}
      </div>
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
