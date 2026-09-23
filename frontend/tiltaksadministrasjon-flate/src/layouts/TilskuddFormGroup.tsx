import { PropsWithChildren } from "react";
import { Box, VStack } from "@navikt/ds-react";

export function TilskuddFormGroup({ children }: PropsWithChildren) {
  return (
    <Box
      asChild
      width="100%"
      borderColor="neutral"
      borderWidth="2"
      padding="space-16"
      borderRadius="8"
    >
      <VStack>{children}</VStack>
    </Box>
  );
}
