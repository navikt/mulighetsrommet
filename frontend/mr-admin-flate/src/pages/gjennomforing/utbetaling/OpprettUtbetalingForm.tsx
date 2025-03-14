import { zodResolver } from "@hookform/resolvers/zod";
import {
  GjennomforingDto,
  OpprettManuellUtbetalingRequest,
  ProblemDetail,
} from "@mr/api-client-v2";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Button, Heading, HStack, Textarea, TextField, VStack } from "@navikt/ds-react";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import z from "zod";
import { useCreateManuellUtbetaling } from "../../../api/utbetaling/useCreateOpprettManuellUtbetaling";
import { Separator } from "../../../components/detaljside/Metadata";
import { GjennomforingDetaljerMini } from "../../../components/gjennomforing/GjennomforingDetaljerMini";
import { ControlledDateInput } from "../../../components/skjema/ControlledDateInput";
import { FormGroup } from "../../../components/skjema/FormGroup";
import { addYear } from "../../../utils/Utils";

interface Props {
  gjennomforing: GjennomforingDto;
}

const MIN_BEGRUNNELSE_LENGDE = 10;
const MAKS_BEGRUNNELSE_LENGDE = 300;

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
      .string({ required_error: "Du må oppgi en begrunnelse for utbetalingen" })
      .min(MIN_BEGRUNNELSE_LENGDE, {
        message: `Begrunnelsen er for kort (minimum ${MIN_BEGRUNNELSE_LENGDE} tegn).`,
      })
      .max(MAKS_BEGRUNNELSE_LENGDE, {
        message: `Begrunnelsen er for lang (maks ${MAKS_BEGRUNNELSE_LENGDE} tegn).`,
      }),
    kontonummer: z
      .string({ required_error: "Du må skrive inn kontonummer" })
      .regex(/^\d{11}$/, { message: "Kontonummer må være 11 siffer" }),
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
  )
  .refine(
    (data) => {
      const KID_REGEX = /^\d{2,25}$/;
      if (!data.kidNummer) {
        return true;
      } else {
        return data.kidNummer && KID_REGEX.test(data.kidNummer);
      }
    },
    { message: "KID-nummer må være mellom 2 og 25 siffer", path: ["kidNummer"] },
  );

type InferredOpprettUtbetalingFormSchema = z.infer<typeof Schema>;

export function OpprettUtbetalingForm({ gjennomforing }: Props) {
  const form = useForm<InferredOpprettUtbetalingFormSchema>({
    resolver: zodResolver(Schema),
  });
  const navigate = useNavigate();
  const utbetalingId = useRef(window.crypto.randomUUID());

  const { register, formState, handleSubmit, setError, control } = form;

  const mutation = useCreateManuellUtbetaling(utbetalingId.current);

  function postData(data: InferredOpprettUtbetalingFormSchema) {
    mutation.mutate(
      {
        ...data,
        kidNummer: data.kidNummer || null,
        gjennomforingId: gjennomforing.id,
      },
      {
        onSuccess: () => {
          form.reset();
          navigate(`/gjennomforinger/${gjennomforing.id}/utbetalinger/${utbetalingId.current}`);
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
    <>
      <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
      <div className="w-1/2">
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
              <VStack align={"start"}>
                <TextField
                  size="small"
                  label="Beløp (NOK)"
                  type="number"
                  {...register("belop")}
                  error={errors.belop?.message}
                />
              </VStack>
              <HStack>
                <Textarea
                  size="small"
                  label="Begrunnelse for utbetaling"
                  {...register("beskrivelse")}
                  error={errors.beskrivelse?.message}
                  resize
                  cols={93}
                  minLength={MIN_BEGRUNNELSE_LENGDE}
                  maxLength={MAKS_BEGRUNNELSE_LENGDE}
                />
              </HStack>
              <Separator />
              <Heading size="small" level="2">
                Betalingsinformasjon
              </Heading>
              <VStack gap="5" align={"start"}>
                <TextField
                  size="small"
                  label="Kontonummer"
                  {...register("kontonummer")}
                  minLength={11}
                  maxLength={11}
                  error={errors.kontonummer?.message}
                />
                <TextField
                  size="small"
                  label="Valgfritt KID-nummer"
                  {...register("kidNummer")}
                  error={errors.kidNummer?.message}
                />
              </VStack>
              <HStack align={"start"} justify={"end"} gap="2">
                <Button
                  size="small"
                  variant="tertiary"
                  onClick={() => navigate(`/gjennomforinger/${gjennomforing.id}/utbetalinger`)}
                >
                  Avbryt
                </Button>
                <Button size="small" type="submit">
                  Opprett
                </Button>
              </HStack>
            </FormGroup>
          </form>
        </FormProvider>
      </div>
    </>
  );
}
