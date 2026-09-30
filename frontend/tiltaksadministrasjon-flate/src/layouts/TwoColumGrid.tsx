import { Box, HGrid, Hide } from "@navikt/ds-react";
import { ReactNode } from "react";
import React from "react";

interface Props {
  separator?: boolean;
  children: ReactNode;
}

export function TwoColumnGrid(props: Props) {
  const [leftChild, ...rightChildren] = React.Children.toArray(props.children);
  const { separator = false } = props;

  return (
    <HGrid gap="space-36" height="full" columns={{ xs: 1, lg: "1fr 1fr" }}>
      <Box>
        {/* Left Column Content */}
        {leftChild}
      </Box>
      <Box paddingInline={{ lg: "space-36 space-0", xs: "space-0" }} className="relative">
        {/* Right Column Content */}
        {separator && (
          <Hide below="md" asChild>
            <Box
              position="absolute"
              marginInline={{ md: "space-4" }}
              width="1px"
              className="inset-y-0 left-0 bg-(--ax-bg-neutral-moderate-pressed) "
            />
          </Hide>
        )}
        {...rightChildren}
      </Box>
    </HGrid>
  );
}
