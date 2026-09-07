import { BodyShort, GuidePanel, Heading, Link, VStack } from "@navikt/ds-react";
import type { MetaFunction } from "react-router";
import { Link as ReactRouterLink } from "react-router";
import { getEnvironment } from "~/services/environment";
import { deltakerOversiktLenke, useIdFromUrl } from "~/utils/navigation";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { DataDrivenTable } from "@mr/frontend-common";
import { StengtePerioder } from "~/components/common/StengtePerioder";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { StepFooter } from "~/components/utbetaling/StepFooter";
import { ArrangorflateUtbetalingDto } from "@arrangor-utbetalinger/api-client";

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

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const wizard = useUtbetalingWizard(utbetaling);

  return (
    <VStack gap="space-16">
      <Heading level="2" size="large">
        Deltakere
      </Heading>
      <DeltakereGuidePanel utbetaling={utbetaling} />
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

interface DeltakereGuidePanelProps {
  utbetaling: ArrangorflateUtbetalingDto;
}

function DeltakereGuidePanel({ utbetaling }: DeltakereGuidePanelProps) {
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());

  if (utbetaling.beregning.pris.type === "KREVER_REGISTRERING") {
    return (
      <GuidePanel>
        <BodyShort>
          Her vises deltakere som er registrert på tiltaket. Det er disse deltakerne det skal
          faktureres for. Kontrollér at deltakelsene stemmer.
        </BodyShort>
      </GuidePanel>
    );
  } else {
    return (
      <GuidePanel>
        <BodyShort>
          Hvis noen av opplysningene om deltakerne ikke stemmer må dere sende forslag til Nav om
          endring via{" "}
          <Link as={ReactRouterLink} to={deltakerlisteUrl}>
            Deltakeroversikten
          </Link>
          .
        </BodyShort>
        <BodyShort>
          Opplysninger om deltakerne må være riktig oppdatert før dere sender inn kravet.
        </BodyShort>
      </GuidePanel>
    );
  }
}
