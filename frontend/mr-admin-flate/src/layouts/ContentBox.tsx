import { Box } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function ContentBox({ children }: Props) {
  return <Box marginBlock="4">{children}</Box>;
}
