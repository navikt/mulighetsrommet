import { BodyShort, Panel } from '@navikt/ds-react';
import { VeilederflateTiltaksgjennomforing, VeilederflateTiltakstype } from 'mulighetsrommet-api-client';
import { formaterDato, utledLopenummerFraTiltaksnummer } from '../../utils/Utils';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import Regelverksinfo from './Regelverksinfo';
import styles from './Sidemenydetaljer.module.scss';

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const SidemenyDetaljer = ({ tiltaksgjennomforing }: Props) => {
  const { tiltaksnummer, kontaktinfoArrangor, tiltakstype, sluttdato, oppstartsdato, lokasjon } = tiltaksgjennomforing;
  const oppstart = resolveOppstart(tiltaksgjennomforing);

  const visDato = (
    tiltakstype: VeilederflateTiltakstype,
    oppstart: string,
    oppstartsdato?: string,
    sluttdato?: string
  ) => {
    return (
      <div className={styles.rad}>
        <BodyShort size="small" className={styles.tittel}>
          {visSluttdato(tiltakstype, sluttdato, oppstartsdato) ? 'Varighet' : 'Oppstart'}
        </BodyShort>
        <BodyShort size="small">
          {visSluttdato(tiltakstype, sluttdato, oppstartsdato)
            ? `${formaterDato(oppstartsdato!!)} - ${formaterDato(sluttdato!!)}`
            : oppstart}
        </BodyShort>
      </div>
    );
  };

  const visSluttdato = (tiltakstype: VeilederflateTiltakstype, sluttdato?: string, oppstartsdato?: string): boolean => {
    return (
      !!oppstartsdato &&
      !!sluttdato &&
      !!tiltakstype?.arenakode &&
      [
        VeilederflateTiltakstype.arenakode.GRUPPEAMO,
        VeilederflateTiltakstype.arenakode.JOBBK,
        VeilederflateTiltakstype.arenakode.DIGIOPPARB,
        VeilederflateTiltakstype.arenakode.GRUFAGYRKE,
        VeilederflateTiltakstype.arenakode.ENKFAGYRKE,
      ].includes(tiltakstype?.arenakode)
    );
  };

  return (
    <>
      <Panel className={styles.panel} id="sidemeny">
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

        {lokasjon && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Lokasjon
            </BodyShort>
            <BodyShort size="small">{lokasjon}</BodyShort>
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

        {visDato(tiltakstype, oppstart, oppstartsdato, sluttdato)}

        {(tiltakstype.regelverkLenker) && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Regelverk
            </BodyShort>
            <Regelverksinfo regelverkLenker={tiltakstype.regelverkLenker} />
          </div>
        )}
      </Panel>
    </>
  );
};

function resolveOppstart({ oppstart, oppstartsdato }: VeilederflateTiltaksgjennomforing) {
  return oppstart === 'dato' && oppstartsdato ? formaterDato(oppstartsdato) : 'Løpende';
}

export default SidemenyDetaljer;
