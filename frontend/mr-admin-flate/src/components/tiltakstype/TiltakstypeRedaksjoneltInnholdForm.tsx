import { zodResolver } from "@hookform/resolvers/zod";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { usePatchTiltakstypeRedaksjoneltInnhold } from "@/api/tiltakstyper/usePatchTiltakstypeRedaksjoneltInnhold";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormButtons } from "@/components/skjema/FormButtons";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  TiltakstypeRedaksjoneltInnholdFormValues,
  TiltakstypeRedaksjoneltInnholdSchema,
} from "@/schemas/tiltakstypeRedaksjoneltInnhold";
import { FormProvider, useFieldArray, useForm, useFormContext } from "react-hook-form";
import { RedaksjoneltInnholdForm } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdForm";

interface Props {
  tiltakstype: TiltakstypeDto;
  onSuccess: () => void;
  onCancel?: () => void;
}

export function TiltakstypeRedaksjoneltInnholdForm({ tiltakstype, onSuccess, onCancel }: Props) {
  const mutation = usePatchTiltakstypeRedaksjoneltInnhold(tiltakstype.id);
  const { data: alleTiltakstyper } = useTiltakstyper();

  const methods = useForm<TiltakstypeRedaksjoneltInnholdFormValues>({
    resolver: zodResolver(TiltakstypeRedaksjoneltInnholdSchema) as any,
    defaultValues: {
      beskrivelse: tiltakstype.beskrivelse || null,
      faneinnhold: tiltakstype.faneinnhold,
      regelverklenker: tiltakstype.regelverklenker,
      kanKombineresMed: alleTiltakstyper
        .filter((t) => tiltakstype.kanKombineresMed.includes(t.navn))
        .map((t) => t.id),
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
      regelverklenker: values.regelverklenker.map((lenke) => ({
        regelverkUrl: lenke.regelverkUrl,
        regelverkLenkeNavn: lenke.regelverkLenkeNavn || null,
        beskrivelse: lenke.beskrivelse || null,
      })),
      kanKombineresMed: values.kanKombineresMed,
    });
    onSuccess();
  }

  const andreKombinasjonOptions = alleTiltakstyper
    .filter((t) => t.id !== tiltakstype.id)
    .map((t) => ({ value: t.id, label: t.navn }));

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormButtons
          submitLabel="Lagre redaksjonelt innhold"
          onCancel={onCancel}
          isPending={mutation.isPending}
        />
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

            <Heading size="medium" level="3">
              Regelverk
            </Heading>
            <RegelverklenkerSkjema />
          </VStack>
        </TwoColumnGrid>
        <Separator />
        <FormButtons
          submitLabel="Lagre redaksjonelt innhold"
          onCancel={onCancel}
          isPending={mutation.isPending}
        />
      </form>
    </FormProvider>
  );
}

function RegelverklenkerSkjema() {
  const { control } = useFormContext<TiltakstypeRedaksjoneltInnholdFormValues>();
  const { fields, append, remove } = useFieldArray({ control, name: "regelverklenker" });

  return (
    <VStack gap="space-12">
      <Button
        type="button"
        size="small"
        variant="secondary"
        icon={<PlusIcon />}
        onClick={() => append({ regelverkUrl: "", regelverkLenkeNavn: null, beskrivelse: null })}
      >
        Legg til regelverkslenke
      </Button>
      {fields.map((field, index) => (
        <Box key={field.id} padding="space-8" borderWidth="1" borderRadius="8">
          <VStack gap="space-8">
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.regelverkUrl`}
              label="URL"
            />
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.regelverkLenkeNavn`}
              label="Lenketekst"
            />
            <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
              name={`regelverklenker.${index}.beskrivelse`}
              label="Beskrivelse (intern)"
            />
            <HStack justify="end">
              <Button
                type="button"
                size="small"
                variant="danger"
                icon={<XMarkIcon />}
                onClick={() => remove(index)}
              >
                Fjern
              </Button>
            </HStack>
          </VStack>
        </Box>
      ))}
    </VStack>
  );
}
