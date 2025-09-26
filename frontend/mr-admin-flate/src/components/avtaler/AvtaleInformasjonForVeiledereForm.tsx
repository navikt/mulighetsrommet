import { Alert, Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import { AvtaleDto, NavEnhetType } from "@mr/api-client-v2";
import { useFormContext } from "react-hook-form";
import { useState } from "react";
import { AvtaleFormValues } from "@/schemas/avtale";
import { AvtaleListe } from "./AvtaleListe";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import {
  getLokaleUnderenheterAsSelectOptions,
  getAndreUnderenheterAsSelectOptions,
} from "@/api/enhet/helpers";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { InformasjonForVeiledereForm } from "../redaksjoneltInnhold/InformasjonForVeiledereForm";

export function AvtaleInformasjonForVeiledereForm() {
  const [key, setKey] = useState(0);
  const { data: tiltakstyper } = useTiltakstyper();

  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");
  const { data: enheter } = useNavEnheter();

  const { setValue, watch } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("detaljer.tiltakskode");

  const tiltakId = tiltakstyper.find((type) => type.tiltakskode === tiltakskode)?.id;

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: AvtaleDto) {
    setValue("veilederinformasjon.redaksjoneltInnhold.beskrivelse", beskrivelse ?? null);
    setValue("veilederinformasjon.redaksjoneltInnhold.faneinnhold", faneinnhold ?? null);
  }

  const regionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  const kontorEnheterOptions = getLokaleUnderenheterAsSelectOptions(
    watch("veilederinformasjon.navRegioner"),
    enheter,
  );
  const andreEnheterOptions = getAndreUnderenheterAsSelectOptions(
    watch("veilederinformasjon.navRegioner"),
    enheter,
  );

  if (!tiltakId) {
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
      <InformasjonForVeiledereForm
        key={`redaksjonelt-innhold-${key}`}
        tiltakId={tiltakId}
        regionerOptions={regionerOptions}
        kontorerOptions={kontorEnheterOptions}
        andreEnheterOptions={andreEnheterOptions}
        kontaktpersonForm={false}
      />
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
            filter={{ sok: search, tiltakstyper: [tiltakId] }}
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
