import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import { BodyShort, Box, HStack } from "@navikt/ds-react";
import { ValutaBelop } from "@tiltaksadministrasjon/api-client";

interface Props {
  belop: ValutaBelop;
  label: string;
}

export function TotaltBelopBox({ label, belop }: Props) {
  return (
    <Box
      className="w-full"
      borderWidth="2"
      borderRadius="8"
      borderColor="neutral-subtle"
      padding="space-8"
    >
      <HStack justify="space-between">
        <HStack align="center" gap="space-8">
          <PadlockLockedIcon fontSize="1.5rem" />
          <BodyShort size="medium">{label}</BodyShort>
        </HStack>
        <BodyShort size="large">{formaterValutaBelop(belop)}</BodyShort>
      </HStack>
    </Box>
  );
}
