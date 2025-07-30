import { HStack } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

export function KnapperadContainer(props: PropsWithChildren) {
  return (
    <HStack justify="end" align="center" gap="2">
      {props.children}
    </HStack>
  );
}
