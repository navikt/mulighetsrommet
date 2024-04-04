import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { DeltakerKort, DeltakerStatus } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import styles from "./DeltakelseKort.module.scss";

interface Props {
  deltakelse: DeltakerKort;
}

export function DeltakelseKort({ deltakelse }: Props) {
  const { tiltakstype, deltakerId, tittel, innsoktDato } = deltakelse;

  const deltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: deltakerId!!,
  });

  return (
    <LinkPanel
      as="button"
      onClick={deltakelseRoute.navigate}
      className={classNames(styles.panel, {
        [styles.utkast]: deltakelse?.status.type === DeltakerStatus.type.UTKAST_TIL_PAMELDING,
        [styles.kladd]: deltakelse?.status.type === DeltakerStatus.type.KLADD,
      })}
    >
      <VStack gap="2">
        <HStack gap="10">
          {tiltakstype?.navn ? <small>{tiltakstype?.navn.toUpperCase()}</small> : null}
          {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
        </HStack>
        {tittel ? (
          <Heading size="medium" level="4">
            {tittel}
          </Heading>
        ) : null}
        <HStack align={"center"} gap="5">
          {deltakelse?.status ? <Status status={deltakelse.status} /> : null}
          {deltakelse.status.aarsak ? (
            <BodyShort size="small">Årsak: {deltakelse.status.aarsak}</BodyShort>
          ) : null}
          {deltakelse.periode?.startdato ? (
            <BodyShort size="small">
              {[deltakelse.periode.startdato, deltakelse.periode.sluttdato]
                .filter(Boolean)
                .map((dato) => dato && formaterDato(dato))
                .join(" - ") +
                (deltakelse.periode?.startdato && !deltakelse.periode?.sluttdato ? " - " : "")}
            </BodyShort>
          ) : null}
          {deltakelse.sistEndretDato ? (
            <span>Sist endret: {formaterDato(deltakelse.sistEndretDato)}</span>
          ) : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: DeltakerStatus;
}

function Status({ status }: StatusProps) {
  const { statustekst } = status;
  switch (status.type) {
    case DeltakerStatus.type.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          {statustekst}
        </Tag>
      );
    case DeltakerStatus.type.IKKE_AKTUELL:
    case DeltakerStatus.type.AVBRUTT_UTKAST:
    case DeltakerStatus.type.AVBRUTT:
      return (
        <Tag size="small" variant="neutral">
          {statustekst}
        </Tag>
      );
    case DeltakerStatus.type.UTKAST_TIL_PAMELDING:
    case DeltakerStatus.type.FULLFORT:
      return (
        <Tag size="small" variant="info">
          {statustekst}
        </Tag>
      );
    case DeltakerStatus.type.HAR_SLUTTET:
      return (
        <Tag size="small" variant="alt1">
          {statustekst}
        </Tag>
      );
    case DeltakerStatus.type.VENTER_PA_OPPSTART:
      return (
        <Tag size="small" variant="alt3">
          {statustekst}
        </Tag>
      );

    case DeltakerStatus.type.KLADD:
    case DeltakerStatus.type.SOKT_INN:
    case DeltakerStatus.type.VURDERES:
    case DeltakerStatus.type.VENTELISTE:
      return (
        <Tag size="small" variant="warning">
          {statustekst}
        </Tag>
      );
  }
}
