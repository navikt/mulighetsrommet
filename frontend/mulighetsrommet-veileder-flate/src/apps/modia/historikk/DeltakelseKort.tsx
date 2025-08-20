import {
  ArbeidsgiverAvtaleStatus,
  ArenaDeltakerStatus,
  Deltakelse,
  DeltakelseEierskap,
  DeltakerStatusType as DeltakerStatusType,
} from "@api-client";
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
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL } from "@/constants";
import { Link } from "react-router";

interface Props {
  deltakelse: Deltakelse;
  aktiv: boolean;
}

export function DeltakelseKort({ deltakelse, aktiv }: Props) {
  return (
    <Box
      background="bg-default"
      borderRadius="medium"
      padding="5"
      className={
        isUtkast(deltakelse.status.type)
          ? "border-2 border-dashed border-border-info"
          : isKladd(deltakelse.status.type)
            ? "border-2 border-dashed border-border-warning"
            : ""
      }
    >
      <HGrid columns="1fr 20%" align="center">
        <Innhold deltakelse={deltakelse} />
        <Knapper deltakelse={deltakelse} aktiv={aktiv} />
      </HGrid>
    </Box>
  );
}

function Knapper({ deltakelse, aktiv }: Props) {
  switch (deltakelse.eierskap) {
    case DeltakelseEierskap.ARENA:
      return null;
    case DeltakelseEierskap.TEAM_KOMET: {
      const deltakelseRoute = resolveModiaRoute({
        route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
        deltakerId: deltakelse.id,
      });
      const gjennomforingId = "gjennomforingId" in deltakelse ? deltakelse.gjennomforingId : null;
      return (
        <VStack gap="2">
          <Button variant="secondary" onClick={deltakelseRoute.navigate} size="small">
            Gå til deltakelse
          </Button>
          {aktiv && gjennomforingId && (
            <Link
              to={`/arbeidsmarkedstiltak/tiltak/${gjennomforingId}`}
              className="text-center no-underline text-[16px] hover:underline"
            >
              Gå til tiltak
            </Link>
          )}
        </VStack>
      );
    }
    case DeltakelseEierskap.TEAM_TILTAK: {
      const link = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/avtale/${deltakelse.id}?part=VEILEDER`;
      return (
        <Lenkeknapp variant="secondary" to={link} size="small">
          Gå til avtale
        </Lenkeknapp>
      );
    }
  }
}

function isKladd(type: ArenaDeltakerStatus | DeltakerStatusType | ArbeidsgiverAvtaleStatus) {
  return type === DeltakerStatusType.KLADD || type === ArbeidsgiverAvtaleStatus.PAABEGYNT;
}

function isUtkast(type: ArenaDeltakerStatus | DeltakerStatusType | ArbeidsgiverAvtaleStatus) {
  return (
    type === DeltakerStatusType.UTKAST_TIL_PAMELDING ||
    type === DeltakerStatusType.PABEGYNT_REGISTRERING ||
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
  status: ArenaDeltakerStatus | DeltakerStatusType | ArbeidsgiverAvtaleStatus;
  visningstekst: string;
}

function Status({ status, visningstekst }: StatusProps) {
  const { variant, style } = resolveStatusStyle(status);
  return (
    <Tag size="small" variant={variant} style={style}>
      {visningstekst}
    </Tag>
  );
}

function resolveStatusStyle(
  status: ArenaDeltakerStatus | DeltakerStatusType | ArbeidsgiverAvtaleStatus,
): {
  variant: TagProps["variant"];
  style?: any;
} {
  switch (status) {
    case DeltakerStatusType.DELTAR:
    case ArenaDeltakerStatus.GJENNOMFORES:
    case ArbeidsgiverAvtaleStatus.GJENNOMFORES:
      return {
        variant: "success",
        style: {
          backgroundColor: "var(--a-surface-default)",
          border: "1px solid var(--a-border-default)",
        },
      };
    case DeltakerStatusType.PABEGYNT_REGISTRERING:
    case DeltakerStatusType.KLADD:
    case ArbeidsgiverAvtaleStatus.PAABEGYNT:
      return { variant: "warning" };

    case ArenaDeltakerStatus.INFORMASJONSMOTE:
    case ArenaDeltakerStatus.TILBUD:
    case DeltakerStatusType.UTKAST_TIL_PAMELDING:
    case ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART:
    case ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING:
      return { variant: "info" };

    case DeltakerStatusType.IKKE_AKTUELL:
    case DeltakerStatusType.FEILREGISTRERT:
    case DeltakerStatusType.AVBRUTT:
    case DeltakerStatusType.AVBRUTT_UTKAST:
    case ArenaDeltakerStatus.IKKE_AKTUELL:
    case ArenaDeltakerStatus.FEILREGISTRERT:
    case ArenaDeltakerStatus.AVSLAG:
    case ArenaDeltakerStatus.DELTAKELSE_AVBRUTT:
    case ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT:
    case ArenaDeltakerStatus.GJENNOMFORING_AVLYST:
    case ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD:
    case ArenaDeltakerStatus.IKKE_MOTT:
    case ArbeidsgiverAvtaleStatus.AVBRUTT:
    case ArbeidsgiverAvtaleStatus.ANNULLERT:
      return { variant: "neutral" };

    case DeltakerStatusType.VENTELISTE:
    case DeltakerStatusType.HAR_SLUTTET:
    case DeltakerStatusType.FULLFORT:
    case ArenaDeltakerStatus.VENTELISTE:
    case ArenaDeltakerStatus.FULLFORT:
    case ArbeidsgiverAvtaleStatus.AVSLUTTET:
      return { variant: "alt1" };

    case DeltakerStatusType.VENTER_PA_OPPSTART:
    case ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD:
    case ArenaDeltakerStatus.AKTUELL:
      return { variant: "alt3" };

    case DeltakerStatusType.SOKT_INN:
    case DeltakerStatusType.VURDERES:
      return { variant: "alt2" };
  }
}
