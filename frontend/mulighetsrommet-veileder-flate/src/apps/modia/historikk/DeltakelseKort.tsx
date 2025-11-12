import { Deltakelse, DeltakelseEierskap, DeltakelseTilstand } from "@api-client";
import { BodyShort, Box, Button, Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL } from "@/constants";
import { Link } from "react-router";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

interface Props {
  deltakelse: Deltakelse;
}

export function DeltakelseKort({ deltakelse }: Props) {
  return (
    <Box
      background="bg-default"
      borderRadius="medium"
      padding="5"
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
  switch (deltakelse.eierskap) {
    case DeltakelseEierskap.ARENA:
      return null;
    case DeltakelseEierskap.TEAM_KOMET: {
      const deltakelseRoute = resolveModiaRoute({
        route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
        deltakerId: deltakelse.id,
      });
      return (
        <VStack gap="2">
          <Button variant="secondary" onClick={deltakelseRoute.navigate} size="small">
            Gå til deltakelse
          </Button>
          {deltakelse.pamelding && (
            <Link
              to={`/arbeidsmarkedstiltak/tiltak/${deltakelse.pamelding.gjennomforingId}`}
              className="text-center no-underline text-[16px] hover:underline"
            >
              Gå til tiltak
            </Link>
          )}
        </VStack>
      );
    }
    case DeltakelseEierskap.TEAM_TILTAK: {
      const link = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/avtale/${deltakelse.id}?part=VEILEDER`;
      return (
        <Lenkeknapp variant="secondary" to={link} size="small">
          Gå til avtale
        </Lenkeknapp>
      );
    }
  }
}

function getDeltakelseKortBorder(tilstand: DeltakelseTilstand) {
  switch (tilstand) {
    case DeltakelseTilstand.UTKAST:
      return "border-2 border-dashed border-border-info";
    case DeltakelseTilstand.KLADD:
      return "border-2 border-dashed border-border-warning";
    case DeltakelseTilstand.AKTIV:
    case DeltakelseTilstand.AVSLUTTET:
      return "";
  }
}

function Innhold({ deltakelse }: { deltakelse: Deltakelse }) {
  const { tiltakstype, status, periode, tittel, innsoktDato } = deltakelse;
  const aarsak = "aarsak" in status ? status.aarsak : null;
  return (
    <VStack gap="2">
      <HStack gap="10">
        <small>{tiltakstype.navn.toUpperCase()}</small>
        {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
      </HStack>
      {tittel ? (
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
      ) : null}
      <HStack align={"end"} gap="5">
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
        {deltakelse.sistEndretDato ? (
          <BodyShort size="small">Sist endret: {formaterDato(deltakelse.sistEndretDato)}</BodyShort>
        ) : null}
      </HStack>
    </VStack>
  );
}
