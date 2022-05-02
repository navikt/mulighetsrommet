import React from 'react';
import { Panel } from '@navikt/ds-react';
import './Sidemeny.less';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import { Tiltakskode } from '../../../../mulighetsrommet-api-client';

interface SidemenyDetaljerProps {
  tiltaksnummer: string;
  tiltakstype: Tiltakskode;
  leverandor: string;
  innsatsgruppe: number | null;
  oppstartsdato: string | null;
  beskrivelse: string;
}

const SidemenyDetaljer = ({
  tiltaksnummer,
  tiltakstype,
  leverandor,
  innsatsgruppe,
  oppstartsdato,
  beskrivelse,
}: SidemenyDetaljerProps) => {
  return (
    <>
      <Panel className="tiltakstype-detaljer__sidemeny">
        <div className="tiltakstype-detaljer__rad">
          <span>Tiltaksnummer</span>
          <span>
            {tiltaksnummer} <Kopiknapp kopitekst={tiltaksnummer} />
          </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <span>Tiltakstype</span>
          <span>{tiltakstype}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <span>Leverandør</span>
          <span>{leverandor}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <span>Innsatsgruppe</span>
          <span>{innsatsgruppe} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <span>Oppstart</span>
          <span>{oppstartsdato} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <span>Beskrivelse</span>
          <span>{beskrivelse} </span>
        </div>
      </Panel>
    </>
  );
};

export default SidemenyDetaljer;
