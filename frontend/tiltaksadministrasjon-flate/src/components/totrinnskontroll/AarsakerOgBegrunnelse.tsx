import { MetadataFritekstfelt } from "@mr/frontend-common/components/datadriven/Metadata";
import { BodyShort, Heading, List, Box, InfoCard } from "@navikt/ds-react";

type Props = {
  heading: string;
  ingress?: string;
  tekster?: string[];
  aarsaker: string[];
  begrunnelse: string | null | undefined;
};

export function AarsakerOgBegrunnelse({ heading, ingress, tekster, aarsaker, begrunnelse }: Props) {
  return (
    <InfoCard data-color="warning">
      <InfoCard.Header>
        <InfoCard.Title>{heading}</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
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
        {begrunnelse && <MetadataFritekstfelt label="Begrunnelse" value={begrunnelse} />}
      </InfoCard.Content>
    </InfoCard>
  );
}
