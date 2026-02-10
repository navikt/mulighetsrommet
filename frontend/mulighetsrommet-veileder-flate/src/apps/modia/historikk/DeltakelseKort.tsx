import { Deltakelse, DeltakelseEierskap, DeltakelseTilstand } from "@api-client";
import { BodyShort, Box, Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL } from "@/constants";
import { DataElementStatusTag, Lenke, Lenkeknapp } from "@mr/frontend-common";

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
  switch (deltakelse.eierskap) {
    case DeltakelseEierskap.ARENA:
      return null;
    case DeltakelseEierskap.TEAM_KOMET: {
      const deltakelseRoute = resolveModiaRoute({
        route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
        deltakerId: deltakelse.id,
      });
      return (
        <VStack gap="space-12" align="center">
          <Lenkeknapp to={deltakelseRoute.href} variant="secondary">
            Gå til deltakelse
          </Lenkeknapp>
          {deltakelse.pamelding && (
            <Lenke to={`/arbeidsmarkedstiltak/tiltak/${deltakelse.pamelding.gjennomforingId}`}>
              <BodyShort size="small">Gå til tiltak</BodyShort>
            </Lenke>
          )}
        </VStack>
      );
    }
    case DeltakelseEierskap.TEAM_TILTAK: {
      const link = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/avtale/${deltakelse.id}?part=VEILEDER`;
      return (
        <Lenkeknapp to={link} variant="secondary" size="small">
          Gå til avtale
        </Lenkeknapp>
      );
    }
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
  const { tiltakstype, status, periode, tittel, innsoktDato } = deltakelse;
  const aarsak = "aarsak" in status ? status.aarsak : null;
  return (
    <VStack gap="space-8">
      <HStack gap="space-40">
        <small>{tiltakstype.navn.toUpperCase()}</small>
        {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
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
        {deltakelse.sistEndretDato ? (
          <BodyShort size="small">Sist endret: {formaterDato(deltakelse.sistEndretDato)}</BodyShort>
        ) : null}
      </HStack>
    </VStack>
  );
}
