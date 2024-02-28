import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import styles from "./HistorikkKort.module.scss";
import { HistorikkForBrukerFraKomet } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";

interface Props {
  historikk: HistorikkForBrukerFraKomet;
}
export function HistorikkKort({ historikk }: Props) {
  const {
    tiltakstype,
    tittel,
    fraDato,
    tilDato,
    status,
    tiltaksgjennomforingId,
    beskrivelse,
    innsoktDato,
  } = historikk;
  return (
    <LinkPanel
      href={`/arbeidsmarkedstiltak/tiltak/${tiltaksgjennomforingId}`}
      className={styles.panel}
    >
      <VStack gap="2">
        <HStack gap="10">
          <small>{tiltakstype.toUpperCase()}</small>
          <small>Søkt inn: {formaterDato(innsoktDato)}</small>
        </HStack>
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
        <HStack align={"center"} gap="5">
          <Status status={status.navn} />
          {beskrivelse ? <BodyShort size="small">Årsak: {beskrivelse}</BodyShort> : null}
          <BodyShort size="small">
            {fraDato} - {tilDato}
          </BodyShort>
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: HistorikkForBrukerFraKomet.navn;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case HistorikkForBrukerFraKomet.navn.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Deltar
        </Tag>
      );
    case HistorikkForBrukerFraKomet.navn.AVSLUTTET:
      return (
        <Tag size="small" variant="info">
          Avsluttet
        </Tag>
      );
    case HistorikkForBrukerFraKomet.navn.IKKE_AKTUELL:
      return (
        <Tag size="small" variant="neutral">
          Ikke aktuell
        </Tag>
      );
    case HistorikkForBrukerFraKomet.navn.VENTER:
      return (
        <Tag size="small" variant="info">
          Venter på oppstart
        </Tag>
      );
  }
}
