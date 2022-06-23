import React from 'react';
import { Panel } from '@navikt/ds-react';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import { Tiltaksgjennomforing } from '../../api/models';
import Regelverksinfo from './Regelverksinfo';

interface SidemenyDetaljerProps {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

const SidemenyDetaljer = ({ tiltaksgjennomforing }: SidemenyDetaljerProps) => {
  const { tiltaksnummer, kontaktinfoArrangor, tiltakstype } = tiltaksgjennomforing;
  const oppstart = resolveOppstart(tiltaksgjennomforing);

  return (
    <>
      <Panel className="tiltakstype-detaljer__sidemeny">
        <div className="tiltakstype-detaljer__rad">
          <strong>Tiltaksnummer</strong>
          <span>
            {tiltaksnummer} <Kopiknapp kopitekst={String(tiltaksnummer)} />
          </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Tiltakstype</strong>
          <span>{tiltakstype.tiltakstypeNavn}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Arrangør</strong>
          <span>{kontaktinfoArrangor.selskapsnavn}</span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Innsatsgruppe</strong>
          <span>{tiltakstype.innsatsgruppe.beskrivelse} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Oppstart</strong>
          <span>{oppstart}</span>
        </div>
        {(tiltakstype.regelverkFiler || tiltakstype.regelverkLenker) && (
          <div className="tiltakstype-detaljer__rad">
            <strong>Regelverk</strong>
            <Regelverksinfo regelverkFiler={tiltakstype.regelverkFiler} regelverkLenker={tiltakstype.regelverkLenker}/>
          </div>)
        }
      </Panel>
    </>
  );
};

function resolveOppstart({ oppstart, oppstartsdato }: Tiltaksgjennomforing) {
  return oppstart === 'dato' && oppstartsdato ? new Intl.DateTimeFormat().format(new Date(oppstartsdato)) : 'Løpende';
}

export default SidemenyDetaljer;
