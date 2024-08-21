import { DeltakerKort, DeltakerStatus, DeltakerStatusType } from "@mr/api-client";
import { BodyShort, Box, Button, HStack, Heading, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import styles from "./DeltakelseKort.module.scss";

interface Props {
  deltakelse: DeltakerKort;
}

export function DeltakelseKort({ deltakelse }: Props) {
  const { id, eierskap } = deltakelse;

  const deltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: id!,
  });

  if (eierskap === "ARENA") {
    return (
      <Box background="bg-default" padding="5">
        <Innhold deltakelse={deltakelse} />
      </Box>
    );
  }

  return (
    <Box
      background="bg-default"
      padding="5"
      className={classNames(styles.panel, {
        [styles.utkast]: deltakelse?.status.type === DeltakerStatusType.UTKAST_TIL_PAMELDING,
        [styles.kladd]: deltakelse?.status.type === DeltakerStatusType.KLADD,
      })}
    >
      <HStack justify={"space-between"} align={"center"}>
        <Innhold deltakelse={deltakelse} />
        <Button onClick={deltakelseRoute.navigate} size="small">
          Gå til deltakelse
        </Button>
      </HStack>
    </Box>
  );
}

function Innhold({ deltakelse }: { deltakelse: DeltakerKort }) {
  const { tiltakstypeNavn, status, periode, tittel, innsoktDato } = deltakelse;
  return (
    <VStack gap="2">
      <HStack gap="10">
        {tiltakstypeNavn ? <small>{tiltakstypeNavn.toUpperCase()}</small> : null}
        {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
      </HStack>
      {tittel ? (
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
      ) : null}
      <HStack align={"end"} gap="5">
        {status ? <Status status={status} /> : null}
        {status.aarsak ? <BodyShort size="small">Årsak: {status.aarsak}</BodyShort> : null}
        {periode?.startdato ? (
          <BodyShort size="small">
            {periode?.startdato && !periode?.sluttdato
              ? `Oppstartsdato ${formaterDato(periode?.startdato)}`
              : [periode?.startdato, periode?.sluttdato]
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
