import { Alert, Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client-v2";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { AvtaleFormValues } from "@/schemas/avtale";
import { AvtaleListe } from "./AvtaleListe";

export function AvtaleRedaksjoneltInnholdForm() {
  const [key, setKey] = useState(0);

  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");

  const { setValue, watch } = useFormContext<AvtaleFormValues>();
  const tiltakstype = watch("tiltakstype");

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: AvtaleDto) {
    setValue("beskrivelse", beskrivelse ?? null);
    setValue("faneinnhold", faneinnhold ?? null);
  }

  if (!tiltakstype) {
    return (
      <Alert variant="info">Tiltakstype må velges før redaksjonelt innhold kan redigeres.</Alert>
    );
  }

  return (
    <>
      <HStack>
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
      <RedaksjoneltInnholdForm key={`redaksjonelt-innhold-${key}`} tiltakstype={tiltakstype} />
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} aria-label="modal">
        <Modal.Header closeButton>
          <Heading size="medium">Kopier redaksjonelt innhold fra avtale</Heading>
        </Modal.Header>
        <Modal.Body style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
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
