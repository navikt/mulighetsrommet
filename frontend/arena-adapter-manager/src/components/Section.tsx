import { Box, Heading, HStack, Loader, VStack } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

interface SectionProps extends PropsWithChildren {
  headerText: string;
  isLoading: boolean;
  loadingText: string;
}

export function Section({ children, headerText, isLoading, loadingText }: SectionProps) {
  return (
    <Box width="100%">
      <Heading size="medium" spacing>
        {headerText}
      </Heading>
      <Box padding="5" borderWidth="1" borderRadius="medium">
        {isLoading ? (
          <HStack gap="4">
            <Heading size="small">{loadingText}</Heading>
            <Loader />
          </HStack>
        ) : (
          <>
            <VStack gap="4">{children}</VStack>
          </>
        )}
      </Box>
    </Box>
  );
}
