import { HStack } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return (
    <HStack gap="space-6" className="bg-ax-bg-default m-auto px-4 pb-4" align="center">
      {children}
    </HStack>
  );
}
