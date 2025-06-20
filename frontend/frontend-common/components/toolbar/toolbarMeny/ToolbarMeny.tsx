import { HStack } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

export function ToolbarMeny(props: PropsWithChildren) {
  return (
    <HStack align="center" justify="space-between" className="pb-2 px-4">
      {props.children}
    </HStack>
  );
}
