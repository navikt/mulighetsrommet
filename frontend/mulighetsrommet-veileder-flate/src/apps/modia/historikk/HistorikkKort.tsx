import { BodyShort, Heading, HStack, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import styles from "./HistorikkKort.module.scss";
import { formaterDato } from "@/utils/Utils";
import { HistorikkForBrukerV2 } from "mulighetsrommet-api-client";
import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";

interface Props {
  historikk: HistorikkForBrukerV2;
}

export function HistorikkKort({ historikk }: Props) {
  const { tiltakstype, tittel, periode, historiskStatus, beskrivelse, innsoktDato } = historikk;

  const deltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: historikk.deltakerId,
  });

  return (
    <LinkPanel onClick={deltakelseRoute.navigate} className={styles.panel}>
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
          {periode?.startdato && periode.sluttdato ? (
            <BodyShort size="small">
              {formaterDato(periode.startdato)} - {formaterDato(periode.sluttdato)}
            </BodyShort>
          ) : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: HistorikkForBrukerV2.historiskStatusType;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case HistorikkForBrukerV2.historiskStatusType.AVBRUTT:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Avbrutt
        </Tag>
      );
    case HistorikkForBrukerV2.historiskStatusType.HAR_SLUTTET:
      return (
        <Tag size="small" variant="alt1">
          Har sluttet
        </Tag>
      );
    case HistorikkForBrukerV2.historiskStatusType.IKKE_AKTUELL:
      return (
        <Tag size="small" variant="neutral">
          Ikke aktuell
        </Tag>
      );
    case HistorikkForBrukerV2.historiskStatusType.FEILREGISTRERT:
      return (
        <Tag size="small" variant="info">
          Feilregistrert
        </Tag>
      );
    case HistorikkForBrukerV2.historiskStatusType.FULLFORT:
      return (
        <Tag size="small" variant="info">
          Fullført
        </Tag>
      );
    case HistorikkForBrukerV2.historiskStatusType.AVBRUTT_UTKAST:
      return (
        <Tag size="small" variant="neutral">
          Avbrutt utkast
        </Tag>
      );
  }
}
