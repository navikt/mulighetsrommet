import { BodyShort, Box, InfoCard } from "@navikt/ds-react";
import { tekster } from "~/tekster";

interface IngenTreffProps {
  type: "utbetaling" | "tiltak" | "tilsagn";
}

export function IngenTreff({ type }: IngenTreffProps) {
  const { header, description } = tekster.bokmal.ingenTreff[type];
  return (
    <Box marginBlock="space-16">
      <InfoCard data-color="info" className="my-10">
        <InfoCard.Header>
          <InfoCard.Title>{header}</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort spacing>{description}</BodyShort>
          <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
        </InfoCard.Content>
      </InfoCard>
    </Box>
  );
}
