import {Alert} from "@navikt/ds-react";
import {PortableText} from "@portabletext/react";
import React from "react";

const DetaljerFane = ({
                        tiltaksgjennomforingAlert,
                        tiltakstypeAlert,
                        tiltaksgjennomforing,
                        tiltakstype,
                      }: DetaljerFaneProps) => {
  return (
    <div className="tiltaksdetaljer__maksbredde">
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
    </div>
  );
};
