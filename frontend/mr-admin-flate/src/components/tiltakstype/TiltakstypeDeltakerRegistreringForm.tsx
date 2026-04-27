import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { FormProvider, useForm } from "react-hook-form";
import { useUpdateTiltakstypeDeltakerinfo } from "@/api/tiltakstyper/useUpdateTiltakstypeDeltakerinfo";
import { useDeltakerRegistreringInnholdselementer } from "@/api/tiltakstyper/useDeltakerRegistreringInnholdselementer";
import { FormButtons } from "@/components/skjema/FormButtons";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormCheckboxGroup } from "@/components/skjema/FormCheckboxGroup";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";

interface FormValues {
  ledetekst: string | null;
  innholdskoder: string[];
}

interface Props {
  tiltakstype: TiltakstypeDto;
  onSuccess: () => void;
  onCancel?: () => void;
}

export function TiltakstypeDeltakerRegistreringForm({ tiltakstype, onSuccess, onCancel }: Props) {
  const mutation = useUpdateTiltakstypeDeltakerinfo(tiltakstype.id);
  const { data: innholdselementer } = useDeltakerRegistreringInnholdselementer();

  const methods = useForm<FormValues>({
    defaultValues: {
      ledetekst: tiltakstype.deltakerinfo?.ledetekst ?? null,
      innholdskoder: tiltakstype.deltakerinfo?.innholdselementer.map((e) => e.innholdskode) ?? [],
    },
  });

  const { handleSubmit } = methods;

  async function onSubmit(values: FormValues) {
    await mutation.mutateAsync({
      ledetekst: values.ledetekst || null,
      innholdskoder: values.innholdskoder,
    });
    onSuccess();
  }

  const options = innholdselementer.map((e) => ({ value: e.innholdskode, label: e.tekst }));

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormButtons submitLabel="Lagre" onCancel={onCancel} isPending={mutation.isPending} />
        <Separator />
        <TwoColumnGrid separator>
          <FormTextarea
            name="ledetekst"
            label="Ledetekst"
            description="Tekst som som beskriver tiltaket og som vises til deltaker som blir meldt på."
          />
          <FormCheckboxGroup
            name="innholdskoder"
            legend="Innholdselementer"
            description="Velg hvilke elementer som skal være tilgjengelig ved påmelding av deltakere."
            options={options}
          />
        </TwoColumnGrid>
        <Separator />
        <FormButtons submitLabel="Lagre" onCancel={onCancel} isPending={mutation.isPending} />
      </form>
    </FormProvider>
  );
}
