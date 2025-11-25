import { Heading, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";

interface HeaderBannerProps {
  heading: string;
  ikon?: ReactNode;
}

export function HeaderBanner({ heading, ikon }: HeaderBannerProps) {
  return (
    <div className="bg-white p-2">
      <HStack align="center" justify="start" gap="2" wrap>
        {ikon ? <span>{ikon}</span> : null}
        <Heading level="2" size="large" data-testid={`header_${kebabCase(heading)}`}>
          {heading}
        </Heading>
      </HStack>
    </div>
  );
}
