import { zodResolver } from "@hookform/resolvers/zod";
import { FormProvider, useForm } from "react-hook-form";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { useEffect } from "react";
import {
  HGrid,
  TextField,
  DatePicker,
  Select,
  BodyShort,
  Alert,
  HStack,
  Button,
} from "@navikt/ds-react";
import { NumericFormat } from "react-number-format";
import { addYear } from "../../utils/Utils";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { FormGroup } from "../skjema/FormGroup";
import { ApiError, TilsagnDto, TilsagnRequest, Tiltaksgjennomforing } from "@mr/api-client";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  tilsagn?: TilsagnDto;
  onSubmit: (data: InferredOpprettTilsagnSchema) => void;
  onAvbryt?: () => void;
  mutation: UseMutationResult<TilsagnDto, ApiError, TilsagnRequest, unknown>;
}

export function TilsagnSkjema({
  tiltaksgjennomforing,
  tilsagn,
  onSubmit,
  onAvbryt,
  mutation,
}: Props) {
  const { data: navEnheter } = useNavEnheter();

  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
    defaultValues: tilsagn
      ? {
          id: tilsagn.id,
          belop: tilsagn.belop,
          kostnadssted: tilsagn.kostnadssted.enhetsnummer,
          periode: {
            start: tilsagn.periodeStart,
            slutt: tilsagn.periodeSlutt,
          },
        }
      : {},
  });

  useEffect(() => {
    if (tilsagn) {
      setValue("id", tilsagn.id);
      setValue("kostnadssted", tilsagn?.kostnadssted.enhetsnummer);
      setValue("belop", tilsagn.belop);
      setValue("periode.start", tilsagn.periodeStart);
      setValue("periode.slutt", tilsagn.periodeSlutt);
    }
  }, [navEnheter, tilsagn]);

  const {
    handleSubmit,
    register,
    watch,
    setValue,
    formState: { errors },
  } = form;

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormGroup>
          <HGrid columns={2} gap="2">
            <TextField
              size="small"
              readOnly
              label="Tiltaksgjennomføring"
              value={tiltaksgjennomforing.navn}
            />
            <TextField
              readOnly
              size="small"
              label="Organisasjonsnummer for arrangør"
              value={`${tiltaksgjennomforing.arrangor.navn} - ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
            />
          </HGrid>
        </FormGroup>

        <FormGroup>
          <DatePicker>
            <HGrid columns={2} gap={"2"}>
              <ControlledDateInput
                label="Startdato"
                fromDate={new Date(tiltaksgjennomforing.startDato)}
                toDate={addYear(new Date(), 50)}
                format="iso-string"
                {...register("periode.start")}
                size="small"
              />
              <ControlledDateInput
                label="Sluttdato"
                fromDate={new Date(tiltaksgjennomforing.startDato)}
                toDate={addYear(new Date(), 50)}
                format="iso-string"
                {...register("periode.slutt")}
                size="small"
              />
            </HGrid>
          </DatePicker>
        </FormGroup>
        <FormGroup>
          <HGrid columns={2} gap="2">
            <Select
              size="small"
              label="Kostnadssted"
              {...register("kostnadssted")}
              error={errors.kostnadssted?.message}
            >
              <option value={undefined}>Velg kostnadssted</option>
              {navEnheter
                ?.sort((a, b) => a.navn.localeCompare(b.navn))
                .map(({ navn, enhetsnummer }) => {
                  return (
                    <option key={enhetsnummer} value={enhetsnummer}>
                      {navn} - {enhetsnummer}
                    </option>
                  );
                })}
            </Select>

            <NumericFormat
              size="small"
              error={errors.belop?.message}
              label="Beløp i kroner"
              customInput={TextField}
              value={watch("belop")}
              valueIsNumericString
              thousandSeparator
              suffix=" kr"
              onValueChange={(e) => {
                setValue("belop", Number.parseInt(e.value));
              }}
            />
          </HGrid>
        </FormGroup>
        <BodyShort spacing>
          {mutation.error ? (
            <Alert variant="error" size="small">
              Klarte ikke opprette tilsagn
            </Alert>
          ) : null}
        </BodyShort>
        <HStack gap="2" justify={"space-between"}>
          <Button size="small" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Sender til beslutning" : "Send til beslutning"}
          </Button>

          <Button onClick={onAvbryt} size="small" type="button" variant="primary-neutral">
            Avbryt
          </Button>
        </HStack>
      </form>
    </FormProvider>
  );
}
