import React from 'react';
import { Button, Panel } from '@navikt/ds-react';
import './Sidemeny.less';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import { Tiltaksgjennomforing, Tiltakstype } from '../../../../mulighetsrommet-api-client';
import Lenke from '../lenke/Lenke';

interface SidemenyDetaljerProps {
  tiltaksnummer: string;
  arrangor: string;
  oppstartsdato?: string | null;
  tiltakstype: Tiltakstype;
}

const SidemenyDetaljer = ({ tiltaksnummer, arrangor, oppstartsdato, tiltakstype }: SidemenyDetaljerProps) => {
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

        {(tiltakstype.regelverkFil || tiltakstype.regelverkLenke) && (
          <div className="tiltakstype-detaljer__rad">
            <strong>Regelverk</strong>
            <div className="tiltakstype-detaljer__regelverk">
              {tiltakstype.regelverkFil && <span>{tiltakstype.regelverkFilNavn}</span>}
              {tiltakstype.regelverkLenke && (
                <span>
                  <Lenke to={tiltakstype.regelverkLenke}>{tiltakstype.regelverkLenkeNavn}</Lenke>
                </span>
              )}
            </div>
          </div>
        )}
      </Panel>
    </>
  );
};

export default SidemenyDetaljer;
