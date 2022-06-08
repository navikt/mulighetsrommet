import React from 'react';
import { Panel } from '@navikt/ds-react';
import './Sidemeny.less';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import { Tiltaksgjennomforing, Tiltakstype } from '../../../../mulighetsrommet-api-client';

interface SidemenyDetaljerProps {
  tiltaksnummer: string;
  regelverk?: string;
  arrangor: string;
  oppstartsdato?: string | null;
  tiltakstype: Tiltakstype;
}

const SidemenyDetaljer = ({
  tiltaksnummer,
  arrangor,
  oppstartsdato,
  regelverk,
  tiltakstype,
}: SidemenyDetaljerProps) => {
  return (
    <>
      <Panel className="tiltakstype-detaljer__sidemeny">
        <div className="tiltakstype-detaljer__rad">
          <strong>Tiltaksnummer</strong>
          <span>
            {tiltaksnummer} <Kopiknapp kopitekst={tiltaksnummer} />
          </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Tiltakstype</strong>
          <span>{tiltakstype.tiltakstypeNavn}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Arrangør</strong>
          <span>{arrangor}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Innsatsgruppe</strong>
          <span>{tiltakstype.innsatsgruppe} </span>
        </div>

        {oppstartsdato && (
          <div className="tiltakstype-detaljer__rad">
            <strong>Oppstart</strong>
            <span>{oppstartsdato} </span>
          </div>
        )}

        <div className="tiltakstype-detaljer__rad">
          <strong>Regelverk</strong>
          <span>{regelverk}</span>
        </div>
      </Panel>
    </>
  );
};

export default SidemenyDetaljer;
