import { Alert, Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { Avtale, EmbeddedTiltakstype } from "mulighetsrommet-api-client";
import { RedaksjoneltInnholdForm } from "../redaksjonelt-innhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { RedaksjoneltInnholdContainer } from "../redaksjonelt-innhold/RedaksjoneltInnholdContainer";
import styles from "../modal/LeggTilGjennomforingModal.module.scss";
import { AvtaleListe } from "./AvtaleListe";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import skjemastyles from "../skjema/Skjema.module.scss";

interface Props {
  tiltakstype?: EmbeddedTiltakstype;
}

export function AvtaleRedaksjoneltInnholdForm({ tiltakstype }: Props) {
  const [key, setKey] = useState(0);

  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");

  const { setValue } = useFormContext<InferredAvtaleSchema>();

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: Avtale) {
    setValue("beskrivelse", beskrivelse ?? null);
    setValue("faneinnhold", faneinnhold ?? null);
  }

  if (!tiltakstype) {
    return (
      <div className={skjemastyles.container}>
        <Alert variant="info">Tiltakstype må velges før redaksjonelt innhold kan redigeres.</Alert>
      </div>
    );
  }

  return (
    <>
      <RedaksjoneltInnholdContainer>
        <HStack justify="end">
          <Button
            size="small"
            variant="tertiary"
            type="button"
            title="Kopier redaksjonelt innhold fra en annen avtale under samme tiltakstype"
            onClick={() => setModalOpen(true)}
          >
            Kopier redaksjonelt innhold fra avtale
          </Button>
        </HStack>
      </RedaksjoneltInnholdContainer>

      <RedaksjoneltInnholdForm key={`redaksjonelt-innhold-${key}`} tiltakstype={tiltakstype} />

      <Modal
        open={modalOpen}
        onClose={() => {
          setSearch("");
          setModalOpen(false);
        }}
        className={styles.modal_container}
        aria-label="modal"
        width="50rem"
      >
        <Modal.Header closeButton>
          <Heading size="medium">Kopier redaksjonelt innhold fra avtale</Heading>
        </Modal.Header>

        <Modal.Body className={styles.modal_content}>
          <Search
            label="Søk på navn eller avtalenummer"
            variant="simple"
            hideLabel={false}
            autoFocus
            onChange={(search) => setSearch(search)}
            value={search}
          />

          <AvtaleListe
            filter={{ sok: search, tiltakstyper: [tiltakstype.id] }}
            action={(avtale) => (
              <Button
                size="small"
                variant="tertiary"
                type="button"
                onClick={() => {
                  kopierRedaksjoneltInnhold(avtale);

                  // Ved å endre `key` så tvinger vi en update av den underliggende Slate-komponenten slik at
                  // innhold i komponenten blir resatt til å reflektere den nye tilstanden i skjemaet
                  setKey(key + 1);

                  setModalOpen(false);
                }}
              >
                Kopier innhold
              </Button>
            )}
          />
        </Modal.Body>
      </Modal>
    </>
  );
}
