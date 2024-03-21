import { BodyShort, Button, Heading, Modal, VStack } from "@navikt/ds-react";
import { useAtom, useSetAtom, WritableAtom } from "jotai";
import { Avtalestatus, Opphav } from "mulighetsrommet-api-client";
import { useState } from "react";
import {
  defaultTiltaksgjennomforingfilter,
  gjennomforingDetaljerTabAtom,
  TiltaksgjennomforingFilter,
} from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { inneholderUrl } from "../../utils/Utils";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import styles from "./../modal/Modal.module.scss";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
}

export function TiltaksgjennomforingFilterButtons({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: avtale } = useAvtale();
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const [visKanIkkeOppretteTiltakModal, setVisKanIkkeOppretteTiltakModal] = useState(false);
  const setTiltaksgjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);

  const visOpprettTiltaksgjennomforingKnapp = inneholderUrl("/avtaler/");

  const avtaleErOpprettetIAdminFlate = avtale?.opphav === Opphav.MR_ADMIN_FLATE;
  const avtalenErAktiv = avtale?.avtalestatus === Avtalestatus.AKTIV;

  const kanOppretteTiltak =
    avtale?.tiltakstype?.arenaKode && migrerteTiltakstyper?.includes(avtale.tiltakstype.arenaKode);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {filter.search.length > 0 ||
      filter.tiltakstyper.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.navEnheter.length > 0 ||
      filter.statuser.length > 0 ||
      filter.arrangorOrgnr.length > 0 ? (
        <NullstillFilterKnapp
          onClick={() => {
            setFilter({
              ...defaultTiltaksgjennomforingfilter,
              avtale: avtale?.id ?? defaultTiltaksgjennomforingfilter.avtale,
            });
          }}
        />
      ) : (
        <div></div>
      )}
      {/*
        Empty div over for å dytte de andre knappene til høyre uavhengig
        av om nullstill knappen er der
      */}
      {avtale && avtalenErAktiv && (
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "end",
            gap: "1rem",
            alignItems: "center",
          }}
        >
          <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
            {visOpprettTiltaksgjennomforingKnapp && kanOppretteTiltak ? (
              <Lenkeknapp
                size="small"
                to={`skjema`}
                variant="primary"
                dataTestid="opprett-ny-tiltaksgjenomforing_knapp"
                handleClick={() => setTiltaksgjennomforingFane("detaljer")}
              >
                Opprett ny tiltaksgjennomføring
              </Lenkeknapp>
            ) : (
              <>
                <Button
                  variant="primary"
                  size="small"
                  onClick={() => setVisKanIkkeOppretteTiltakModal(true)}
                >
                  Opprett ny tiltaksgjennomføring
                </Button>
              </>
            )}
            {avtaleErOpprettetIAdminFlate && (
              <>
                <Button
                  size="small"
                  onClick={() => setModalOpen(true)}
                  variant="secondary"
                  type="button"
                  title="Legg til en eksisterende gjennomføring til avtalen"
                >
                  Legg til gjennomføring
                </Button>
                <LeggTilGjennomforingModal
                  avtale={avtale}
                  modalOpen={modalOpen}
                  onClose={() => setModalOpen(false)}
                />
              </>
            )}
          </HarSkrivetilgang>
        </div>
      )}
      <OpprettTiltakIArenaModal
        open={visKanIkkeOppretteTiltakModal}
        onClose={() => setVisKanIkkeOppretteTiltakModal(false)}
        tiltakstype={avtale?.tiltakstype.navn!!}
      />
    </div>
  );
}

interface OpprettTiltakIArenaModalProps {
  open: boolean;
  onClose: () => void;
  tiltakstype: string;
}
function OpprettTiltakIArenaModal({ open, onClose, tiltakstype }: OpprettTiltakIArenaModalProps) {
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const { data: tiltakstyper } = useTiltakstyper({});

  const migrerteTiltakstyperNavn =
    tiltakstyper?.data
      .filter((tiltakstype) => migrerteTiltakstyper?.includes(tiltakstype.arenaKode))
      .map((tiltakstype) => tiltakstype.navn) ?? [];

  return (
    <Modal
      open={open}
      onClose={onClose}
      closeOnBackdropClick
      className={styles.modal_container}
      aria-label="modal"
      width="50rem"
    >
      <Modal.Header closeButton>
        <Heading size="medium">Tiltaksgjennomføring kan ikke opprettes her</Heading>
      </Modal.Header>
      <Modal.Body className={styles.modal_content}>
        <VStack gap="2">
          <BodyShort>
            Tiltak knyttet til tiltakstypen <code>{tiltakstype}</code> kan ikke opprettes her enda.
            Du må fortsatt opprette tiltaksgjennomføringer for denne tiltakstypen i Arena.
          </BodyShort>
          {migrerteTiltakstyperNavn.length > 0 ? (
            <>
              <Heading size="small" level="4">
                Du kan opprette tiltak for følgende tiltakstyper her i NAV Tiltaksadministrasjon:
              </Heading>
              <ul>
                {migrerteTiltakstyperNavn.map((tiltakstype) => (
                  <li key={tiltakstype}>{tiltakstype}</li>
                ))}
              </ul>
            </>
          ) : null}
        </VStack>
      </Modal.Body>
    </Modal>
  );
}
