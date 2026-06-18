import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import {
  DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus as InfoMeldingStatus,
  Tiltaksadministrasjon,
} from "@arbeidsmarkedstiltak/api-client";
import { Alert, BodyShort, Button, Heading, VStack } from "@navikt/ds-react";

interface PameldingDeltakelseAlertProps {
  deltakelse: Tiltaksadministrasjon;
}

export function InfoMeldingDeltakelse({ deltakelse }: PameldingDeltakelseAlertProps) {
  if (!deltakelse.infoMeldingStatus) {
    return null;
  }
  const vedtakRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: deltakelse.id,
  });

  const tekster = utledTekster(deltakelse.infoMeldingStatus);
  return (
    <Alert variant={tekster.variant}>
      <Heading level={"2"} size="small">
        {tekster.overskrift}
      </Heading>
      <VStack gap="space-8">
        <BodyShort>
          <Button
            role="link"
            variant="tertiary"
            className="underline"
            size="xsmall"
            onClick={vedtakRoute.navigate}
          >
            {tekster.lenketekst}
          </Button>
        </BodyShort>
      </VStack>
    </Alert>
  );
}

interface Tekst {
  overskrift: string;
  lenketekst: string;
  variant: "info" | "success" | "warning";
}

function utledTekster(status: InfoMeldingStatus): Tekst {
  switch (status) {
    case InfoMeldingStatus.VENTER_PA_OPPSTART:
      return {
        overskrift: "Venter på oppstart",
        variant: "info",
        lenketekst: "Les om brukerens deltakelse",
      };
    case InfoMeldingStatus.DELTAR:
      return {
        overskrift: "Brukeren deltar på tiltaket",
        variant: "success",
        lenketekst: "Les om brukerens deltakelse",
      };
    case InfoMeldingStatus.UTKAST_TIL_PAMELDING:
      return {
        overskrift: "Utkastet er delt og venter på godkjenning",
        variant: "info",
        lenketekst: "Gå til utkastet",
      };
    case InfoMeldingStatus.KLADD:
      return {
        overskrift: "Kladden er ikke delt",
        lenketekst: "Gå til kladden",
        variant: "warning",
      };
    case InfoMeldingStatus.SOKT_INN:
      return {
        overskrift: "Brukeren er søkt inn",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
    case InfoMeldingStatus.VENTELISTE:
      return {
        overskrift: "Brukeren er på venteliste",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
    case InfoMeldingStatus.VURDERES:
      return {
        overskrift: "Brukeren er søkt inn",
        lenketekst: "Les om brukerens deltakelse",
        variant: "info",
      };
  }
}
