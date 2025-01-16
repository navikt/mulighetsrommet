import { Heading, HStack } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";

interface HeaderBannerProps {
  heading: string;
  harUndermeny?: boolean;
  ikon?: ReactNode;
}

export function HeaderBanner({ heading, harUndermeny = false, ikon }: HeaderBannerProps) {
  return (
    <div className={classNames("bg-white p-2", !harUndermeny && "border-b-border-divider")}>
      <HStack align="center" justify="start" gap="2" wrap>
        {ikon ? <span>{ikon}</span> : null}
        <Heading level="2" size="large" data-testid={`header_${kebabCase(heading)}`}>
          {heading}
        </Heading>
      </HStack>
    </div>
  );
}
