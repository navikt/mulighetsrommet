import {
  GjennomforingDto,
  OpprettManuellUtbetalingRequest,
  ProblemDetail,
} from "@mr/api-client-v2";
import { Button, Heading, HStack, Textarea, TextField, VStack } from "@navikt/ds-react";
import { FormProvider, useForm } from "react-hook-form";
import z from "zod";
import { ControlledDateInput } from "../../../components/skjema/ControlledDateInput";
import { FormGroup } from "../../../components/skjema/FormGroup";
import { TwoColumnGrid } from "../../../layouts/TwoColumGrid";
import { addYear } from "../../../utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useManuellUtbetaling } from "@/api/utbetaling/useOpprettManuellUtbetaling";

interface Props {
  gjennomforing: GjennomforingDto;
}

const Schema = z
  .object({
    periode: z.object(
      {
        start: z.string({ required_error: "Du må velge periodestart" }),
        slutt: z.string({ required_error: "Du må velge periodelsutt" }),
      },
      { required_error: "Du må sette start- og sluttperiode" },
    ),
    beskrivelse: z
      .string({ required_error: "Du må skrive inn en beskrivelse" })
      .min(10, { message: "Du må skrive inn en beskrivelse" })
      .max(300),
    kontonummer: z
      .string({ required_error: "Du må skrive inn kontonummer" })
      .length(11, { message: "Kontonummer består av 11 siffer" }),
    kidNummer: z.string().optional(),
    belop: z
      .string({ required_error: "Du må skrive inn et beløp" })
      .min(1, { message: "Du må skrive inn et beløp" }),
  })
  .refine(
    (data) => {
      return !!data.periode.start;
    },
    {
      message: "Du må sette startdato for perioden",
      path: ["periode.start"],
    },
  )
  .refine(
    (data) => {
      return !!data.periode.slutt;
    },
    {
      message: "Du må sette sluttdato for perioden",
      path: ["periode.slutt"],
    },
  );

type InferredOpprettUtbetalingFormSchema = z.infer<typeof Schema>;

export function OpprettUtbetalingForm({ gjennomforing }: Props) {
  const form = useForm<InferredOpprettUtbetalingFormSchema>({
    resolver: zodResolver(Schema),
  });

  const { register, formState, handleSubmit, setError, control } = form;

  const mutation = useManuellUtbetaling(window.crypto.randomUUID());

  function postData(data: InferredOpprettUtbetalingFormSchema) {
    mutation.mutate(
      { ...data, gjennomforingId: gjennomforing.id },
      {
        onSuccess: () => {
          form.reset();
        },
        onError: (error: ProblemDetail) => {
          if (isValidationError(error)) {
            error.errors.forEach((error) => {
              const name = jsonPointerToFieldPath(error.pointer) as keyof Omit<
                OpprettManuellUtbetalingRequest,
                "gjennomforingId"
              >;
              setError(name, { type: "custom", message: error.detail });
            });
          }
        },
      },
    );
  }

  const errors = formState.errors;
  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <FormGroup>
          <Heading size="medium" level="2">
            Utbetalingsinformasjon
          </Heading>
          <HStack gap="20">
            <ControlledDateInput
              size="small"
              label="Periodestart"
              fromDate={new Date(gjennomforing.startDato)}
              toDate={addYear(new Date(), 5)}
              format="iso-string"
              {...register("periode.start")}
              control={control}
            />
            <ControlledDateInput
              size="small"
              label="Periodeslutt"
              fromDate={new Date(gjennomforing.startDato)}
              toDate={addYear(new Date(), 5)}
              format="iso-string"
              {...register("periode.slutt")}
              control={control}
            />
          </HStack>
          <TwoColumnGrid>
            <VStack gap="5">
              <Textarea
                size="small"
                label="Beskrivelse"
                {...register("beskrivelse")}
                error={errors.beskrivelse?.message}
                resize
              />
              <TextField
                size="small"
                label="Kontonummer"
                {...register("kontonummer")}
                minLength={11}
                maxLength={11}
                error={errors.kontonummer?.message}
              />
              <TextField size="small" label="Valgfritt KID-nummer" {...register("kidNummer")} />
              <TextField
                size="small"
                label="Beløp (NOK)"
                type="number"
                {...register("belop")}
                error={errors.belop?.message}
              />
              <p>
                <b>Tilgjengelig på tilsagn:</b> TODO
              </p>
              <HStack align={"start"} justify={"end"}>
                <Button size="small" type="submit">
                  Opprett utbetaling
                </Button>
              </HStack>
            </VStack>
          </TwoColumnGrid>
        </FormGroup>
      </form>
    </FormProvider>
  );
}
