import { MetadataFritekstfelt } from "@mr/frontend-common/components/datadriven/Metadata";
import { Alert, BodyShort, Heading, List, Box } from "@navikt/ds-react";

type Props = {
  heading: string;
  ingress?: string;
  tekster?: string[];
  aarsaker: string[];
  forklaring: string | null | undefined;
};

export function AarsakerOgForklaring({ heading, ingress, tekster, aarsaker, forklaring }: Props) {
  const aarsakHeading = "Årsaker:";
  const forklaringHeading = "Forklaring:";
  return (
    <Alert size="medium" variant="warning">
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
        {aarsakHeading}
      </Heading>
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 as="ul" size="small">
          {aarsaker.map((aarsak) => (
            <List.Item key={aarsak}>{aarsak}</List.Item>
          ))}
        </List>
      </Box>
      {forklaring && <MetadataFritekstfelt label={forklaringHeading} value={forklaring} />}
    </Alert>
  );
}
