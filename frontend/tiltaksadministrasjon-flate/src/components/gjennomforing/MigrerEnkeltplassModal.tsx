import {
  BodyShort,
  Box,
  CopyButton,
  Fieldset,
  HGrid,
  Heading,
  HStack,
  Modal,
  VStack,
} from "@navikt/ds-react";
import {
  ArenaEnkeltplassDto,
  PrismodellType,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { useMigrerEnkeltplass } from "@/api/gjennomforing/useMigrerEnkeltplass";
import {
  EnkeltplassPrisinformasjonFelter,
  EnkeltplassPrisinformasjonValues,
} from "./EnkeltplassPrisinformasjonFelter";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { FormButtons } from "@/components/skjema/FormButtons";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function MigrerEnkeltplassModal({
  kandidat,
  onClose,
}: {
  kandidat: ArenaEnkeltplassDto;
  onClose: () => void;
}) {
  const form = useForm<EnkeltplassPrisinformasjonValues>({
    defaultValues: {
      type:
        kandidat.tiltakstype.tiltakskode === Tiltakskode.HOYERE_UTDANNING
          ? PrismodellType.TILSKUDD_TIL_OPPLAERING
          : "",
      totalbelop: "",
      prisbetingelser: "",
      tilskuddstyper: [],
      tilskudd: {},
    },
    shouldUnregister: true,
  });
  const migration = useMigrerEnkeltplass();
  const pending = migration.isPending;

  const onSubmit: SubmitHandler<EnkeltplassPrisinformasjonValues> = (values) => {
    if (!values.type || pending) {
      return;
    }
    form.clearErrors();
    migration.mutate(
      {
        id: kandidat.id,
        body: {
          type: values.type,
          totalbelop:
            values.type === PrismodellType.ANSKAFFET_ENKELTPLASS ? Number(values.totalbelop) : null,
          prisbetingelser: values.prisbetingelser || null,
          tilskudd:
            values.type === PrismodellType.TILSKUDD_TIL_OPPLAERING
              ? Object.fromEntries(
                  (values.tilskuddstyper ?? []).map((type) => [
                    type,
                    Number(values.tilskudd?.[type]),
                  ]),
                )
              : {},
        },
      },
      {
        onValidationError: (validation) => applyValidationErrors(form, validation),
      },
    );
  };

  return (
    <Modal
      open
      onClose={() => !pending && onClose()}
      header={{ heading: "Migrer enkeltplass" }}
      width="900px"
    >
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)}>
          <Modal.Body>
            <VStack>
              <Box borderWidth="0 0 1 0" borderColor="neutral-subtle" paddingBlock="space-24">
                <VStack gap="space-24">
                  <Heading level="3" size="small">
                    Gjennomføring fra Arena
                  </Heading>
                  <HGrid columns={{ xs: 1, md: 2 }} gap="space-16">
                    <MetadataVStack label="Tiltakstype" value={kandidat.tiltakstype.navn} />
                    <MetadataVStack label="Innhold" value={kandidat.navn} />
                    <MetadataVStack label="Ansvarlig Nav-enhet" value={kandidat.ansvarligEnhet} />
                    <MetadataVStack label="Tiltaksnummer" value={kandidat.tiltaksnummer} />
                    <MetadataVStack
                      label="Arrangør"
                      value={`${kandidat.arrangor.navn} (${kandidat.arrangor.organisasjonsnummer})`}
                    />
                  </HGrid>
                </VStack>
              </Box>
              <Box borderWidth="0 0 1 0" borderColor="neutral-subtle" paddingBlock="space-16">
                <VStack gap="space-24">
                  <Heading level="3" size="small">
                    Varighet
                  </Heading>
                  <HGrid columns={{ xs: 1, md: 2 }} gap="space-16">
                    <MetadataVStack
                      label="Startdato"
                      value={formaterDato(kandidat.startDato) || "Ikke registrert"}
                    />
                    <MetadataVStack
                      label="Sluttdato"
                      value={formaterDato(kandidat.sluttDato) || "Ikke registrert"}
                    />
                    <MetadataVStack
                      label="Antall dager i uken"
                      value={
                        kandidat.deltaker.dagerPerUke !== null
                          ? `${kandidat.deltaker.dagerPerUke} dager`
                          : "Ikke registrert"
                      }
                    />
                  </HGrid>
                </VStack>
              </Box>
              <Box borderWidth="0 0 1 0" borderColor="neutral-subtle" paddingBlock="space-16">
                <VStack gap="space-24">
                  <Heading level="3" size="small">
                    Deltaker
                  </Heading>
                  <HGrid columns={{ xs: 1, md: 2 }} gap="space-16">
                    {kandidat.deltaker.navn && (
                      <MetadataVStack label="Navn" value={kandidat.deltaker.navn} />
                    )}
                    {kandidat.deltaker.norskIdent && (
                      <MetadataVStack
                        label="Fødselsnummer"
                        value={
                          <HStack gap="space-4" align="center" wrap={false}>
                            {kandidat.deltaker.norskIdent}
                            <CopyButton size="xsmall" copyText={kandidat.deltaker.norskIdent} />
                          </HStack>
                        }
                      />
                    )}
                    {kandidat.deltaker.oppfolgingEnhet?.navn && (
                      <MetadataVStack
                        label="Enhet"
                        value={kandidat.deltaker.oppfolgingEnhet.navn}
                      />
                    )}
                    {kandidat.deltaker.navVeilederNavn && (
                      <MetadataVStack label="Veileder" value={kandidat.deltaker.navVeilederNavn} />
                    )}
                  </HGrid>
                </VStack>
              </Box>
              <Box borderWidth="0 0 1 0" borderColor="neutral-subtle" paddingBlock="space-16">
                <VStack gap="space-16">
                  <Heading level="3" size="small">
                    Pris og betalingsinformasjon
                  </Heading>
                  <Fieldset legend="Velg prisinformasjon" hideLegend disabled={migration.isPending}>
                    <VStack gap="space-16">
                      <EnkeltplassPrisinformasjonFelter
                        tiltakskode={kandidat.tiltakstype.tiltakskode}
                      />
                    </VStack>
                  </Fieldset>
                  <BodyShort>
                    Gjennomføringen endres først når du trykker «Migrer enkeltplass».
                  </BodyShort>
                </VStack>
              </Box>
            </VStack>
          </Modal.Body>
          <Modal.Footer>
            <FormButtons submitLabel="Migrer enkeltplass" isPending={pending} onCancel={onClose} />
          </Modal.Footer>
        </form>
      </FormProvider>
    </Modal>
  );
}
