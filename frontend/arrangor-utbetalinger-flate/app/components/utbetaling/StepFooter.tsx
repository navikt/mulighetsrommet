import { Button, HStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import type { UtbetalingWizard } from "~/hooks/useUtbetalingWizard";

interface StepFooterProps {
  wizard: UtbetalingWizard;
  primaryAction?: PrimaryAction;
}

export interface PrimaryAction {
  onClick: () => void | Promise<void>;
  label?: string;
  loading?: boolean;
}

export function StepFooter({ wizard, primaryAction }: StepFooterProps) {
  return (
    <HStack gap="space-16" marginBlock="space-16 space-0">
      {wizard.isFirstStep ? (
        <Button as={ReactRouterLink} type="button" variant="tertiary" to={wizard.cancelHref}>
          Avbryt
        </Button>
      ) : (
        <Button type="button" variant="tertiary" onClick={wizard.goToPrevious}>
          Tilbake
        </Button>
      )}
      <Button
        onClick={primaryAction?.onClick ?? (() => wizard.goToNext())}
        loading={primaryAction?.loading}
      >
        {primaryAction?.label ?? "Neste"}
      </Button>
    </HStack>
  );
}
