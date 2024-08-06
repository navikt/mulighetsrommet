import { zodResolver } from "@hookform/resolvers/zod";
import {
  Alert,
  BodyShort,
  Button,
  DatePicker,
  HGrid,
  HStack,
  Select,
  TextField,
} from "@navikt/ds-react";
import { TilsagnRequest, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { NumericFormat } from "react-number-format";
import { useNavigate } from "react-router-dom";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { addYear } from "../../utils/Utils";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { FormGroup } from "../skjema/FormGroup";
import { SkjemaDetaljerContainer } from "../skjema/SkjemaDetaljerContainer";
import { SkjemaKolonne } from "../skjema/SkjemaKolonne";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { useOpprettTilsagn } from "./useOpprettTilsagn";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing }: Props) {
  const { data: navEnheter } = useNavEnheter();
  const navigate = useNavigate();
  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
  });

  const mutation = useOpprettTilsagn();

  const {
    handleSubmit,
    register,
    watch,
    setValue,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<InferredOpprettTilsagnSchema> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      id: window.crypto.randomUUID(),
      periodeStart: data.periode.start,
      periodeSlutt: data.periode.slutt,
      kostnadssted: data.kostnadssted,
      belop: data.belop,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
    };

    mutation.mutate(request, {
      onSuccess: navigerTilGjennomforing,
    });
  };

  function navigerTilGjennomforing() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`);
  }

  return (
    <SkjemaDetaljerContainer>
      <SkjemaKolonne>
        <FormProvider {...form}>
          <form onSubmit={handleSubmit(postData)}>
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
                    .map((enhet) => {
                      return (
                        <option key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
                          {enhet.navn} - {enhet.enhetsnummer}
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
            <HStack gap="2">
              <Button size="small" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Oppretter tilsagn" : "Opprett tilsagn"}
              </Button>
              <Button
                onClick={navigerTilGjennomforing}
                size="small"
                type="button"
                variant="primary-neutral"
              >
                Avbryt
              </Button>
            </HStack>
          </form>
        </FormProvider>
      </SkjemaKolonne>
    </SkjemaDetaljerContainer>
  );
}
