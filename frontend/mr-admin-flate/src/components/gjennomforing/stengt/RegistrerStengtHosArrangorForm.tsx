import { GjennomforingDto, SetStengtHosArrangorRequest } from "@mr/api-client-v2";
import { Alert, Button, HGrid, TextField, VStack } from "@navikt/ds-react";
import { FormProvider, useForm } from "react-hook-form";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { addYear } from "@/utils/Utils";
import { useSetStengtHosArrangor } from "@/api/gjennomforing/useSetStengtHosArrangor";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { useRevalidator } from "react-router";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ApiError } from "@mr/frontend-common/components/error-handling/errors";

interface RegistrerStengtHosArrangorFormProps {
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorForm({
  gjennomforing,
}: RegistrerStengtHosArrangorFormProps) {
  const setStengtHosArrangor = useSetStengtHosArrangor(gjennomforing.id);
  const revalidator = useRevalidator();

  const form = useForm<SetStengtHosArrangorRequest>({});

  const { register, handleSubmit, formState, setError } = form;

  function onSubmit(data: SetStengtHosArrangorRequest) {
    setStengtHosArrangor.mutate(data, {
      onSuccess: async () => {
        form.reset();
        await revalidator.revalidate();
      },
      onError: (error: ApiError) => {
        if (isValidationError(error.body)) {
          error.body.errors.forEach((error) => {
            const name = error.name as keyof SetStengtHosArrangorRequest;
            setError(name, { type: "custom", message: error.message });
          });
        }
      },
    });
  }

  const minDate = addYear(new Date(), -1);
  const maxDate = addYear(new Date(), 5);

  return (
    <FormProvider {...form}>
      <form
        onSubmit={handleSubmit((values) => {
          onSubmit(values);
        })}
      >
        <FormGroup>
          <HGrid columns={2}>
            <ControlledDateInput
              label="Periode start"
              fromDate={minDate}
              toDate={maxDate}
              format="iso-string"
              size="small"
              {...register("periodeStart")}
            />
            <ControlledDateInput
              label="Periode slutt"
              fromDate={minDate}
              toDate={maxDate}
              format="iso-string"
              size="small"
              {...register("periodeSlutt")}
            />
          </HGrid>
          <HGrid columns={1}>
            <TextField
              size="small"
              label="Beskrivelse"
              style={{ width: "180px" }}
              error={formState.errors.beskrivelse?.message as string}
              {...register("beskrivelse")}
            />
          </HGrid>
        </FormGroup>

        {setStengtHosArrangor.error && (
          <VStack>
            <Alert inline variant="error">
              Klarte ikke lagre periode
            </Alert>
          </VStack>
        )}

        <Button icon={<FloppydiskIcon area-hidden />} type="submit">
          Lagre periode
        </Button>
      </form>
    </FormProvider>
  );
}
