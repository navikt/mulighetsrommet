import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "../DetaljerInfo.module.scss";
import { Alert, BodyLong, Heading } from "@navikt/ds-react";
import { useTiltakstypeSanityData } from "../../api/tiltaksgjennomforing/useTiltakstypeSanityData";
import { PortableText } from "@portabletext/react";
import { Separator } from "../../components/detaljside/Metadata";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export const TiltaksgjennomforingRedInnhold = (props: Props) => {
  const { tiltaksgjennomforing } = props;
  const { data: tiltakstypeSanityData } = useTiltakstypeSanityData(
    tiltaksgjennomforing.tiltakstype.id,
  );

  return (
    <div className={styles.red_innhold_container}>
      {tiltakstypeSanityData?.beskrivelse && (
        <BodyLong size="large" className={styles.beskrivelse}>
          {tiltakstypeSanityData.beskrivelse}
        </BodyLong>
      )}
      {tiltaksgjennomforing.beskrivelse && (
        <BodyLong textColor="subtle" size="medium">
          {tiltaksgjennomforing.beskrivelse}
        </BodyLong>
      )}
      <Heading size="medium">For hvem</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.forHvem}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.forHvemInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.forHvem}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.forHvemInfoboks}
      />
      <Heading size="medium">Detaljer og innhold</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.detaljerOgInnhold}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.detaljerOgInnholdInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnhold}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.detaljerOgInnholdInfoboks}
      />
      <Heading size="medium">Påmelding og varighet</Heading>
      <DetaljerFane
        tiltaksgjennomforing={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighet}
        tiltaksgjennomforingAlert={tiltaksgjennomforing.faneinnhold?.pameldingOgVarighetInfoboks}
        tiltakstype={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighet}
        tiltakstypeAlert={tiltakstypeSanityData?.faneinnhold?.pameldingOgVarighetInfoboks}
      />
    </div>
  );
};

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
  if (!tiltaksgjennomforingAlert && !tiltakstypeAlert && !tiltaksgjennomforing && !tiltakstype) {
    return <></>;
  }

  return (
    <div>
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
    </div>
  );
};
