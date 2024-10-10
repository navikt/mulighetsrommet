import {
  ArbeidsgiverAvtaleStatus,
  ArenaDeltakerStatus,
  Deltakelse,
  GruppetiltakDeltakerStatus,
} from "@mr/api-client";
import {
  BodyShort,
  Box,
  Button,
  Heading,
  HGrid,
  HStack,
  Tag,
  TagProps,
  VStack,
} from "@navikt/ds-react";
import classNames from "classnames";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import styles from "./DeltakelseKort.module.scss";
import { ReactNode } from "react";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_AVTALER_URL } from "@/constants";

type Size = "small" | "medium" | "large";

interface Props {
  deltakelse: Deltakelse;
}

export function DeltakelseKort({ deltakelse }: Props) {
  return (
    <Wrapper size="medium" status={deltakelse.status.type}>
      <DeltakelseInnhold deltakelse={deltakelse} />
    </Wrapper>
  );
}

function DeltakelseInnhold({ deltakelse }: Props) {
  if (deltakelse.eierskap === "ARENA") {
    return <Innhold deltakelse={deltakelse} />;
  } else if (deltakelse.eierskap === "TEAM_KOMET") {
    const deltakelseRoute = resolveModiaRoute({
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
      deltakerId: deltakelse.id,
    });

    return (
      <HGrid columns="1fr 20%" align="center">
        <Innhold deltakelse={deltakelse} />
        <Button variant="secondary" onClick={deltakelseRoute.navigate} size="small">
          Gå til deltakelse
        </Button>
      </HGrid>
    );
  } else if (deltakelse.eierskap === "TEAM_TILTAK") {
    const link = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_AVTALER_URL}/tiltaksgjennomforing/avtale/${deltakelse.id}?part=VEILEDER`;
    return (
      <HGrid columns="1fr 20%" align="center">
        <Innhold deltakelse={deltakelse} />
        <Lenkeknapp variant="secondary" to={link} size="small">
          Gå til avtale
        </Lenkeknapp>
      </HGrid>
    );
  }
}

function Wrapper({
  status,
  size,
  children,
}: {
  status: ArenaDeltakerStatus | GruppetiltakDeltakerStatus | ArbeidsgiverAvtaleStatus;
  size: Size;
  children: ReactNode;
}) {
  return (
    <Box
      background="bg-default"
      borderRadius="medium"
      padding={size === "small" ? "2" : size === "medium" ? "5" : "8"}
      className={classNames(styles.panel, {
        [styles.utkast]: isUtkast(status),
        [styles.kladd]: isKladd(status),
      })}
    >
      {children}
    </Box>
  );
}

function isKladd(
  type: ArenaDeltakerStatus | GruppetiltakDeltakerStatus | ArbeidsgiverAvtaleStatus,
) {
  return type === GruppetiltakDeltakerStatus.KLADD || type === ArbeidsgiverAvtaleStatus.PAABEGYNT;
}

function isUtkast(
  type: ArenaDeltakerStatus | GruppetiltakDeltakerStatus | ArbeidsgiverAvtaleStatus,
) {
  return (
    type === GruppetiltakDeltakerStatus.UTKAST_TIL_PAMELDING ||
    type === GruppetiltakDeltakerStatus.PABEGYNT_REGISTRERING ||
    type === ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING
  );
}

function Innhold({ deltakelse }: { deltakelse: Deltakelse }) {
  const { tiltakstypeNavn, status, periode, tittel, innsoktDato } = deltakelse;
  const aarsak = "aarsak" in status ? status.aarsak : null;
  return (
    <VStack gap="2">
      <HStack gap="10">
        <small>{tiltakstypeNavn.toUpperCase()}</small>
        {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
      </HStack>
      {tittel ? (
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
      ) : null}
      <HStack align={"end"} gap="5">
        <Status status={status.type} visningstekst={status.visningstekst} />
        {aarsak ? <BodyShort size="small">Årsak: {aarsak}</BodyShort> : null}
        {periode?.startDato ? (
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

interface StatusProps {
  status: ArenaDeltakerStatus | GruppetiltakDeltakerStatus | ArbeidsgiverAvtaleStatus;
  visningstekst: string;
}

function Status({ status, visningstekst }: StatusProps) {
  const { variant, className } = resolveStatusStyle(status);
  return (
    <Tag size="small" variant={variant} className={className}>
      {visningstekst}
    </Tag>
  );
}

function resolveStatusStyle(
  status: ArenaDeltakerStatus | GruppetiltakDeltakerStatus | ArbeidsgiverAvtaleStatus,
): {
  variant: TagProps["variant"];
  className?: string;
} {
  switch (status) {
    case GruppetiltakDeltakerStatus.DELTAR:
    case ArenaDeltakerStatus.GJENNOMFORES:
    case ArbeidsgiverAvtaleStatus.GJENNOMFORES:
      return { variant: "success", className: styles.deltarStatus };

    case GruppetiltakDeltakerStatus.PABEGYNT_REGISTRERING:
    case GruppetiltakDeltakerStatus.KLADD:
    case ArbeidsgiverAvtaleStatus.PAABEGYNT:
      return { variant: "warning" };

    case ArenaDeltakerStatus.INFORMASJONSMOTE:
    case ArenaDeltakerStatus.TILBUD:
    case GruppetiltakDeltakerStatus.UTKAST_TIL_PAMELDING:
    case ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART:
    case ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING:
      return { variant: "info" };

    case GruppetiltakDeltakerStatus.IKKE_AKTUELL:
    case GruppetiltakDeltakerStatus.FEILREGISTRERT:
    case GruppetiltakDeltakerStatus.VENTELISTE:
    case GruppetiltakDeltakerStatus.AVBRUTT:
    case GruppetiltakDeltakerStatus.AVBRUTT_UTKAST:
    case ArenaDeltakerStatus.IKKE_AKTUELL:
    case ArenaDeltakerStatus.FEILREGISTRERT:
    case ArenaDeltakerStatus.VENTELISTE:
    case ArenaDeltakerStatus.AVSLAG:
    case ArenaDeltakerStatus.DELTAKELSE_AVBRUTT:
    case ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT:
    case ArenaDeltakerStatus.GJENNOMFORING_AVLYST:
    case ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD:
    case ArenaDeltakerStatus.IKKE_MOTT:
    case ArbeidsgiverAvtaleStatus.AVBRUTT:
    case ArbeidsgiverAvtaleStatus.ANNULLERT:
      return { variant: "neutral" };

    case GruppetiltakDeltakerStatus.HAR_SLUTTET:
    case GruppetiltakDeltakerStatus.FULLFORT:
    case ArenaDeltakerStatus.FULLFORT:
    case ArbeidsgiverAvtaleStatus.AVSLUTTET:
      return { variant: "alt1" };

    case GruppetiltakDeltakerStatus.SOKT_INN:
    case GruppetiltakDeltakerStatus.VENTER_PA_OPPSTART:
    case ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD:
    case ArenaDeltakerStatus.AKTUELL:
      return { variant: "alt3" };

    case GruppetiltakDeltakerStatus.VURDERES:
      return { variant: "alt2" };
  }
}
