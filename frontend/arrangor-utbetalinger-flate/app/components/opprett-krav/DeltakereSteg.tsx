import {
  OpprettKravDeltakere,
  OpprettKravDeltakereGuidePanelType,
} from "@arrangor-utbetalinger/api-client";
import { BodyShort, GuidePanel, Heading, Link, VStack } from "@navikt/ds-react";
import { LabeledDataElementList } from "../common/Definisjonsliste";
import { DataDrivenTable } from "@mr/frontend-common";
import { tekster } from "~/tekster";
import { Link as ReactRouterLink } from "react-router";
import { Laster } from "../common/Laster";
import { StengtePerioder } from "../common/StengtePerioder";

interface DeltakereStepProps {
  deltakere: OpprettKravDeltakere | null;
  deltakerlisteUrl: string;
}

export default function DeltakereSteg({ deltakere, deltakerlisteUrl }: DeltakereStepProps) {
  if (!deltakere) {
    return <Laster tekst="Laster deltakere..." />;
  }

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Oversikt over deltakere
      </Heading>
      <DeltakelseGuidePanel
        deltakerlisteUrl={deltakerlisteUrl}
        guidePanelType={deltakere.guidePanel}
      />
      <VStack gap="space-16">
        {deltakere.stengtHosArrangor.length > 0 && (
          <StengtePerioder perioder={deltakere.stengtHosArrangor} />
        )}
        <DataDrivenTable data={deltakere.tabell} />
        <VStack gap="space-8">
          {deltakere.tabellFooter.map((details, idx) => (
            <VStack gap="space-8" key={idx}>
              {details.header && deltakere.tabellFooter.length > 2 && (
                <Heading size="xsmall">{details.header}</Heading>
              )}
              <LabeledDataElementList entries={details.entries} />
            </VStack>
          ))}
        </VStack>
      </VStack>
    </VStack>
  );
}

interface DeltakelseGuidePanelProps {
  deltakerlisteUrl: string;
  guidePanelType: OpprettKravDeltakereGuidePanelType;
}

function DeltakelseGuidePanel({ deltakerlisteUrl, guidePanelType }: DeltakelseGuidePanelProps) {
  switch (guidePanelType) {
    case OpprettKravDeltakereGuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          <BodyShort>
            Her vises deltakere som er registrert på tiltaket. Det er disse deltakerne det skal
            faktureres for. Kontrollér at deltakelsene stemmer.
          </BodyShort>
        </GuidePanel>
      );
    case OpprettKravDeltakereGuidePanelType.GENERELL:
    default:
      return (
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
      );
  }
}
