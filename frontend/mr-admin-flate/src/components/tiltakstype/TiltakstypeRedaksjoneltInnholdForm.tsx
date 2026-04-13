import { zodResolver } from "@hookform/resolvers/zod";
import { Heading, HStack, VStack } from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { usePatchTiltakstypeRedaksjoneltInnhold } from "@/api/tiltakstyper/usePatchTiltakstypeRedaksjoneltInnhold";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ControlledMultiSelect } from "@/components/skjema/ControlledMultiSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormButtons } from "@/components/skjema/FormButtons";
import { FormListInput } from "@/components/skjema/FormListInput";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  TiltakstypeRedaksjoneltInnholdFormValues,
  TiltakstypeRedaksjoneltInnholdSchema,
} from "@/schemas/tiltakstypeRedaksjoneltInnhold";
import { FormProvider, useForm } from "react-hook-form";
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
        url: lenke.url,
        navn: lenke.navn || null,
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
  return (
    <FormListInput
      name="regelverklenker"
      addButtonLabel="Legg til regelverkslenke"
      emptyItem={{ url: "", navn: null, beskrivelse: null }}
      renderItem={(index) => (
        <HStack gap="space-8">
          <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
            name={`regelverklenker.${index}.url`}
            label="URL"
          />
          <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
            name={`regelverklenker.${index}.navn`}
            label="Lenketekst"
          />
          <FormTextField<TiltakstypeRedaksjoneltInnholdFormValues>
            name={`regelverklenker.${index}.beskrivelse`}
            label="Beskrivelse (intern)"
          />
        </HStack>
      )}
    />
  );
}
