import { BodyShort, Box } from "@navikt/ds-react";
import {
  TiltaksgjennomforingOppstartstype,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltak,
} from "@mr/api-client";
import { formaterDato, utledLopenummerFraTiltaksnummer } from "@/utils/Utils";
import Kopiknapp from "../kopiknapp/Kopiknapp";
import Regelverksinfo from "./Regelverksinfo";
import styles from "./SidemenyInfo.module.scss";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  innsatsgrupper: VeilederflateInnsatsgruppe[];
  tiltak: VeilederflateTiltak;
}

const SidemenyInfo = ({ innsatsgrupper, tiltak }: Props) => {
  const { tiltakstype, stedForGjennomforing } = tiltak;

  const minimumInnsatsgruppe = innsatsgrupper
    .filter((innsatsgruppe) => (tiltakstype.innsatsgrupper ?? []).includes(innsatsgruppe.nokkel))
    .reduce((prev, current) => (prev.order < current.order ? prev : current));

  const tiltaksnummer = "tiltaksnummer" in tiltak ? tiltak.tiltaksnummer : null;
  const arrangor = "arrangor" in tiltak ? tiltak.arrangor : null;

  return (
    <Box padding="5" background="bg-subtle" className={styles.panel} id="sidemeny">
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

      {arrangor && (
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
        <BodyShort size="small">{minimumInnsatsgruppe.tittel}</BodyShort>
      </div>

      <TiltakVarighetInfo tiltak={tiltak} />

      {tiltakstype.regelverkLenker && (
        <div className={styles.rad}>
          <BodyShort size="small" className={styles.tittel}>
            Regelverk og rutiner
          </BodyShort>
          <Regelverksinfo
            regelverkLenker={[
              ...tiltakstype.regelverkLenker,
              {
                _id: "klage",
                regelverkLenkeNavn: "Avslag og klage",
                regelverkUrl:
                  "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Klage-p%C3%A5-arbeidsmarkedstiltak.aspx",
              },
              {
                _id: "vurdering",
                regelverkLenkeNavn: "Tiltak hos familie/nærstående",
                regelverkUrl:
                  "https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-tiltak-og-virkemidler/SitePages/Rutine.aspx",
              },
            ]}
          />
        </div>
      )}
    </Box>
  );
};

function TiltakVarighetInfo({ tiltak }: { tiltak: VeilederflateTiltak }) {
  const { tittel, innhold } =
    !isTiltakGruppe(tiltak) || tiltak.oppstart === TiltaksgjennomforingOppstartstype.LOPENDE
      ? {
          tittel: "Oppstart",
          innhold: "Løpende",
        }
      : tiltak.sluttdato
        ? {
            tittel: "Varighet",
            innhold: `${formaterDato(tiltak.oppstartsdato)} - ${formaterDato(tiltak.sluttdato)}`,
          }
        : {
            tittel: "Oppstart",
            innhold: formaterDato(tiltak.oppstartsdato),
          };

  return (
    <div className={styles.rad}>
      <BodyShort size="small" className={styles.tittel}>
        {tittel}
      </BodyShort>
      <BodyShort size="small">{innhold}</BodyShort>
    </div>
  );
}

export default SidemenyInfo;
