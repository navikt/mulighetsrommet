import { Link as ReactRouterLink, Outlet } from "react-router";
import { Suspense } from "react";
import { Box, Button, Hide, HStack, Link, Stepper, VStack } from "@navikt/ds-react";
import { Laster } from "~/components/common/Laster";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { useArrangorflateTilsagnTilUtbetaling } from "~/hooks/useArrangorflateTilsagnTilUtbetaling";
import { pathTo, useIdFromUrl } from "~/utils/navigation";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";

export default function InnsendingLayout() {
  const id = useIdFromUrl();
  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const { data: tilsagn } = useArrangorflateTilsagnTilUtbetaling(id);

  const wizard = useUtbetalingWizard(utbetaling);

  const harTilsagn = tilsagn.length > 0;

  return (
    <VStack gap="space-16" justify="center">
      <Link as={ReactRouterLink} to={pathTo.utbetalinger}>
        <ChevronLeftIcon /> Tilbake til oversikt
      </Link>

      {wizard.steps.length > 0 && (
        <Hide below="sm">
          <Stepper aria-label="Steg" activeStep={wizard.activeStep} orientation="horizontal">
            {wizard.steps.map(({ name }, index) => (
              <Stepper.Step
                key={name}
                interactive={false}
                completed={wizard.activeStep > index + 1}
              >
                {name}
              </Stepper.Step>
            ))}
          </Stepper>
        </Hide>
      )}

      <Box background="default" borderRadius="8" padding="space-32">
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          <Outlet />
        </Suspense>

        {harTilsagn && !wizard.isLastStep && (
          <HStack gap="space-16" marginBlock="space-16 space-0">
            {wizard.isFirstStep ? (
              <Button
                as={ReactRouterLink}
                type="button"
                variant="tertiary"
                to={pathTo.utbetalinger}
              >
                Avbryt
              </Button>
            ) : (
              <Button type="button" variant="tertiary" onClick={wizard.goToPrevious}>
                Tilbake
              </Button>
            )}
            <Button onClick={wizard.goToNext}>Neste</Button>
          </HStack>
        )}
      </Box>
    </VStack>
  );
}
