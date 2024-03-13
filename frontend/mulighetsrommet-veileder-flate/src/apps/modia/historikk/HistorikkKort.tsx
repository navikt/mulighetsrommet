import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import styles from "./HistorikkKort.module.scss";
import { HistorikkForBrukerFraKomet } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";

interface Props {
  historikk: HistorikkForBrukerFraKomet;
}
export function HistorikkKort({ historikk }: Props) {
  const { tiltakstype, tittel, periode, historiskStatus, beskrivelse, innsoktDato } = historikk;
  return (
    <LinkPanel
      href="#" // TODO Fiks korrekt url til Komets løsning for påmelding
      className={styles.panel}
    >
      <VStack gap="2">
        <HStack gap="10">
          <small>{tiltakstype.navn.toUpperCase()}</small>
          <small>Søkt inn: {formaterDato(innsoktDato)}</small>
        </HStack>
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
        <HStack align={"center"} gap="5">
          <Status status={historiskStatus.historiskStatusType} />
          {beskrivelse ? <BodyShort size="small">Årsak: {beskrivelse}</BodyShort> : null}
          {periode ? (
            <BodyShort size="small">
              {periode.startDato} - {periode.sluttDato}
            </BodyShort>
          ) : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: HistorikkForBrukerFraKomet.historiskStatusType;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case HistorikkForBrukerFraKomet.historiskStatusType.AVBRUTT:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Avbrutt
        </Tag>
      );
    case HistorikkForBrukerFraKomet.historiskStatusType.HAR_SLUTTET:
      return (
        <Tag size="small" variant="info">
          Har sluttet
        </Tag>
      );
    case HistorikkForBrukerFraKomet.historiskStatusType.IKKE_AKTUELL:
      return (
        <Tag size="small" variant="neutral">
          Ikke aktuell
        </Tag>
      );
    case HistorikkForBrukerFraKomet.historiskStatusType.FEILREGISTRERT:
      return (
        <Tag size="small" variant="info">
          Feilregistrert
        </Tag>
      );
    case HistorikkForBrukerFraKomet.historiskStatusType.FULLFORT:
      return (
        <Tag size="small" variant="info">
          Fullført
        </Tag>
      );
    case HistorikkForBrukerFraKomet.historiskStatusType.AVBRUTT_UTKAST:
      return (
        <Tag size="small" variant="info">
          Avbrutt utkast
        </Tag>
      );
  }
}
