import { Box, VStack } from "@navikt/ds-react";

export function UthevetBox({ children }: { children: React.ReactNode }) {
  return (
    <Box
      asChild
      width="100%"
      background="sunken"
      borderColor="neutral"
      borderWidth="1"
      padding="space-16"
      borderRadius="4"
    >
      <VStack gap="space-20">{children}</VStack>
    </Box>
  );
}
