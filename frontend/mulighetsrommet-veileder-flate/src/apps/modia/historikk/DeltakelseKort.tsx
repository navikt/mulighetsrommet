import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { DeltakerKort, DeltakerStatus, DeltakerStatusType } from "mulighetsrommet-api-client";
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
    deltakerId: deltakerId!,
  });

  return (
    <LinkPanel
      as="button"
      onClick={deltakelseRoute.navigate}
      className={classNames(styles.panel, {
        [styles.utkast]: deltakelse?.status.type === DeltakerStatusType.UTKAST_TIL_PAMELDING,
        [styles.kladd]: deltakelse?.status.type === DeltakerStatusType.KLADD,
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
        <HStack align={"end"} gap="5">
          {deltakelse?.status ? <Status status={deltakelse.status} /> : null}
          {deltakelse.status.aarsak ? (
            <BodyShort size="small">Årsak: {deltakelse.status.aarsak}</BodyShort>
          ) : null}
          {deltakelse.periode?.startdato ? (
            <BodyShort size="small">
              {deltakelse.periode?.startdato && !deltakelse.periode?.sluttdato
                ? `Oppstartsdato ${formaterDato(deltakelse.periode.startdato)}`
                : [deltakelse.periode.startdato, deltakelse.periode.sluttdato]
                    .filter(Boolean)
                    .map((dato) => dato && formaterDato(dato))
                    .join(" - ")}
            </BodyShort>
          ) : null}
          {deltakelse.sistEndretDato ? (
            <BodyShort size="small">
              Sist endret: {formaterDato(deltakelse.sistEndretDato)}
            </BodyShort>
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
  const { visningstekst } = status;
  switch (status.type) {
    case DeltakerStatusType.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.IKKE_AKTUELL:
    case DeltakerStatusType.AVBRUTT_UTKAST:
    case DeltakerStatusType.AVBRUTT:
    case DeltakerStatusType.FEILREGISTRERT:
      return (
        <Tag size="small" variant="neutral">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
    case DeltakerStatusType.FULLFORT:
      return (
        <Tag size="small" variant="info">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.HAR_SLUTTET:
      return (
        <Tag size="small" variant="alt1">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.VENTER_PA_OPPSTART:
      return (
        <Tag size="small" variant="alt3">
          {visningstekst}
        </Tag>
      );

    case DeltakerStatusType.KLADD:
    case DeltakerStatusType.SOKT_INN:
    case DeltakerStatusType.VURDERES:
    case DeltakerStatusType.VENTELISTE:
      return (
        <Tag size="small" variant="warning">
          {visningstekst}
        </Tag>
      );
  }
}
