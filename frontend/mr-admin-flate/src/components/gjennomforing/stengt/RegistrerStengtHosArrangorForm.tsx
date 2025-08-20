import { useSetStengtHosArrangor } from "@/api/gjennomforing/useSetStengtHosArrangor";
import { QueryKeys } from "@/api/QueryKeys";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { FormGroup } from "@/components/skjema/FormGroup";
import { addYear } from "@/utils/Utils";
import { GjennomforingDto, SetStengtHosArrangorRequest, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, Button, HGrid, TextField, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";

interface RegistrerStengtHosArrangorFormProps {
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorForm({
  gjennomforing,
}: RegistrerStengtHosArrangorFormProps) {
  const setStengtHosArrangor = useSetStengtHosArrangor(gjennomforing.id);
  const queryClient = useQueryClient();

  const form = useForm<SetStengtHosArrangorRequest>({});

  const { register, handleSubmit, formState, setError, getValues, setValue } = form;

  function onSubmit(data: SetStengtHosArrangorRequest) {
    setStengtHosArrangor.mutate(
      {
        beskrivelse: data.beskrivelse,
        periodeStart: data.periodeStart || undefined,
        periodeSlutt: data.periodeSlutt || undefined,
      },
      {
        onSuccess: async () => {
          form.reset();
          await queryClient.invalidateQueries({
            queryKey: QueryKeys.gjennomforing(gjennomforing.id),
            refetchType: "all",
          });
        },
        onValidationError: (error: ValidationError) => {
          error.errors.forEach((error) => {
            const name = jsonPointerToFieldPath(error.pointer) as keyof SetStengtHosArrangorRequest;
            setError(name, { type: "custom", message: error.detail });
          });
        },
      },
    );
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
              defaultSelected={getValues("periodeStart")}
              onChange={(val) => setValue("periodeStart", val)}
              error={formState.errors.periodeStart?.message}
              fromDate={minDate}
              toDate={maxDate}
            />
            <ControlledDateInput
              label="Periode slutt"
              defaultSelected={getValues("periodeSlutt")}
              onChange={(val) => setValue("periodeSlutt", val)}
              error={formState.errors.periodeSlutt?.message}
              fromDate={minDate}
              toDate={maxDate}
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
          <VStack className="my-5">
            <Alert inline variant="error">
              Klarte ikke lagre periode
            </Alert>
          </VStack>
        )}

        <Button size="small" icon={<FloppydiskIcon aria-hidden />} type="submit">
          Lagre periode
        </Button>
      </form>
    </FormProvider>
  );
}
