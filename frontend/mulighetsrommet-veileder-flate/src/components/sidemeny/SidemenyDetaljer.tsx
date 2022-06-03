import React from 'react';
import { Panel } from '@navikt/ds-react';
import './Sidemeny.less';
import Kopiknapp from '../kopiknapp/Kopiknapp';

interface SidemenyDetaljerProps {
  tiltaksnummer: string;
  tiltakstype: string;
  beskrivelse?: string;
  arrangor: string;
  innsatsgruppe: string | null;
  oppstartsdato?: string | null;
}

const SidemenyDetaljer = ({
  tiltaksnummer,
  tiltakstype,
  arrangor,
  innsatsgruppe,
  oppstartsdato,
  beskrivelse,
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
          <span>{tiltakstype}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Arrangør</strong>
          <span>{arrangor}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Innsatsgruppe</strong>
          <span>{innsatsgruppe} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Oppstart</strong>
          <span>{oppstartsdato} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Regelverk</strong>
          <span>{beskrivelse}</span>
        </div>
      </Panel>
    </>
  );
};

export default SidemenyDetaljer;
