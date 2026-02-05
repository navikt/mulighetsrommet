import { Box } from "@navikt/ds-react";
import { ReactNode } from "react";

export function WhitePaddedBox({ children }: { children: ReactNode }) {
  return (
    <Box padding="space-16" background="default">
      {children}
    </Box>
  );
}
