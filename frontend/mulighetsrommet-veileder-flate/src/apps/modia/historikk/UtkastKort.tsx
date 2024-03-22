import { Heading, HStack, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { AktivDeltakelse } from "mulighetsrommet-api-client";
import { formaterDato } from "@/utils/Utils";
import styles from "./UtkastKort.module.scss";
import { ModiaRoute, resolveModiaRoute } from "@/apps/modia/ModiaRoute";

interface Props {
  utkast: AktivDeltakelse;
}

export function UtkastKort({ utkast }: Props) {
  const { tiltakstype, tittel, aktivStatus, innsoktDato, sistEndretdato } = utkast;

  const deltakelseRoute = resolveModiaRoute({
    route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE,
    deltakerId: utkast.deltakerId,
  });

  return (
    <LinkPanel
      onClick={deltakelseRoute.navigate}
      className={classNames(styles.panel, {
        [styles.utkast]: aktivStatus === AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING,
        [styles.kladd]: aktivStatus === AktivDeltakelse.aktivStatus.KLADD,
      })}
    >
      <VStack gap="2">
        <HStack gap="10">
          <small>{tiltakstype.navn.toUpperCase()}</small>
          {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
        </HStack>
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
        <HStack align={"center"} gap="5">
          <Status status={aktivStatus} />
          <span>Sist endret: {formaterDato(sistEndretdato)}</span>
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: AktivDeltakelse.aktivStatus;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING:
      return (
        <Tag size="small" variant="info">
          Utkast til påmelding
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
