import { Alert, Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { Avtale, EmbeddedTiltakstype } from "mulighetsrommet-api-client";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { AvtaleListe } from "./AvtaleListe";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { RedaksjoneltInnholdModalContainer } from "@/components/modal/RedaksjoneltInnholdModalContainer";
import { RedaksjoneltInnholdModalBody } from "@/components/modal/RedaksjoneltInnholdModalBody";
import { RedaksjoneltInnholdToppKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdToppKnapperad";

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
      <div style={{ margin: "1rem 0 1rem 0" }}>
        <Alert variant="info">Tiltakstype må velges før redaksjonelt innhold kan redigeres.</Alert>
      </div>
    );
  }

  return (
    <>
      <RedaksjoneltInnholdToppKnapperad>
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
      </RedaksjoneltInnholdToppKnapperad>

      <RedaksjoneltInnholdForm key={`redaksjonelt-innhold-${key}`} tiltakstype={tiltakstype} />

      <RedaksjoneltInnholdModalContainer modalOpen={modalOpen} onClose={() => setModalOpen(false)}>
        <Modal.Header closeButton>
          <Heading size="medium">Kopier redaksjonelt innhold fra avtale</Heading>
        </Modal.Header>

        <RedaksjoneltInnholdModalBody>
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
        </RedaksjoneltInnholdModalBody>
      </RedaksjoneltInnholdModalContainer>
    </>
  );
}
