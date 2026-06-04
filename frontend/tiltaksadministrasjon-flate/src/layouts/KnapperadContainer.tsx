import { HStack } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

export function KnapperadContainer(props: PropsWithChildren) {
  return (
    <HStack justify="end" align="center" gap="space-8">
      {props.children}
    </HStack>
  );
}
