import { ActionMenu, BodyShort, Button } from "@navikt/ds-react";
import { Fragment, ReactNode, useState } from "react";
import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Link as ReactRouterLink } from "react-router";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";

export type HandlingItem<T> = HandlingItemButton<T> | HandlingItemLink<T>;

type HandlingItemBase<T> = {
  label: string;
  icon?: ReactNode;
  variant?: "danger";
  /** Handlingen som kreves for at item skal vises. */
  handling?: T;
};

export type HandlingItemButton<T> = HandlingItemBase<T> & {
  onClick: () => void;
  /** Hvis satt, vises en bekreftelsesdialog hvis brukeren ikke er administrator. */
  administratorer?: string[];
  href?: never;
};

export type HandlingItemLink<T> = HandlingItemBase<T> & {
  href: string;
  /** Sett til true for å åpne lenken i ny fane (ekstern lenke), intern lenke ellers. */
  isExternal?: boolean;
  onClick?: never;
  administratorer?: never;
};

export interface HandlingGruppe<T> {
  label?: string;
  items: HandlingItem<T>[];
}

interface Props<T> {
  handlingerLabel?: string;
  ingenHandlingerTekst?: string;
  grupper: HandlingGruppe<T>[];
  /** Liste over tilgjengelige handlinger. Items med matching handling-felt vises, øvrige skjules. */
  handlinger?: T[];
  navIdent?: string;
}

export function Handlinger<T>({
  grupper,
  handlinger = [],
  navIdent,
  handlingerLabel = "Handlinger",
  ingenHandlingerTekst = "Ingen handlinger",
}: Props<T>) {
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);

  const synligeGrupper = grupper
    .map((g) => ({
      ...g,
      items: g.items.filter((i) => !i.handling || handlinger.includes(i.handling)),
    }))
    .filter((g) => g.items.length > 0);

  const harIngenHandlinger = synligeGrupper.length === 0;

  return (
    <>
      <ActionMenu>
        <ActionMenu.Trigger>
          <Button
            data-color={harIngenHandlinger ? "neutral" : undefined}
            variant="secondary"
            size="small"
            icon={<ChevronDownIcon aria-hidden />}
            iconPosition="right"
          >
            {handlingerLabel}
          </Button>
        </ActionMenu.Trigger>
        <ActionMenu.Content>
          {harIngenHandlinger ? (
            <BodyShort size="small" textColor="subtle">
              {ingenHandlingerTekst}
            </BodyShort>
          ) : (
            synligeGrupper.map((gruppe, idx) => (
              <Fragment key={gruppe.label ?? idx}>
                {idx > 0 && <ActionMenu.Divider />}
                {gruppe.label ? (
                  <ActionMenu.Group label={gruppe.label}>
                    {gruppe.items.map((item, i) =>
                      renderItem(item, i, navIdent, (action) => setPendingAction(() => action)),
                    )}
                  </ActionMenu.Group>
                ) : (
                  gruppe.items.map((item, i) =>
                    renderItem(item, i, navIdent, (action) => setPendingAction(() => action)),
                  )
                )}
              </Fragment>
            ))
          )}
        </ActionMenu.Content>
      </ActionMenu>

      <VarselModal
        open={pendingAction !== null}
        handleClose={() => setPendingAction(null)}
        headingIconType="info"
        headingText="Du er ikke satt som administrator"
        body={<BodyShort>Vil du fortsette?</BodyShort>}
        secondaryButton
        primaryButton={
          <Button
            variant="primary"
            onClick={() => {
              pendingAction?.();
              setPendingAction(null);
            }}
          >
            Ja, jeg vil fortsette
          </Button>
        }
      />
    </>
  );
}

function renderItem<T>(
  item: HandlingItem<T>,
  index: number,
  navIdent?: string,
  onPendingAction?: (action: () => void) => void,
) {
  if (item.href) {
    return item.isExternal ? (
      <ActionMenu.Item
        key={index}
        as="a"
        href={item.href}
        target="_blank"
        rel="noopener noreferrer"
        icon={item.icon}
        variant={item.variant}
      >
        {item.label}
      </ActionMenu.Item>
    ) : (
      <ActionMenu.Item
        key={index}
        as={ReactRouterLink}
        to={item.href}
        icon={item.icon}
        variant={item.variant}
      >
        {item.label}
      </ActionMenu.Item>
    );
  }

  const needsConfirm =
    item.administratorer !== undefined && !isAdmin(item.administratorer, navIdent);

  const handleClick = needsConfirm ? () => onPendingAction?.(() => item.onClick()) : item.onClick;

  return (
    <ActionMenu.Item key={index} onClick={handleClick} icon={item.icon} variant={item.variant}>
      {item.label}
    </ActionMenu.Item>
  );
}

function isAdmin(administratorer: string[], navIdent?: string): boolean {
  return administratorer.length === 0 || (!!navIdent && administratorer.includes(navIdent));
}
