import { BodyShort, Box, GlobalAlert, Link } from "@navikt/ds-react";
import { PageHeading } from "~/components/common/PageHeading";

const loggPaaFaktuaTiltakUrl = "https://www.nav.no/samarbeidspartner/faktura-tiltak#logg-pa";

export default function IngenTilgang() {
  return (
    <Box padding="space-24" background="default" borderRadius="8">
      <PageHeading title="Mangler tilgang" />
      <GlobalAlert status="warning" centered={false}>
        <GlobalAlert.Header>
          <GlobalAlert.Title>Du mangler tilgang til utbetalingsløsningen.</GlobalAlert.Title>
        </GlobalAlert.Header>
        <GlobalAlert.Content>
          <BodyShort spacing>
            Tilgang delegeres i Altinn som en enkeltrettighet av din arbeidsgiver.
          </BodyShort>
          <BodyShort spacing>
            Det er enkeltrettigheten <b>"Be om utbetaling - Nav Arbeidsmarkedstiltak"</b> du må få
            via Altinn.
          </BodyShort>
          <BodyShort spacing>
            Se <Link href={loggPaaFaktuaTiltakUrl}>Hvordan få tilgang til løsningen</Link>
          </BodyShort>
          <BodyShort>
            Når enkeltrettigheten er delegert i Altinn du laste siden på nytt og få tilgang.
          </BodyShort>
        </GlobalAlert.Content>
      </GlobalAlert>
    </Box>
  );
}
