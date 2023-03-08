import { Panel } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import Regelverksinfo from './Regelverksinfo';
import styles from './Sidemenydetaljer.module.scss';
import { formaterDato } from '../../utils/Utils';
import { SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';

const SidemenyDetaljer = () => {
  const { data } = useTiltaksgjennomforingById();
  if (!data) return null;

  const { tiltaksnummer, kontaktinfoArrangor, tiltakstype } = data;
  const oppstart = resolveOppstart(data);

  return (
    <>
      <Panel className={styles.panel}>
        {tiltaksnummer && (
          <div className={styles.rad}>
            <strong>Tiltaksnummer</strong>
            <div className={styles.info}>
              {tiltaksnummer} <Kopiknapp kopitekst={String(tiltaksnummer)} dataTestId="knapp_kopier" />
            </div>
          </div>
        )}

        <div className={styles.rad}>
          <strong>Tiltakstype</strong>
          <span>{tiltakstype.tiltakstypeNavn}</span>
        </div>

        {kontaktinfoArrangor?.selskapsnavn ? (
          <div className={styles.rad}>
            <strong>Arrangør</strong>
            <span>{kontaktinfoArrangor.selskapsnavn}</span>
          </div>
        ) : null}

        <div className={styles.rad}>
          <strong>Innsatsgruppe</strong>
          <span>{tiltakstype?.innsatsgruppe?.beskrivelse} </span>
        </div>

        <div className={styles.rad}>
          <strong>Oppstart</strong>
          <span>{oppstart}</span>
        </div>
        {(tiltakstype.regelverkFiler || tiltakstype.regelverkLenker) && (
          <div className={styles.rad}>
            <strong>Regelverk</strong>
            <Regelverksinfo regelverkFiler={tiltakstype.regelverkFiler} regelverkLenker={tiltakstype.regelverkLenker} />
          </div>
        )}
      </Panel>
    </>
  );
};

function resolveOppstart({ oppstart, oppstartsdato }: SanityTiltaksgjennomforing) {
  if (oppstart === 'midlertidig_stengt') return 'Midlertidig stengt';
  return oppstart === 'dato' && oppstartsdato ? formaterDato(oppstartsdato) : 'Løpende';
}

export default SidemenyDetaljer;
