import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { RedaksjoneltInnholdLenke, TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useUpdateTiltakstypeVeilederinfo } from "@/api/tiltakstyper/useUpdateTiltakstypeVeilederinfo";
import { useRedaksjoneltInnholdLenker } from "@/api/redaksjonelt-innhold/useRedaksjoneltInnholdLenker";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { FormButtons } from "@/components/skjema/FormButtons";
import { FormCombobox } from "@/components/skjema/FormCombobox";
import { FormListInput } from "@/components/skjema/FormListInput";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  TiltakstypeRedaksjoneltInnholdFormValues,
  TiltakstypeRedaksjoneltInnholdSchema,
} from "@/pages/tiltakstyper/form/validation";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";
import { RedaksjoneltInnholdLenkeModal } from "@/components/tiltakstype/RedaksjoneltInnholdLenkeModal";

interface Props {
  tiltakstype: TiltakstypeDto;
  onSuccess: () => void;
  onCancel?: () => void;
}

export function TiltakstypeInformasjonForVeiledereForm({
  tiltakstype,
  onSuccess,
  onCancel,
}: Props) {
  const mutation = useUpdateTiltakstypeVeilederinfo(tiltakstype.id);
  const tiltakstyper = useTiltakstyper();
  const [modalOpen, setModalOpen] = useState(false);

  const methods = useForm<TiltakstypeRedaksjoneltInnholdFormValues>({
    resolver: zodResolver(TiltakstypeRedaksjoneltInnholdSchema) as any,
    defaultValues: {
      beskrivelse: tiltakstype.veilederinfo.beskrivelse || null,
      faneinnhold: tiltakstype.veilederinfo.faneinnhold,
      faglenker: tiltakstype.veilederinfo.faglenker.map((l) => ({ id: l.id })),
      kanKombineresMed: tiltakstype.veilederinfo.kanKombineresMed.map((k) => k.id),
    },
  });

  const { handleSubmit } = methods;

  async function onSubmit(values: TiltakstypeRedaksjoneltInnholdFormValues) {
    await mutation.mutateAsync({
      beskrivelse: values.beskrivelse ?? null,
      faneinnhold: {
        forHvemInfoboks: values.faneinnhold?.forHvemInfoboks || null,
        forHvem: values.faneinnhold?.forHvem || null,
        detaljerOgInnholdInfoboks: values.faneinnhold?.detaljerOgInnholdInfoboks || null,
        detaljerOgInnhold: values.faneinnhold?.detaljerOgInnhold || null,
        pameldingOgVarighetInfoboks: values.faneinnhold?.pameldingOgVarighetInfoboks || null,
        pameldingOgVarighet: values.faneinnhold?.pameldingOgVarighet || null,
        kontaktinfo: values.faneinnhold?.kontaktinfo || null,
        kontaktinfoInfoboks: values.faneinnhold?.kontaktinfoInfoboks || null,
        lenker: values.faneinnhold?.lenker || null,
        delMedBruker: values.faneinnhold?.delMedBruker || null,
        oppskrift: [],
      },
      faglenker: values.faglenker.map((l) => l.id),
      kanKombineresMed: values.kanKombineresMed,
    });
    onSuccess();
  }

  const andreKombinasjonOptions = tiltakstyper
    .filter((t) => t.id !== tiltakstype.id)
    .map((t) => ({ value: t.id, label: t.navn }));

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormButtons submitLabel="Lagre" onCancel={onCancel} isPending={mutation.isPending} />
        <Separator />
        <TwoColumnGrid separator>
          <RedaksjoneltInnholdForm
            path=""
            description="Kort beskrivelse av formål med tiltakstypen."
          />

          <VStack gap="space-16" paddingBlock="space-4">
            <Heading size="medium" level="3">
              Kan kombineres med
            </Heading>
            <ControlledMultiSelect
              name="kanKombineresMed"
              label="Tiltakstyper som kan kombineres med denne"
              placeholder="Søk etter tiltakstyper..."
              size="small"
              options={andreKombinasjonOptions}
            />

            <HStack justify="space-between" align="center">
              <Heading size="medium" level="3">
                Regelverk og rutiner
              </Heading>
              <Button
                size="small"
                variant="tertiary"
                onClick={() => setModalOpen(true)}
                type="button"
              >
                Administrer lenker
              </Button>
            </HStack>
            <FaglenkerSkjema />
            <RedaksjoneltInnholdLenkeModal open={modalOpen} onClose={() => setModalOpen(false)} />
          </VStack>
        </TwoColumnGrid>
        <Separator />
        <FormButtons submitLabel="Lagre" onCancel={onCancel} isPending={mutation.isPending} />
      </form>
    </FormProvider>
  );
}

function FaglenkerSkjema() {
  const lenker = useRedaksjoneltInnholdLenker();

  return (
    <FormListInput
      name="faglenker"
      addButtonLabel="Legg til faglenke"
      emptyItem={{ id: "" }}
      renderItem={(index) => (
        <FormCombobox
          name={`faglenker.${index}.id`}
          label="Faglenke"
          hideLabel
          options={lenker.map((l) => ({ value: l.id, label: getLabel(l) }))}
          placeholder="Velg lenke..."
        />
      )}
    />
  );
}
function getLabel(lenke: RedaksjoneltInnholdLenke) {
  const label = lenke.navn ?? lenke.url;
  return lenke.beskrivelse ? `${label} (${lenke.beskrivelse})` : label;
}
