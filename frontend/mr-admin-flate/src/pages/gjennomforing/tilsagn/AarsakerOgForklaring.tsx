import { Alert, BodyShort, Heading, HStack, List } from "@navikt/ds-react";

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
      <Heading spacing size="small" level="4">
        {heading}
      </Heading>
      {ingress && <BodyShort className="mb-4">{ingress}</BodyShort>}
      {tekster &&
        tekster.map((tekst, index) => (
          <BodyShort key={index} className="mb-4">
            {tekst}
          </BodyShort>
        ))}
      <Heading level="5" size="xsmall">
        Årsaker:
      </Heading>
      <List as="ul" size="small">
        {aarsaker.map((aarsak) => (
          <List.Item key={aarsak}>{aarsak}</List.Item>
        ))}
      </List>
      {forklaring && (
        <HStack gap="2">
          <Heading level="5" size="xsmall">
            Forklaring:
          </Heading>
          <BodyShort>{forklaring}</BodyShort>
        </HStack>
      )}
    </Alert>
  );
}
