import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import { formaterDato } from "../../../utils/Utils";
import styles from "./UtkastKort.module.scss";
import classNames from "classnames";
import { AktivDeltakelse } from "mulighetsrommet-api-client";

interface Props {
  utkast: AktivDeltakelse;
}
export function UtkastKort({ utkast }: Props) {
  const { tiltakstype, tittel, aktivStatus, beskrivelse, innsoktDato } = utkast;
  return (
    <LinkPanel
      href="#" // TODO Fiks korrekt url til Komets løsning for påmelding
      className={classNames(styles.panel, {
        [styles.utkast]: aktivStatus?.navn === AktivDeltakelse.navn.UTKAST_PAMELDING,
        [styles.kladd]: aktivStatus?.navn === AktivDeltakelse.navn.KLADD,
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
          <Status status={aktivStatus.navn} />
          {beskrivelse ? <BodyShort size="small">Årsak: {beskrivelse}</BodyShort> : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: AktivDeltakelse.navn;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case AktivDeltakelse.navn.UTKAST_PAMELDING:
      return (
        <Tag size="small" variant="info">
          Utkast til påmelding
        </Tag>
      );
    case AktivDeltakelse.navn.VENTER_PA_OPPSTART:
      return (
        <Tag size="small" variant="alt3">
          Venter på oppstart
        </Tag>
      );

    case AktivDeltakelse.navn.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Deltar
        </Tag>
      );
    case AktivDeltakelse.navn.KLADD:
      return (
        <Tag size="small" variant="warning">
          Kladden er ikke delt
        </Tag>
      );
    case AktivDeltakelse.navn.SOKT_INN:
      return (
        <Tag size="small" variant="warning">
          Søkt inn
        </Tag>
      );
    case AktivDeltakelse.navn.VURDERES:
      return (
        <Tag size="small" variant="warning">
          Vurderes
        </Tag>
      );
    case AktivDeltakelse.navn.VENTELISTE:
      return (
        <Tag size="small" variant="warning">
          Venteliste
        </Tag>
      );
    case AktivDeltakelse.navn.PABEGYNT_REGISTRERING:
      return (
        <Tag size="small" variant="warning">
          Påbegynt registrering
        </Tag>
      );
  }
}
