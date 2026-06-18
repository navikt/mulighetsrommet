import { Deltakelse, DeltakelseTilstand } from "@arbeidsmarkedstiltak/api-client";
import { BodyShort, Box, Button, Heading, HGrid, HStack, VStack, Link } from "@navikt/ds-react";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL } from "@/constants";
import { Link as ReactRouterLink } from "react-router";
import { DataElementStatusTag } from "@mr/frontend-common";

interface Props {
  deltakelse: Deltakelse;
}

export function DeltakelseKort({ deltakelse }: Props) {
  return (
    <Box
      background="default"
      borderRadius="4"
      padding="space-20"
      className={getDeltakelseKortBorder(deltakelse.tilstand)}
    >
      <HGrid columns="1fr 20%" align="center">
        <Innhold deltakelse={deltakelse} />
        <Knapper deltakelse={deltakelse} />
      </HGrid>
    </Box>
  );
}

function Knapper({ deltakelse }: Props) {
  switch (deltakelse.type) {
    case "TILTAKSADMINISTRASJON": {
      const deltakelseRoute = resolveModiaRoute({
        route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
        deltakerId: deltakelse.id,
      });
      return (
        <VStack gap="space-12" align="center">
          <Button variant="secondary" onClick={deltakelseRoute.navigate} size="small">
            Gå til deltakelse
          </Button>
          {deltakelse.infoMeldingStatus && (
            <Link
              as={ReactRouterLink}
              to={`/arbeidsmarkedstiltak/tiltak/${deltakelse.gjennomforingId}`}
            >
              <BodyShort size="small">Gå til tiltak</BodyShort>
            </Link>
          )}
        </VStack>
      );
    }
    case "TILTAK_ARBEIDSGIVER": {
      const link = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/avtale/${deltakelse.id}?part=VEILEDER`;
      return (
        <Button as="a" variant="secondary" href={link} size="small">
          Gå til avtale
        </Button>
      );
    }
    case "ARENA":
    case undefined:
    default:
      return null;
  }
}

function getDeltakelseKortBorder(tilstand: DeltakelseTilstand) {
  switch (tilstand) {
    case DeltakelseTilstand.UTKAST:
      return "border-2 border-dashed border-ax-border-accent-strong";
    case DeltakelseTilstand.KLADD:
      return "border-2 border-dashed border-ax-border-warning";
    case DeltakelseTilstand.AKTIV:
    case DeltakelseTilstand.AVSLUTTET:
      return "";
  }
}

function Innhold({ deltakelse }: { deltakelse: Deltakelse }) {
  const { tiltakstype, status, periode, tittel } = deltakelse;
  const aarsak = "aarsak" in status ? status.aarsak : null;
  return (
    <VStack gap="space-8">
      <HStack gap="space-40">
        <small>{tiltakstype.navn.toUpperCase()}</small>
        {<InnsoktDato deltakelse={deltakelse} />}
      </HStack>
      {tittel ? (
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
      ) : null}
      <HStack align={"end"} gap="space-20">
        <DataElementStatusTag {...status.type} />
        {aarsak ? <BodyShort size="small">Årsak: {aarsak}</BodyShort> : null}
        {periode.startDato ? (
          <BodyShort size="small">
            {periode.startDato && !periode.sluttDato
              ? `Oppstartsdato ${formaterDato(periode.startDato)}`
              : [periode.startDato, periode.sluttDato]
                  .filter(Boolean)
                  .map((dato) => dato && formaterDato(dato))
                  .join(" - ")}
          </BodyShort>
        ) : null}
        <SistEndretDato deltakelse={deltakelse} />
      </HStack>
    </VStack>
  );
}

function InnsoktDato({ deltakelse }: { deltakelse: Deltakelse }) {
  switch (deltakelse.type) {
    case "TILTAKSADMINISTRASJON": {
      if (deltakelse.innsoktDato) {
        return <small>Søkt inn: {formaterDato(deltakelse.innsoktDato)}</small>;
      }
      return null;
    }
    case "TILTAK_ARBEIDSGIVER":
    case "ARENA":
    case undefined:
    default:
      return null;
  }
}

function SistEndretDato({ deltakelse }: { deltakelse: Deltakelse }) {
  switch (deltakelse.type) {
    case "TILTAKSADMINISTRASJON": {
      if (deltakelse.sistEndretDato) {
        return (
          <BodyShort size="small">Sist endret: {formaterDato(deltakelse.sistEndretDato)}</BodyShort>
        );
      }
      return null;
    }
    case "TILTAK_ARBEIDSGIVER":
    case "ARENA":
    case undefined:
    default:
      return null;
  }
}
