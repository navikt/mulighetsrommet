import { PropsWithChildren } from "react";
import { Box } from "@navikt/ds-react";

export function TilskuddFormGroup({ children }: PropsWithChildren) {
  return (
    <Box width="100%" borderColor="neutral" borderWidth="2" padding="space-16" borderRadius="8">
      {children}
    </Box>
  );
}
