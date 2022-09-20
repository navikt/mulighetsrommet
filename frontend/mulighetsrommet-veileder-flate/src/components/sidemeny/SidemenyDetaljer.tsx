import React from 'react';
import { Panel } from '@navikt/ds-react';
import { Tiltaksgjennomforing } from '../../core/api/models';
import Regelverksinfo from './Regelverksinfo';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { CopyToClipboard } from '@navikt/ds-react-internal';

const SidemenyDetaljer = () => {
  const { data } = useTiltaksgjennomforingByTiltaksnummer();
  if (!data) return null;

  const { tiltaksnummer, kontaktinfoArrangor, tiltakstype } = data;
  const oppstart = resolveOppstart(data);

  return (
    <>
      <Panel className="tiltakstype-detaljer__sidemeny">
        <div className="tiltakstype-detaljer__rad">
          <strong>Tiltaksnummer</strong>
          <div className="tiltakstype-detaljer__rad__info">
            {tiltaksnummer}
            <CopyToClipboard
              popoverText="Kopiert!"
              copyText={String(tiltaksnummer)}
              iconPosition="right"
              size="small"
              popoverPlacement="top"
              data-testid="knapp_kopier"
              className="kopiknapp"
            />
          </div>
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
          <span>{tiltakstype?.innsatsgruppe?.beskrivelse} </span>
        </div>

        <div className="tiltakstype-detaljer__rad">
          <strong>Oppstart</strong>
          <span>{oppstart}</span>
        </div>
        {(tiltakstype.regelverkFiler || tiltakstype.regelverkLenker) && (
          <div className="tiltakstype-detaljer__rad">
            <strong>Regelverk</strong>
            <Regelverksinfo regelverkFiler={tiltakstype.regelverkFiler} regelverkLenker={tiltakstype.regelverkLenker} />
          </div>
        )}
      </Panel>
    </>
  );
};

function resolveOppstart({ oppstart, oppstartsdato }: Tiltaksgjennomforing) {
  if (oppstart === 'midlertidig_stengt') return 'Midlertidig stengt';

  return oppstart === 'dato' && oppstartsdato ? new Intl.DateTimeFormat().format(new Date(oppstartsdato)) : 'Løpende';
}

export default SidemenyDetaljer;
