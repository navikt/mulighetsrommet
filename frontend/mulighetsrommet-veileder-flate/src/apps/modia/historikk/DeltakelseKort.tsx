import { DeltakerKort, DeltakerStatus, DeltakerStatusType } from "@mr/api-client";
import { BodyShort, Box, Button, HGrid, HStack, Heading, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import styles from "./DeltakelseKort.module.scss";

type Size = "small" | "medium" | "large";

interface Props {
  deltakelse: DeltakerKort;
  size?: Size;
}

export function DeltakelseKort({ deltakelse, size = "medium" }: Props) {
  const { id, eierskap } = deltakelse;

  const deltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: id!,
  });

  if (eierskap === "ARENA") {
    return (
      <Wrapper size={size} deltakelse={deltakelse}>
        <Innhold deltakelse={deltakelse} />
      </Wrapper>
    );
  }

  return (
    <Wrapper size={size} deltakelse={deltakelse}>
      <HGrid columns="1fr 20%" align="center">
        <Innhold deltakelse={deltakelse} />
        <Button variant="secondary" onClick={deltakelseRoute.navigate} size="small">
          Gå til deltakelse
        </Button>
      </HGrid>
    </Wrapper>
  );
}

function Wrapper({
  size,
  children,
  deltakelse,
}: {
  size: Size;
  deltakelse: DeltakerKort;
  onClick?: () => void;
  children: React.ReactNode;
}) {
  return (
    <Box
      background="bg-default"
      borderRadius="medium"
      padding={size === "small" ? "2" : size === "medium" ? "5" : "8"}
      className={classNames(styles.panel, {
        [styles.utkast]: deltakelse?.status.type === DeltakerStatusType.UTKAST_TIL_PAMELDING,
        [styles.kladd]:
          deltakelse?.status.type === DeltakerStatusType.KLADD ||
          deltakelse?.status.type === DeltakerStatusType.PABEGYNT_REGISTRERING,
      })}
    >
      {children}
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
        {periode?.startDato ? (
          <BodyShort size="small">
            {periode?.startDato && !periode?.sluttDato
              ? `Oppstartsdato ${formaterDato(periode?.startDato)}`
              : [periode?.startDato, periode?.sluttDato]
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
    case DeltakerStatusType.GJENNOMFORES:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.PABEGYNT_REGISTRERING:
    case DeltakerStatusType.KLADD:
      return (
        <Tag size="small" variant="warning">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.INFORMASJONSMOTE:
    case DeltakerStatusType.TILBUD:
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
      return (
        <Tag size="small" variant="info">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.IKKE_AKTUELL:
    case DeltakerStatusType.FEILREGISTRERT:
    case DeltakerStatusType.VENTELISTE:
    case DeltakerStatusType.AVBRUTT:
    case DeltakerStatusType.AVBRUTT_UTKAST:
    case DeltakerStatusType.AVSLAG:
    case DeltakerStatusType.DELTAKELSE_AVBRUTT:
    case DeltakerStatusType.GJENNOMFORING_AVBRUTT:
    case DeltakerStatusType.GJENNOMFORING_AVLYST:
    case DeltakerStatusType.TAKKET_NEI_TIL_TILBUD:
    case DeltakerStatusType.IKKE_MOTT:
      return (
        <Tag size="small" variant="neutral">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.HAR_SLUTTET:
    case DeltakerStatusType.FULLFORT:
      return (
        <Tag size="small" variant="alt1">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.SOKT_INN:
    case DeltakerStatusType.VENTER_PA_OPPSTART:
    case DeltakerStatusType.TAKKET_JA_TIL_TILBUD:
    case DeltakerStatusType.AKTUELL:
      return (
        <Tag size="small" variant="alt3">
          {visningstekst}
        </Tag>
      );
    case DeltakerStatusType.VURDERES:
      return (
        <Tag size="small" variant="alt2">
          {visningstekst}
        </Tag>
      );
  }
}
