import { Alert, BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import {
  VeilederflateKontaktinfoTiltaksansvarlig,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { RefObject, useRef } from "react";
import styles from "./Kontaktinfo.module.scss";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const NavKontaktpersonInfo = ({ data }: NavKontaktpersonInfoProps) => {
  const { kontaktinfoTiltaksansvarlige: tiltaksansvarlige } = data;
  const modalRef = useRef<HTMLDialogElement>(null);

  return (
    <div className={styles.tiltaksansvarlig_info}>
      {tiltaksansvarlige?.length === 0 || !tiltaksansvarlige ? (
        <Alert variant="info">Kontaktinfo til tiltaksansvarlig er ikke lagt inn</Alert>
      ) : (
        <>
          <Heading size="small" className={styles.header}>
            Tiltaksansvarlig
          </Heading>

          {tiltaksansvarlige.map((tiltaksansvarlig: VeilederflateKontaktinfoTiltaksansvarlig) => {
            const { navn, epost, telefonnummer, enhet, beskrivelse } = tiltaksansvarlig;
            return (
              <div key={epost} className={styles.container}>
                <BodyShort className={styles.navn} size="small">
                  {navn}
                </BodyShort>
                {beskrivelse && (
                  <BodyShort textColor="subtle" size="small">
                    {beskrivelse}
                  </BodyShort>
                )}
                <BodyShort as="div" size="small">
                  <dl className={styles.definisjonsliste}>
                    <dt>Teams:</dt>
                    <dd>
                      <a
                        className={styles.teamslenke}
                        onClick={() => modalRef?.current?.showModal()}
                      >
                        Kontakt meg på Teams
                      </a>
                    </dd>
                    <dt>Epost:</dt>
                    <dd>
                      <a href={`mailto:${epost}`}>{epost}</a>
                    </dd>
                    {telefonnummer ? (
                      <>
                        <dt>Telefon:</dt>
                        <dd>
                          <span>{telefonnummer}</span>
                        </dd>
                      </>
                    ) : null}
                    <dt>Enhet:</dt>
                    <dd>
                      <span>{`${enhet.navn} - ${enhet.enhetsnummer}`}</span>
                    </dd>
                  </dl>
                </BodyShort>
                <PersonsensitiveOpplysningerModal modalRef={modalRef} epost={epost} />
              </div>
            );
          })}
        </>
      )}
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
        <BodyShort>
          Ikke del personsensitive opplysninger når du diskuterer tiltak på Teams.
        </BodyShort>
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
