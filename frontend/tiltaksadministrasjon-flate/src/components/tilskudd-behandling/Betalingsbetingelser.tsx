import { BodyShort, Box } from "@navikt/ds-react";

interface Props {
  prisbetingelser: string | null;
}

export function Betalingsbetingelser({ prisbetingelser }: Props) {
  return (
    <Box background="neutral-soft" padding="space-16" borderRadius="8">
      <BodyShort weight="semibold">Pris og betalingsbetingelser:</BodyShort>
      <BodyShort>{prisbetingelser}</BodyShort>
    </Box>
  );
}
