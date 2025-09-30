import { Button, Heading, HStack, Modal, Search } from "@navikt/ds-react";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingKontaktperson,
} from "@tiltaksadministrasjon/api-client";
import { useFormContext } from "react-hook-form";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { useState } from "react";
import { GjennomforingList } from "./GjennomforingList";
import { RedaksjoneltInnholdToppKnapperad } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdToppKnapperad";
import { splitNavEnheterByType } from "@/api/enhet/helpers";
import { InformasjonForVeiledereForm } from "@/components/redaksjoneltInnhold/InformasjonForVeiledereForm";
import { slateFaneinnholdToPortableText } from "@/components/portableText/helper";

interface Props {
  avtale: AvtaleDto;
  lagredeKontaktpersoner: GjennomforingKontaktperson[];
}

export function GjennomforingInformasjonForVeiledereForm({
  avtale,
  lagredeKontaktpersoner,
}: Props) {
  const [key, setKey] = useState(0);
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [search, setSearch] = useState("");

  const { setValue, watch } = useFormContext<InferredGjennomforingSchema>();

  function kopierRedaksjoneltInnhold({ beskrivelse, faneinnhold }: GjennomforingDto | AvtaleDto) {
    setValue("beskrivelse", beskrivelse ?? null);
    // Portabletext editoren er litt strengere enn slate
    setValue("faneinnhold", slateFaneinnholdToPortableText(faneinnhold ?? null));
    // Ved å endre `key` så tvinger vi en update av den underliggende Slate-komponenten slik at
    // innhold i komponenten blir resatt til å reflektere den nye tilstanden i skjemaet
    setKey(key + 1);
  }
  const navRegioner = watch("navRegioner");

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheter = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => navRegioner.includes(kontor.overordnetEnhet ?? ""));

  const { navKontorEnheter, navAndreEnheter } = splitNavEnheterByType(navEnheter);
  const kontorEnheterOptions = navKontorEnheter.map((enhet) => ({
    label: enhet.navn,
    value: enhet.enhetsnummer,
  }));
  const andreEnheterOptions = navAndreEnheter.map((enhet) => ({
    label: enhet.navn,
    value: enhet.enhetsnummer,
  }));

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

      <InformasjonForVeiledereForm
        key={`redaksjonelt-innhold-${key}`}
        tiltakId={avtale.tiltakstype.id}
        regionerOptions={regionerOptions}
        kontorerOptions={kontorEnheterOptions}
        andreEnheterOptions={andreEnheterOptions}
        kontaktpersonForm
        lagredeKontaktpersoner={lagredeKontaktpersoner}
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
