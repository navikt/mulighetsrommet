import { ActionMenu, BodyShort, Button } from "@navikt/ds-react";
import React, { ReactNode } from "react";

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
  const harIngenHandlinger = countActionItems(children) === 0;

  const content = harIngenHandlinger ? (
    <BodyShort size="small" textColor="subtle">
      {ingenHandlingerTekst}
    </BodyShort>
  ) : (
    children
  );

  const color = harIngenHandlinger ? "neutral" : undefined;

  return (
    <ActionMenu>
      <ActionMenu.Trigger>
        <Button data-color={color} variant="secondary" size="small">
          {handlingerLabel}
        </Button>
      </ActionMenu.Trigger>
      <ActionMenu.Content>{content}</ActionMenu.Content>
    </ActionMenu>
  );
}

function countActionItems(children: ReactNode): number {
  return React.Children.toArray(children).filter((child) => {
    return React.isValidElement(child);
  }).length;
}
