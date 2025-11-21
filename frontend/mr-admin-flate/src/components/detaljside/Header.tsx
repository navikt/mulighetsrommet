import { HStack } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return <HStack className="bg-white m-auto px-4 gap-6">{children}</HStack>;
}
