import { Alert, BodyLong } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import styles from "./Detaljerfane.module.scss";
import FaneTiltaksinformasjon from "./FaneTiltaksinformasjon";
import { Separator } from "../../utils/Separator";

interface DetaljerFaneProps {
  tiltaksgjennomforingAlert?: string;
  tiltakstypeAlert?: string;
  tiltaksgjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  tiltaksgjennomforingAlert,
  tiltakstypeAlert,
  tiltaksgjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  return (
    <FaneTiltaksinformasjon
      harInnhold={
        tiltaksgjennomforingAlert || tiltakstypeAlert || tiltaksgjennomforing || tiltakstype
      }
    >
      {tiltakstypeAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer_alert}>
          {tiltakstypeAlert}
        </Alert>
      )}
      <BodyLong as="div" size="small">
        <PortableText value={tiltakstype} />
      </BodyLong>
      {(tiltaksgjennomforingAlert || tiltaksgjennomforing) && <Separator />}
      {tiltaksgjennomforingAlert && (
        <Alert variant="info" className={styles.tiltaksdetaljer_alert}>
          {tiltaksgjennomforingAlert}
        </Alert>
      )}
      <BodyLong as="div" textColor="subtle" size="small">
        <PortableText value={tiltaksgjennomforing} />
      </BodyLong>
    </FaneTiltaksinformasjon>
  );
};

export default DetaljerFane;
