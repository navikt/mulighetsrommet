import { BodyShort, Panel } from "@navikt/ds-react";
import {
  TiltaksgjennomforingOppstartstype,
  Tiltakskode,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import { formaterDato, utledLopenummerFraTiltaksnummer } from "../../utils/Utils";
import Kopiknapp from "../kopiknapp/Kopiknapp";
import Regelverksinfo from "./Regelverksinfo";
import styles from "./Sidemenydetaljer.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const SidemenyDetaljer = ({ tiltaksgjennomforing }: Props) => {
  const { tiltaksnummer, arrangor, tiltakstype, sluttdato, oppstartsdato, stedForGjennomforing } =
    tiltaksgjennomforing;
  const oppstart = resolveOppstart(tiltaksgjennomforing);

  const visDato = (
    tiltakstype: VeilederflateTiltakstype,
    oppstart: string,
    oppstartsdato?: string,
    sluttdato?: string,
  ) => {
    return (
      <div className={styles.rad}>
        <BodyShort size="small" className={styles.tittel}>
          {visSluttdato(tiltakstype, sluttdato, oppstartsdato) ? "Varighet" : "Oppstart"}
        </BodyShort>
        <BodyShort size="small">
          {visSluttdato(tiltakstype, sluttdato, oppstartsdato)
            ? `${formaterDato(oppstartsdato!!)} - ${formaterDato(sluttdato!!)}`
            : oppstart}
        </BodyShort>
      </div>
    );
  };

  const visSluttdato = (
    tiltakstype: VeilederflateTiltakstype,
    sluttdato?: string,
    oppstartsdato?: string,
  ): boolean => {
    return (
      !!oppstartsdato &&
      !!sluttdato &&
      !!tiltakstype?.arenakode &&
      [
        Tiltakskode.GRUPPEAMO,
        Tiltakskode.JOBBK,
        Tiltakskode.DIGIOPPARB,
        Tiltakskode.GRUFAGYRKE,
        Tiltakskode.ENKFAGYRKE,
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
              <Kopiknapp
                kopitekst={utledLopenummerFraTiltaksnummer(tiltaksnummer)}
                dataTestId="knapp_kopier"
              />
            </div>
          </div>
        )}

        {stedForGjennomforing && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Sted for gjennomføring
            </BodyShort>
            <BodyShort size="small">{stedForGjennomforing}</BodyShort>
          </div>
        )}

        <div className={styles.rad}>
          <BodyShort size="small" className={styles.tittel}>
            Tiltakstype
          </BodyShort>
          <BodyShort size="small">{tiltakstype.navn} </BodyShort>
        </div>

        {arrangor?.selskapsnavn && (
          <div className={styles.rad}>
            <BodyShort size="small" className={styles.tittel}>
              Arrangør
            </BodyShort>
            <BodyShort size="small">{arrangor.selskapsnavn}</BodyShort>
          </div>
        )}

        <div className={styles.rad}>
          <BodyShort title="Minimum krav innsatsgruppe" size="small" className={styles.tittel}>
            <abbr title="Minimum">Min</abbr>. innsatsgruppe
          </BodyShort>
          <BodyShort size="small">{tiltakstype?.innsatsgruppe?.beskrivelse} </BodyShort>
        </div>

        {visDato(tiltakstype, oppstart, oppstartsdato, sluttdato)}

        {tiltakstype.regelverkLenker && (
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
  return oppstart === TiltaksgjennomforingOppstartstype.FELLES && oppstartsdato
    ? formaterDato(oppstartsdato)
    : "Løpende";
}

export default SidemenyDetaljer;
