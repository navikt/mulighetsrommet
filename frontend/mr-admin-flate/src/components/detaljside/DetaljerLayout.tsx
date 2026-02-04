import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { VStack } from "@navikt/ds-react";
import React from "react";

interface Props {
  children: React.ReactNode;
}

export function DetaljerLayout(props: Props) {
  const children = React.Children.toArray(props.children);

  return (
    <VStack justify="space-between">
      {children.map((child, idx) => {
        const key = React.isValidElement(child) && child.key ? child.key : idx;
        const isNotLastElement = idx < children.length - 1;
        return (
          <React.Fragment key={key}>
            {child}
            {isNotLastElement && <Separator />}
          </React.Fragment>
        );
      })}
    </VStack>
  );
}
