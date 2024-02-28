import { BodyShort, HStack, Heading, LinkPanel, Tag, VStack } from "@navikt/ds-react";
import { UtkastForBrukerFraKomet } from "mulighetsrommet-api-client";
import { formaterDato } from "../../../utils/Utils";
import styles from "./UtkastKort.module.scss";
import classNames from "classnames";

interface Props {
  utkast: UtkastForBrukerFraKomet;
}
export function UtkastKort({ utkast }: Props) {
  const { tiltakstype, tittel, status, tiltaksgjennomforingId, beskrivelse, innsoktDato } = utkast;
  return (
    <LinkPanel
      href={`/arbeidsmarkedstiltak/tiltak/${tiltaksgjennomforingId}`}
      className={classNames(styles.panel, {
        [styles.utkast]: status.navn === UtkastForBrukerFraKomet.navn.UTKAST_PAMELDING,
        [styles.kladd]: status.navn === UtkastForBrukerFraKomet.navn.KLADD,
      })}
    >
      <VStack gap="2">
        <HStack gap="10">
          <small>{tiltakstype.toUpperCase()}</small>
          {innsoktDato ? <small>Søkt inn: {formaterDato(innsoktDato)}</small> : null}
        </HStack>
        <Heading size="medium" level="4">
          {tittel}
        </Heading>
        <HStack align={"center"} gap="5">
          <Status status={status.navn} />
          {beskrivelse ? <BodyShort size="small">Årsak: {beskrivelse}</BodyShort> : null}
        </HStack>
      </VStack>
    </LinkPanel>
  );
}

interface StatusProps {
  status: UtkastForBrukerFraKomet.navn;
}

function Status({ status }: StatusProps) {
  switch (status) {
    case UtkastForBrukerFraKomet.navn.DELTAR:
      return (
        <Tag size="small" variant="success" className={styles.deltarStatus}>
          Deltar
        </Tag>
      );
    case UtkastForBrukerFraKomet.navn.KLADD:
      return (
        <Tag size="small" variant="warning">
          Kladd
        </Tag>
      );
    case UtkastForBrukerFraKomet.navn.UTKAST_PAMELDING:
      return (
        <Tag size="small" variant="info">
          Utkast til påmelding
        </Tag>
      );
    case UtkastForBrukerFraKomet.navn.VENTER_PA_OPPSTART:
      return (
        <Tag size="small" variant="alt3">
          Venter på oppstart
        </Tag>
      );
  }
}
