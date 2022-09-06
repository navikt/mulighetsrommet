import React from 'react';
import './TiltaksgjennomforingsHeader.less';
import { Heading, Ingress } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';
import useTiltaksgjennomforingByTiltaksnummer from '../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';

const TiltaksgjennomforingsHeader = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  if (!data) return null;

  const { tiltaksgjennomforingNavn, beskrivelse, tiltakstype } = data;
  return (
    <>
      <Heading
        level="1"
        size="xlarge"
        className="tiltaksgjennomforing__title"
        data-testid={`tiltaksgjennomforing-header_${kebabCase(tiltaksgjennomforingNavn)}`}
      >
        {tiltaksgjennomforingNavn}
      </Heading>
      {tiltakstype?.tiltakstypeNavn === 'Opplæring (Gruppe AMO)'
        ? beskrivelse && <Ingress className="tiltaksgjennomforing__beskrivelse">{beskrivelse}</Ingress>
        : null}
      {tiltakstype.beskrivelse && (
        <Ingress className="tiltaksgjennomforing__beskrivelse">{tiltakstype.beskrivelse}</Ingress>
      )}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
