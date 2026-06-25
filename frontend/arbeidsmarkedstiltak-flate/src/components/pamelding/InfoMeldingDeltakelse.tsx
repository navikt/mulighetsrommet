import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";
import {
  GjennomforingOppstartstype,
  DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus as InfoMeldingStatus,
  Tiltaksadministrasjon,
} from "@arbeidsmarkedstiltak/api-client";
import { BodyShort, Button, InfoCard } from "@navikt/ds-react";
import { AkselColor } from "@navikt/ds-react/types/theme";

interface InfoMeldingDeltakelseProps {
  deltakelse: Tiltaksadministrasjon;
}

export function InfoMeldingDeltakelse({ deltakelse }: InfoMeldingDeltakelseProps) {
  if (!deltakelse.infoMeldingStatus) {
    return null;
  }
  const vedtakRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: deltakelse.id,
  });

  const tekster = utledTekster(deltakelse.infoMeldingStatus);
  return (
    <InfoCard data-color={tekster.variant}>
      <InfoCard.Header>{tekster.overskrift}</InfoCard.Header>
      <InfoCard.Content>
        {deltakelse.oppstartstype === GjennomforingOppstartstype.ENKELTPLASS && (
          <BodyShort size="small"> Gjelder {deltakelse.tittel}</BodyShort>
        )}
        <Button
          role="link"
          variant="tertiary"
          className="underline"
          size="xsmall"
          onClick={vedtakRoute.navigate}
        >
          {tekster.lenketekst}
        </Button>
      </InfoCard.Content>
    </InfoCard>
  );
}

interface Tekst {
  overskrift: string;
  lenketekst: string;
  variant: AkselColor;
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
