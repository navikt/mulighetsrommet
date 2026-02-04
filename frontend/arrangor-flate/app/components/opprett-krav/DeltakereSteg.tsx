import {
  OpprettKravDeltakere,
  OpprettKravDeltakereGuidePanelType,
  StengtPeriode,
} from "@api-client";
import { BodyShort, GuidePanel, Heading, Link, LocalAlert, VStack } from "@navikt/ds-react";
import { LabeledDataElementList } from "../common/Definisjonsliste";
import { DataDrivenTable } from "@mr/frontend-common";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { tekster } from "~/tekster";
import { Link as ReactRouterLink } from "react-router";
import { Laster } from "../common/Laster";

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
          <LocalAlert status="announcement" size="small">
            <LocalAlert.Header>
              <LocalAlert.Title as="h4">Stengte perioder</LocalAlert.Title>
            </LocalAlert.Header>
            <LocalAlert.Content>
              <BodyShort spacing>{tekster.bokmal.utbetaling.beregning.stengtHosArrangor}</BodyShort>
              <ul>
                {deltakere.stengtHosArrangor.map(({ periode, beskrivelse }: StengtPeriode) => (
                  <li key={periode.start + periode.slutt}>
                    {formaterPeriode(periode)}: {beskrivelse}
                  </li>
                ))}
              </ul>
            </LocalAlert.Content>
          </LocalAlert>
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
