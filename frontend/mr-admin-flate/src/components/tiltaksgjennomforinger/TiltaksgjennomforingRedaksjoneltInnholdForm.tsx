import { Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { Avtale, Tiltaksgjennomforing } from "@mr/api-client";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { InferredTiltaksgjennomforingSchema } from "@/components/redaksjoneltInnhold/TiltaksgjennomforingSchema";
import { useState } from "react";
import { TiltaksgjennomforingerListe } from "./TiltaksgjennomforingerListe";
import { RedaksjoneltInnholdModalContainer } from "@/components/modal/RedaksjoneltInnholdModalContainer";
import { RedaksjoneltInnholdModalBody } from "@/components/modal/RedaksjoneltInnholdModalBody";
import { RedaksjoneltInnholdToppKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdToppKnapperad";

interface Props {
  avtale: Avtale;
}

export function TiltakgjennomforingRedaksjoneltInnholdForm({ avtale }: Props) {
  const [key, setKey] = useState(0);
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");

  const { setValue } = useFormContext<InferredTiltaksgjennomforingSchema>();

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: Tiltaksgjennomforing | Avtale) {
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

      <RedaksjoneltInnholdModalContainer modalOpen={modalOpen} onClose={() => setModalOpen(false)}>
        <Modal.Header closeButton>
          <Heading size="medium">Kopier redaksjonelt innhold fra gjennomføring</Heading>
        </Modal.Header>
        <RedaksjoneltInnholdModalBody>
          <Search
            label="Søk på navn eller tiltaksnummer"
            variant="simple"
            hideLabel={false}
            autoFocus
            onChange={(search) => setSearch(search)}
            value={search}
          />
          <TiltaksgjennomforingerListe
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
        </RedaksjoneltInnholdModalBody>
      </RedaksjoneltInnholdModalContainer>
    </>
  );
}
