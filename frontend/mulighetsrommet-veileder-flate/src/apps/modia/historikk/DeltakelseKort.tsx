import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import { AktivDeltakelse, HistorikkForBrukerV2 } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import styles from "./DeltakelseKort.module.scss";
import classNames from "classnames";

interface Props {
  deltakelse: Partial<AktivDeltakelse> | Partial<HistorikkForBrukerV2>;
}

function erUtkast(
  deltakelse: Partial<AktivDeltakelse> | Partial<HistorikkForBrukerV2>,
): deltakelse is Partial<AktivDeltakelse> {
  return (deltakelse as Partial<AktivDeltakelse>).aktivStatus !== undefined;
}

function skalViseSistEndretDato(
  deltakelse: Partial<AktivDeltakelse> | Partial<HistorikkForBrukerV2>,
): boolean {
  if (erUtkast(deltakelse)) {
    return (
      !!deltakelse?.aktivStatus &&
      [
        AktivDeltakelse.aktivStatus.KLADD,
        AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING,
      ].includes(deltakelse.aktivStatus)
    );
  }

  if (erHistorisk(deltakelse)) {
    return (
      !!deltakelse?.historiskStatus &&
      [HistorikkForBrukerV2.historiskStatusType.AVBRUTT_UTKAST].includes(
        deltakelse.historiskStatus.historiskStatusType,
      )
    );
  }

  return false;
}

function erHistorisk(
  deltakelse: Partial<HistorikkForBrukerV2>,
): deltakelse is Partial<HistorikkForBrukerV2> {
  return (deltakelse as Partial<HistorikkForBrukerV2>).historiskStatus !== undefined;
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
        [styles.utkast]:
          erUtkast(deltakelse) &&
          deltakelse?.aktivStatus === AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING,
        [styles.kladd]:
          erUtkast(deltakelse) && deltakelse?.aktivStatus === AktivDeltakelse.aktivStatus.KLADD,
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
          {erHistorisk(deltakelse) && deltakelse?.historiskStatus ? (
            <Status status={deltakelse.historiskStatus.historiskStatusType} />
          ) : erUtkast(deltakelse) && deltakelse.aktivStatus ? (
            <Status status={deltakelse.aktivStatus} />
          ) : null}
          {erHistorisk(deltakelse) && deltakelse.beskrivelse ? (
            <BodyShort size="small">Årsak: {deltakelse.beskrivelse}</BodyShort>
          ) : null}
          {deltakelse.periode?.startdato && deltakelse.periode.sluttdato ? (
            <BodyShort size="small">
              {formaterDato(deltakelse.periode.startdato)} -{" "}
              {formaterDato(deltakelse.periode.sluttdato)}
            </BodyShort>
          ) : null}
          {skalViseSistEndretDato(deltakelse) && deltakelse.sistEndretdato ? (
            <span>Sist endret: {formaterDato(deltakelse.sistEndretdato)}</span>
          ) : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: HistorikkForBrukerV2.historiskStatusType | AktivDeltakelse.aktivStatus;
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
    case AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING:
      return (
        <Tag size="small" variant="info">
          Utkastet er delt og venter på godkjenning
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.VENTER_PA_OPPSTART:
      return (
        <Tag size="small" variant="alt3">
          Venter på oppstart
        </Tag>
      );

    case AktivDeltakelse.aktivStatus.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Deltar
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.KLADD:
      return (
        <Tag size="small" variant="warning">
          Kladden er ikke delt
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.SOKT_INN:
      return (
        <Tag size="small" variant="warning">
          Søkt inn
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.VURDERES:
      return (
        <Tag size="small" variant="warning">
          Vurderes
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.VENTELISTE:
      return (
        <Tag size="small" variant="warning">
          Venteliste
        </Tag>
      );
    case AktivDeltakelse.aktivStatus.PABEGYNT_REGISTRERING:
      return (
        <Tag size="small" variant="warning">
          Påbegynt registrering
        </Tag>
      );
  }
}
