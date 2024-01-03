import { Alert, BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  SanityKontakinfoTiltaksansvarlige,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { RefObject, useRef } from "react";
import { Link } from "react-router-dom";
import { geografiskEnhetForPreviewAtom } from "../../../core/atoms/atoms";
import { erPreview } from "../../../utils/Utils";
import styles from "./Kontaktinfo.module.scss";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const NavKontaktpersonInfo = ({ data }: NavKontaktpersonInfoProps) => {
  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;
  const [brukersGeografiskeEnhet] = useAtom(geografiskEnhetForPreviewAtom);
  const modalRef = useRef<HTMLDialogElement>(null);

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
              <dl className={styles.definisjonsliste}>
                <dt>Teams: </dt>
                <dd>
                  <a className={styles.teamslenke} onClick={() => modalRef?.current?.showModal()}>
                    Kontakt meg på Teams
                  </a>
                </dd>
                <dt>Epost: </dt>
                <dd>
                  <a href={`mailto:${epost}`}>{epost}</a>
                </dd>
                <dt>Telefon: </dt>
                <dd> {telefonnummer && <span>{telefonnummer}</span>}</dd>
                <dt>Enhet: </dt>
                <dd>
                  <span>{enhet}</span>
                </dd>
              </dl>
            </BodyShort>
            <PersonsensitiveOpplysningerModal modalRef={modalRef} epost={epost} />
          </div>
        );
      })}
    </div>
  );
};

interface Props {
  modalRef: RefObject<HTMLDialogElement>;
  epost: string;
}

function PersonsensitiveOpplysningerModal({ modalRef, epost }: Props) {
  function onClose() {
    modalRef?.current?.close();
  }

  function openTeams() {
    window.open(`${TEAMS_DYPLENKE}${encodeURIComponent(epost)}`, "_newtab");
    modalRef?.current?.close();
  }

  return (
    <Modal ref={modalRef} onClose={onClose} aria-label="modal">
      <Modal.Header closeButton>
        <div className={styles.heading}>
          <Heading size="medium">Personvern er viktig</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>Ikke del personsitive opplysninger når du diskuterer tiltak på Teams.</BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={openTeams}>
            Ok
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}

export default NavKontaktpersonInfo;
