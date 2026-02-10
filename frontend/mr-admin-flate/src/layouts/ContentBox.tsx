import { Box } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function ContentBox({ children }: Props) {
  return (
    <Box marginBlock="space-16" marginInline="space-8">
      {children}
    </Box>
  );
}
