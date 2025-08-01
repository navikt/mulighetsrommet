import { VeilederflateKontaktinfo, VeilederflateKontaktinfoTiltaksansvarlig } from "@api-client";
import { Alert, BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import { RefObject, useRef } from "react";

const TEAMS_DYPLENKE = "https://teams.microsoft.com/l/chat/0/0?users=";

interface NavKontaktpersonInfoProps {
  kontaktinfo?: VeilederflateKontaktinfo;
}

const NavKontaktpersonInfo = ({ kontaktinfo }: NavKontaktpersonInfoProps) => {
  const modalRef = useRef<HTMLDialogElement>(null);

  if (!kontaktinfo) return null;

  const { tiltaksansvarlige } = kontaktinfo;

  return (
    <div className="prose">
      {tiltaksansvarlige.length === 0 ? (
        <Alert variant="info">Kontaktinfo til tiltaksansvarlig er ikke lagt inn</Alert>
      ) : (
        <>
          <Heading size="small" className="pb-5">
            {tiltaksansvarlige.length > 1 ? "Tiltaksansvarlige" : "Tiltaksansvarlig"}
          </Heading>

          {tiltaksansvarlige.map((tiltaksansvarlig: VeilederflateKontaktinfoTiltaksansvarlig) => {
            const { navn, epost, telefon, enhet, beskrivelse } = tiltaksansvarlig;
            return (
              <div key={epost} className="prose bg-bg-subtle p-2 mt-2 rounded-md">
                <Heading level="4" size="xsmall">
                  {navn}
                </Heading>
                {beskrivelse && (
                  <BodyShort textColor="subtle" size="small">
                    {beskrivelse}
                  </BodyShort>
                )}
                <BodyShort as="div" size="small">
                  <dl>
                    <dt>Teams:</dt>
                    <dd>
                      <a onClick={() => modalRef?.current?.showModal()}>Kontakt meg på Teams</a>
                    </dd>
                    <dt>Epost:</dt>
                    <dd>
                      <a href={`mailto:${epost}`}>{epost}</a>
                    </dd>
                    {telefon ? (
                      <>
                        <dt>Telefon:</dt>
                        <dd>
                          <span>{telefon}</span>
                        </dd>
                      </>
                    ) : null}
                    {enhet ? (
                      <>
                        <dt>Enhet:</dt>
                        <dd>
                          <span>{`${enhet.navn} - ${enhet.enhetsnummer}`}</span>
                        </dd>
                      </>
                    ) : null}
                  </dl>
                </BodyShort>
                {epost && <PersonsensitiveOpplysningerModal modalRef={modalRef} epost={epost} />}
              </div>
            );
          })}
        </>
      )}
    </div>
  );
};

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
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
        <div>
          <Heading size="medium">Personvern er viktig</Heading>
        </div>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Ikke del personsensitive opplysninger når du diskuterer tiltak på Teams.
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <div>
          <Button variant="secondary" onClick={openTeams}>
            Ok
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}

export default NavKontaktpersonInfo;
