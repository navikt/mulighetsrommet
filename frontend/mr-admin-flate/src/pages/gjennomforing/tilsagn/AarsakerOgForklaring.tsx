import { MetadataFritekstfelt } from "@mr/frontend-common/components/datadriven/Metadata";
import { BodyShort, Heading, List, Box, LocalAlert } from "@navikt/ds-react";

type Props = {
  heading: string;
  ingress?: string;
  tekster?: string[];
  aarsaker: string[];
  forklaring: string | null | undefined;
};

export function AarsakerOgForklaring({ heading, ingress, tekster, aarsaker, forklaring }: Props) {
  return (
    <LocalAlert status="warning">
      <LocalAlert.Header>
        <LocalAlert.Title>{heading}</LocalAlert.Title>
      </LocalAlert.Header>
      <LocalAlert.Content>
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
        <Box asChild>
          <List data-aksel-migrated-v8 as="ul" size="medium">
            {aarsaker.map((aarsak) => (
              <List.Item key={aarsak}>{aarsak}</List.Item>
            ))}
          </List>
        </Box>
        {forklaring && <MetadataFritekstfelt label="Forklaring" value={forklaring} />}
      </LocalAlert.Content>
    </LocalAlert>
  );
}
