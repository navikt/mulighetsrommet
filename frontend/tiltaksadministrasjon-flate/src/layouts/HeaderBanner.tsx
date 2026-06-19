import { Box, Heading, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import { DataElementStatus } from "@mr/frontend-common/components/datadriven/types";
import { DataElementStatusTag } from "@mr/frontend-common";

interface HeaderBannerProps {
  heading: string;
  ikon?: ReactNode;
  status?: DataElementStatus;
}

export function HeaderBanner({ heading, ikon, status }: HeaderBannerProps) {
  return (
    <Box background="default" padding="space-8">
      <HStack align="center" justify="start" gap="space-8" wrap>
        {ikon ? <span>{ikon}</span> : null}
        <Heading level="2" size="large" data-testid={`header_${kebabCase(heading)}`}>
          {heading}
        </Heading>
        {status ? <DataElementStatusTag {...status} /> : null}
      </HStack>
    </Box>
  );
}
