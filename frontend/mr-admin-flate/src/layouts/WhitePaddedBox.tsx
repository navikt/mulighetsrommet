import { Box } from "@navikt/ds-react";
import { ReactNode } from "react";

export function WhitePaddedBox({ children }: { children: ReactNode }) {
  return (
    <Box padding="4" background="bg-default">
      {children}
    </Box>
  );
}
