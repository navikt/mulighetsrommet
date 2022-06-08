import React from 'react';
import { Alert } from '@navikt/ds-react';
import { PortableText } from '@portabletext/react';
import './TiltaksdetaljerFane.less';

interface FaneMalProps {
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
}: FaneMalProps) => {
  return (
    <>
      {tiltakstypeAlert && (
        <Alert variant="info" className="tiltaksdetaljer__alert">
          {tiltakstypeAlert}
        </Alert>
      )}
      {tiltaksgjennomforingAlert && (
        <Alert variant="info" className="tiltaksdetaljer__alert">
          {tiltaksgjennomforingAlert}
        </Alert>
      )}
      <PortableText value={tiltakstype} />
      <PortableText value={tiltaksgjennomforing} />
    </>
  );
};

export default DetaljerFane;
