import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import styles from "./HistorikkKort.module.scss";
import { HistorikkForBrukerFraKomet } from "mulighetsrommet-api-client";

interface Props {
  historikk: HistorikkForBrukerFraKomet;
}
export function HistorikkKort({ historikk }: Props) {
  // TODO Korrekt url
  const { tiltakstype, tiltaksnavn, fraDato, tilDato, status, tiltaksgjennomforingId, arsak } =
    historikk;
  return (
    <LinkPanel
      href={`/arbeidsmarkedstiltak/tiltak/${tiltaksgjennomforingId}`}
      className={styles.panel}
    >
      <VStack gap="2">
        <small>{tiltakstype.toUpperCase()}</small>
        <Heading size="medium" level="4">
          {tiltaksnavn}
        </Heading>
        <HStack align={"center"} gap="5">
          <BodyShort size="small">
            {fraDato} - {tilDato}
          </BodyShort>
          <Status status={status} />
          {arsak ? <BodyShort size="small">Årsak: {arsak}</BodyShort> : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: HistorikkForBrukerFraKomet.status;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case HistorikkForBrukerFraKomet.status.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Deltar
        </Tag>
      );
    case HistorikkForBrukerFraKomet.status.AVSLUTTET:
      return (
        <Tag size="small" variant="info">
          Avsluttet
        </Tag>
      );
    case HistorikkForBrukerFraKomet.status.IKKE_AKTUELL:
      return (
        <Tag size="small" variant="neutral">
          Ikke aktuell
        </Tag>
      );
    case HistorikkForBrukerFraKomet.status.VENTER:
      return (
        <Tag size="small" variant="info">
          Venter på oppstart
        </Tag>
      );
  }
}
