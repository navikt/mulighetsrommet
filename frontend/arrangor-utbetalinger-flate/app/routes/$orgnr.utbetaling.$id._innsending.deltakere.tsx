import { BodyShort, GuidePanel, Heading, Link, VStack } from "@navikt/ds-react";
import type { MetaFunction } from "react-router";
import { Link as ReactRouterLink } from "react-router";
import { getEnvironment } from "~/services/environment";
import { deltakerOversiktLenke, useIdFromUrl } from "~/utils/navigation";
import { tekster } from "~/tekster";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { DataDrivenTable } from "@mr/frontend-common";
import { StengtePerioder } from "~/components/common/StengtePerioder";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { StepFooter } from "~/components/utbetaling/StepFooter";

export const meta: MetaFunction = () => {
  return [
    { title: "Deltakere - Godkjenn innsending" },
    {
      name: "description",
      content: "Informasjon om deltakere",
    },
  ];
};

export default function UtbetalingBeregning() {
  const id = useIdFromUrl();
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const wizard = useUtbetalingWizard(utbetaling);

  return (
    <VStack gap="space-16">
      <Heading level="2" size="large">
        Deltakere
      </Heading>
      <GuidePanel>
        <BodyShort>
          {tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.intro}{" "}
          <Link as={ReactRouterLink} to={deltakerlisteUrl}>
            Deltakeroversikten
          </Link>
          .
        </BodyShort>
        <BodyShort>{tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.utro}</BodyShort>
      </GuidePanel>
      <VStack gap="space-16">
        {utbetaling.beregning.stengt.length > 0 && (
          <StengtePerioder perioder={utbetaling.beregning.stengt} />
        )}
        {utbetaling.beregning.deltakelser && (
          <DataDrivenTable data={utbetaling.beregning.deltakelser} />
        )}
        <SatsPerioderOgBelop
          satsDetaljer={utbetaling.beregning.satsDetaljer}
          pris={utbetaling.beregning.pris}
        />
      </VStack>
      <StepFooter wizard={wizard} />
    </VStack>
  );
}
