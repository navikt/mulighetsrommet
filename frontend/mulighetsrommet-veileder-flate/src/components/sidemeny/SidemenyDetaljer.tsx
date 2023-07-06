import { BodyShort, Panel } from '@navikt/ds-react';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import Kopiknapp from '../kopiknapp/Kopiknapp';
import Regelverksinfo from './Regelverksinfo';
import styles from './Sidemenydetaljer.module.scss';
import { formaterDato, utledLopenummerFraTiltaksnummer } from '../../utils/Utils';
import { SanityTiltaksgjennomforing, SanityTiltakstype } from 'mulighetsrommet-api-client';

const SidemenyDetaljer = () => {
  const { data } = useTiltaksgjennomforingById();
  if (!data) return null;
  const { tiltaksnummer, kontaktinfoArrangor, tiltakstype, sluttdato, oppstartsdato, lokasjon } = data;
  const oppstart = resolveOppstart(data);

  const visDato = (tiltakstype: SanityTiltakstype, oppstart: string, oppstartsdato?: string, sluttdato?: string) => {
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

  const visSluttdato = (tiltakstype: SanityTiltakstype, sluttdato?: string, oppstartsdato?: string): boolean => {
    return (
      !!oppstartsdato &&
      !!sluttdato &&
      !!tiltakstype?.arenakode &&
      [
        SanityTiltakstype.arenakode.GRUPPEAMO,
        SanityTiltakstype.arenakode.JOBBK,
        SanityTiltakstype.arenakode.DIGIOPPARB,
        SanityTiltakstype.arenakode.GRUFAGYRKE,
        SanityTiltakstype.arenakode.ENKFAGYRKE,
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
  return oppstart === 'dato' && oppstartsdato ? formaterDato(oppstartsdato) : 'Løpende';
}

export default SidemenyDetaljer;
