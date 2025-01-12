import { Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { AvtaleDto, GjennomforingDto } from "@mr/api-client";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { useState } from "react";
import { GjennomforingList } from "./GjennomforingList";
import { RedaksjoneltInnholdToppKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdToppKnapperad";

interface Props {
  avtale: AvtaleDto;
}

export function GjennomforingRedaksjoneltInnholdForm({ avtale }: Props) {
  const [key, setKey] = useState(0);
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");

  const { setValue } = useFormContext<InferredGjennomforingSchema>();

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: GjennomforingDto | AvtaleDto) {
    setValue("beskrivelse", beskrivelse ?? null);
    setValue("faneinnhold", faneinnhold ?? null);
    // Ved å endre `key` så tvinger vi en update av den underliggende Slate-komponenten slik at
    // innhold i komponenten blir resatt til å reflektere den nye tilstanden i skjemaet
    setKey(key + 1);
  }

  return (
    <>
      <RedaksjoneltInnholdToppKnapperad>
        <HStack justify="end">
          <Button
            size="small"
            variant="tertiary"
            type="button"
            title="Gjenopprett til redaksjonelt innhold fra avtale"
            onClick={() => {
              kopierRedaksjoneltInnhold(avtale);
            }}
          >
            Gjenopprett til redaksjonelt innhold fra avtale
          </Button>
          <Button
            size="small"
            variant="tertiary"
            type="button"
            title="Kopier redaksjonelt innhold fra en annen gjennomføring under den samme avtalen"
            onClick={() => setModalOpen(true)}
          >
            Kopier redaksjonelt innhold fra gjennomføring
          </Button>
        </HStack>
      </RedaksjoneltInnholdToppKnapperad>

      <RedaksjoneltInnholdForm
        key={`redaksjonelt-innhold-${key}`}
        tiltakstype={avtale.tiltakstype}
      />
      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        style={{ maxHeight: "70rem" }}
        aria-label="modal"
        width="50rem"
      >
        <Modal.Header closeButton>
          <Heading size="medium">Kopier redaksjonelt innhold fra gjennomføring</Heading>
        </Modal.Header>
        <Modal.Body style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
          <Search
            label="Søk på navn eller tiltaksnummer"
            variant="simple"
            hideLabel={false}
            autoFocus
            onChange={(search) => setSearch(search)}
            value={search}
          />
          <GjennomforingList
            filter={{ search, avtale: avtale.id, pageSize: 1000 }}
            action={(gjennomforing) => (
              <Button
                size="small"
                variant="tertiary"
                type="button"
                onClick={() => {
                  kopierRedaksjoneltInnhold(gjennomforing);
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
