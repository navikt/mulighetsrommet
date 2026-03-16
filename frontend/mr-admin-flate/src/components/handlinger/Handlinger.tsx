import { ActionMenu, BodyShort, Button } from "@navikt/ds-react";
import React, { ReactElement, ReactNode } from "react";

interface Props {
  handlingerLabel?: string;
  ingenHandlingerTekst?: string;
  children: ReactNode;
}

export function Handlinger({
  children,
  handlingerLabel = "Handlinger",
  ingenHandlingerTekst = "Ingen handlinger",
}: Props) {
  const elements = React.Children.toArray(children).filter((child) => React.isValidElement(child));

  const harIngenHandlinger = !hasActionItems(elements);

  const content = harIngenHandlinger ? (
    <BodyShort size="small" textColor="subtle">
      {ingenHandlingerTekst}
    </BodyShort>
  ) : (
    processChildren(elements)
  );

  return (
    <ActionMenu>
      <ActionMenu.Trigger>
        <Button
          data-color={harIngenHandlinger ? "neutral" : undefined}
          variant="secondary"
          size="small"
        >
          {handlingerLabel}
        </Button>
      </ActionMenu.Trigger>
      <ActionMenu.Content>{content}</ActionMenu.Content>
    </ActionMenu>
  );
}

function hasActionItems(elements: ReactElement[]): boolean {
  return elements.some((item) => !isDivider(item));
}

function isDivider(child: ReactElement): boolean {
  return child.type === ActionMenu.Divider;
}

function processChildren(elements: ReactElement[]): ReactNode {
  const filtered: ReactElement[] = [];

  for (let i = 0; i < elements.length; i++) {
    const child = elements[i];
    if (!isDivider(child)) {
      filtered.push(child);
    } else if (previousIsActionItem(filtered) && nextContainsActionItem(elements, i)) {
      filtered.push(child);
    }
  }

  return filtered;
}

function previousIsActionItem(elements: ReactElement[]) {
  return elements.length > 0 && !isDivider(elements[elements.length - 1]);
}

function nextContainsActionItem(elements: ReactElement[], i: number) {
  return elements.slice(i + 1).some((c) => !isDivider(c));
}
