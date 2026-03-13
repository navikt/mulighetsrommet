import { Box, Heading, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";

interface HeaderBannerProps {
  heading: string;
  ikon?: ReactNode;
}

export function HeaderBanner({ heading, ikon }: HeaderBannerProps) {
  return (
    <Box background="default" padding="space-8">
      <HStack align="center" justify="start" gap="space-8" wrap>
        {ikon ? <span>{ikon}</span> : null}
        <Heading level="2" size="large" data-testid={`header_${kebabCase(heading)}`}>
          {heading}
        </Heading>
      </HStack>
    </Box>
  );
}
