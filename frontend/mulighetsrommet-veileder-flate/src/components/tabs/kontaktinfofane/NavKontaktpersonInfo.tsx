import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import {
  SanityKontakinfoTiltaksansvarlige,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { logEvent } from "../../../core/api/logger";
import styles from "./Kontaktinfo.module.scss";
import { useAtom } from "jotai";
import { geografiskEnhetForPreviewAtom } from "../../../core/atoms/atoms";
import { erPreview } from "../../../utils/Utils";
import { Link } from "react-router-dom";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const NavKontaktpersonInfo = ({ data }: NavKontaktpersonInfoProps) => {
  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;
  const [brukersGeografiskeEnhet] = useAtom(geografiskEnhetForPreviewAtom);

  if (erPreview() && !brukersGeografiskeEnhet) {
    return (
      <Alert variant="info" inline>
        Det er ikke satt en geografisk enhet i forhåndsvisning så vi vet ikke hvilken kontaktperson
        vi skal vise. Gå til oversikten{" "}
        <Link to="/preview">og velg en geografisk enhet før du går tilbake til tiltaket.</Link>
      </Alert>
    );
  }

  if (tiltaksansvarlige?.length === 0 || !tiltaksansvarlige)
    return (
      <Alert variant="info" inline>
        Kontaktinfo til tiltaksansvarlig er ikke lagt inn
      </Alert>
    );

  return (
    <div className={styles.tiltaksansvarlig_info}>
      <Heading size="small" className={styles.header}>
        Tiltaksansvarlig
      </Heading>

      {tiltaksansvarlige.map((tiltaksansvarlig: SanityKontakinfoTiltaksansvarlige) => {
        const { navn, epost, telefonnummer, enhet } = tiltaksansvarlig;
        return (
          <div key={epost} className={styles.container}>
            <BodyShort className={styles.navn} size="small">
              {navn}
            </BodyShort>

            <BodyShort as="div" size="small">
              <div className={styles.infofelt}>
                <div className={styles.kolonne}>
                  {telefonnummer && <span>Telefon:</span>}
                  <span>Epost:</span>
                  <span>Teams:</span>
                  <span>Enhet:</span>
                </div>

                <div className={styles.kolonne}>
                  {telefonnummer && <span>{telefonnummer}</span>}
                  <a
                    href={`mailto:${epost}`}
                    onClick={() => logEvent("mulighetsrommet.tiltaksansvarlig.epost")}
                  >
                    {epost}
                  </a>
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={`${TEAMS_DYPLENKE}${encodeURIComponent(epost)}`}
                    onClick={() => logEvent("mulighetsrommet.tiltaksansvarlig.teamslenke")}
                  >
                    Kontakt meg på Teams
                  </a>
                  <span>{enhet}</span>
                </div>
              </div>
            </BodyShort>
          </div>
        );
      })}
    </div>
  );
};

export default NavKontaktpersonInfo;
