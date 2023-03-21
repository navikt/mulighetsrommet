import { BodyShort, Panel } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import Regelverksinfo from './Regelverksinfo';
import styles from './Sidemenydetaljer.module.scss';
import { formaterDato, utledLopenummerFraTiltaksnummer } from '../../utils/Utils';
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
            <BodyShort size="small" className={styles.tittel}>
              Tiltaksnummer
            </BodyShort>
            <div className={styles.tiltaksnummer}>
              <BodyShort size="small">{utledLopenummerFraTiltaksnummer(tiltaksnummer)}</BodyShort>
              <Kopiknapp kopitekst={utledLopenummerFraTiltaksnummer(tiltaksnummer)} dataTestId="knapp_kopier" />
            </div>
          </div>
        )}

        <div className={styles.rad}>
          <BodyShort size="small" className={styles.tittel}>
            Tiltakstype
          </BodyShort>
          <BodyShort size="small">{tiltakstype.tiltakstypeNavn} </BodyShort>
        </div>

        {kontaktinfoArrangor?.selskapsnavn && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Arrangør
            </BodyShort>
            <BodyShort size="small">{kontaktinfoArrangor.selskapsnavn}</BodyShort>
          </div>
        )}

        <div className={styles.rad}>
          <BodyShort size="small" className={styles.tittel}>
            Innsatsgruppe
          </BodyShort>
          <BodyShort size="small">{tiltakstype?.innsatsgruppe?.beskrivelse} </BodyShort>
        </div>

        <div className={styles.rad}>
          <BodyShort size="small" className={styles.tittel}>
            Oppstart
          </BodyShort>
          <BodyShort size="small">{oppstart}</BodyShort>
        </div>

        {(tiltakstype.regelverkFiler || tiltakstype.regelverkLenker) && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Regelverk
            </BodyShort>
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
