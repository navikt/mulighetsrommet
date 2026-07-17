import {
  OpprettUtbetalingLinjerRequest,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinjeDto,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  Heading,
  HStack,
  Spacer,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { FormProvider, UseFormReturn, useWatch } from "react-hook-form";
import { GjorOppTilsagnFormCheckbox } from "./GjorOppTilsagnCheckbox";
import { utbetalingTekster } from "./UtbetalingTekster";
import { useOpprettUtbetalingLinjer } from "@/api/utbetaling/mutations";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { extractValidationErrors } from "@/utils/Utils";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
  utbetalingLinjer: UtbetalingLinjeDto[];
  form: UseFormReturn<OpprettUtbetalingLinjerRequest>;
}

export function RedigerUtbetalingLinjeView({
  utbetaling,
  handlinger,
  utbetalingLinjer,
  form,
}: Props) {
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);

  const opprettMutation = useOpprettUtbetalingLinjer(utbetaling.id);

  const {
    handleSubmit,
    register,
    clearErrors,
    getValues,
    control,
    setValue,
    formState: { errors },
  } = form;

  const utbetalingLinjerWatch = useWatch({
    control,
    name: "utbetalingLinjer",
  });

  const utbetalingLinjeBelop = useWatch({
    control,
    name: utbetalingLinjerWatch.map((_, index) => `utbetalingLinjer.${index}.pris.belop` as const),
  });

  const { beregning } = utbetaling;

  const utbetalesTotalt = {
    valuta: beregning.valuta,
    belop: utbetalingLinjeBelop.reduce((acc: number, belop) => acc + (belop ?? 0), 0),
  };

  function sendTilAttestering(payload: OpprettUtbetalingLinjerRequest) {
    clearErrors();

    opprettMutation.mutate(payload, {
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }

  function submitHandler(data: OpprettUtbetalingLinjerRequest) {
    if (utbetalesTotalt.belop < beregning.belop) {
      setMindreBelopModalOpen(true);
    } else {
      sendTilAttestering(data);
    }
  }

  const aktiveLinjer = utbetalingLinjer.filter((linje) =>
    utbetalingLinjerWatch.some((d) => linje.id === d.id),
  );

  return (
    <FormProvider {...form}>
      <form
        onSubmit={(e) => {
          clearErrors();
          handleSubmit(submitHandler)(e);
        }}
      >
        <VStack gap="space-8">
          {!utbetalingLinjer.length && (
            <Alert variant="info">{utbetalingTekster.linje.alert.ingenTilsagn}</Alert>
          )}
          <HStack align="end">
            <Heading spacing size="medium" level="2">
              {utbetalingTekster.linje.header}
            </Heading>
            <Spacer />
          </HStack>

          <UtbetalingLinjeTable
            utbetaling={utbetaling}
            linjer={aktiveLinjer}
            utbetalesTotal={utbetalesTotalt.belop}
            renderRow={(linje: UtbetalingLinjeDto, index: number) => (
              <UtbetalingLinjeRow
                key={`${linje.id}-${linje.status?.type}`}
                gjennomforingId={utbetaling.gjennomforingId}
                linje={linje}
                belopInput={
                  <TextField
                    size="small"
                    style={{ maxWidth: "6rem" }}
                    hideLabel
                    type="text"
                    error={errors.utbetalingLinjer?.[index]?.pris?.belop?.message}
                    label={utbetalingTekster.linje.belop.label}
                    {...register(`utbetalingLinjer.${index}.pris.belop`, {
                      setValueAs: (v: string) => (v === "" ? null : Number(v)),
                      validate: (value: number | null) => {
                        if (!Number.isInteger(value)) return "Beløp må være et heltall";
                        return true;
                      },
                    })}
                  />
                }
                errors={extractValidationErrors(errors)}
                checkboxInput={<GjorOppTilsagnFormCheckbox index={index} />}
                knappeColumn={
                  <FjernUtbetalingLinje
                    onRemove={() => {
                      const current = getValues("utbetalingLinjer");
                      setValue(
                        "utbetalingLinjer",
                        current.filter((_, i) => i !== index),
                      );
                    }}
                  />
                }
              />
            )}
          />
          {utbetalingLinjerWatch.length > 0 && (
            <HStack gap="space-8" justify="end">
              <ValideringsfeilOppsummering />
              {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                <Button size="small" type="submit">
                  {utbetalingTekster.linje.handlinger.sendTilAttestering}
                </Button>
              )}
            </HStack>
          )}
        </VStack>

        <VarselModal
          open={mindreBelopModalOpen}
          handleClose={() => setMindreBelopModalOpen(false)}
          primaryButton={
            <Button
              variant="primary"
              onClick={() => {
                setMindreBelopModalOpen(false);
                const formData = getValues();
                sendTilAttestering(formData);
              }}
            >
              Ja, send til attestering
            </Button>
          }
          headingText="Beløp til utbetaling er mindre enn innsendt beløp"
          headingIconType="warning"
          body={
            <VStack gap="space-16">
              <BodyShort>
                Beløpet du er i ferd med å sende til attestering er mindre enn beløpet på
                utbetalingen. Er du sikker på at du vil fortsette?
              </BodyShort>
              <VStack>
                <BodyShort weight="semibold">
                  Beløp til attestering: {formaterValutaBelop(utbetalesTotalt)}
                </BodyShort>
                <BodyShort weight="semibold">
                  Innsendt beløp: {formaterValutaBelop(beregning)}
                </BodyShort>
              </VStack>
              <BodyLong color="contrast">
                Husk at for tiltakene Oppfølging, Avklaring, ARR og Digitalt jobbsøkerkurs skal
                arrangør alltid få utbetalt for gjennomført aktivitet. Det gjelder også for
                eventuelle andre tiltak hvor avtalt pris er basert på gjennomført aktivitet.
              </BodyLong>
              <Textarea
                label="Begrunnelse"
                onChange={(e) => setValue("begrunnelseMindreBetalt", e.target.value)}
                description="Oppgi begrunnelse for beløpet som utbetales. Begrunnelsen vil kun være synlig for Nav."
              />
            </VStack>
          }
          secondaryButton
        />
      </form>
    </FormProvider>
  );
}

function FjernUtbetalingLinje({ onRemove }: { onRemove: () => void }) {
  return (
    <Button data-color="neutral" size="small" variant="secondary" type="button" onClick={onRemove}>
      {utbetalingTekster.linje.handlinger.fjern}
    </Button>
  );
}
