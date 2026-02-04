import {
  BodyShort,
  Button,
  GuidePanel,
  Heading,
  HStack,
  Link,
  LocalAlert,
  VStack,
} from "@navikt/ds-react";
import type { MetaFunction } from "react-router";
import { Link as ReactRouterLink } from "react-router";
import { getEnvironment } from "~/services/environment";
import { deltakerOversiktLenke, pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import { tekster } from "~/tekster";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 2 av 3: Beregning - Godkjenn innsending" },
    {
      name: "description",
      content: "Informasjon om beregning og deltakere",
    },
  ];
};

export default function UtbetalingBeregning() {
  const id = useIdFromUrl();
  const orgnr = useOrgnrFromUrl();
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());

  const { data: utbetaling } = useArrangorflateUtbetaling(id);

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Beregning
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
      <Heading level="3" size="medium">
        Deltakere
      </Heading>
      <VStack gap="space-16">
        {utbetaling.beregning.stengt.length > 0 && (
          <LocalAlert status="announcement" size="small">
            <LocalAlert.Header>
              <LocalAlert.Title as="h4">Stengte perioder</LocalAlert.Title>
            </LocalAlert.Header>
            <LocalAlert.Content>
              <BodyShort spacing>{tekster.bokmal.utbetaling.beregning.stengtHosArrangor}</BodyShort>
              <ul>
                {utbetaling.beregning.stengt.map(({ periode, beskrivelse }) => (
                  <li key={periode.start + periode.slutt}>
                    {formaterPeriode(periode)}: {beskrivelse}
                  </li>
                ))}
              </ul>
            </LocalAlert.Content>
          </LocalAlert>
        )}
        <DeltakelserTable
          beregning={utbetaling.beregning}
          advarsler={utbetaling.advarsler}
          deltakerlisteUrl={deltakerlisteUrl}
        />
        <SatsPerioderOgBelop
          satsDetaljer={utbetaling.beregning.satsDetaljer}
          pris={utbetaling.beregning.pris}
        />
        <HStack gap="space-16">
          <Button
            as={ReactRouterLink}
            type="button"
            variant="tertiary"
            to={pathTo.innsendingsinformasjon(orgnr, utbetaling.id)}
          >
            Tilbake
          </Button>
          <Button
            as={ReactRouterLink}
            className="justify-self-end"
            to={pathTo.oppsummering(orgnr, utbetaling.id)}
          >
            Neste
          </Button>
        </HStack>
      </VStack>
    </VStack>
  );
}
