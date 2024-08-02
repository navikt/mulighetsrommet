import { zodResolver } from "@hookform/resolvers/zod";
import { Button, DatePicker, HGrid, HStack, Select, TextField } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { NumericFormat } from "react-number-format";
import { useNavigate } from "react-router-dom";
import { useHentBesluttere } from "../../api/ansatt/useHentBesluttere";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { addYear } from "../../utils/Utils";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { FormGroup } from "../skjema/FormGroup";
import { SkjemaDetaljerContainer } from "../skjema/SkjemaDetaljerContainer";
import { SkjemaKolonne } from "../skjema/SkjemaKolonne";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing }: Props) {
  const { data: navEnheter } = useNavEnheter();
  const navigate = useNavigate();
  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
  });
  const { data: besluttere } = useHentBesluttere();

  const {
    handleSubmit,
    register,
    watch,
    setValue,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<InferredOpprettTilsagnSchema> = async (data): Promise<void> => {
    const request = {
      id: window.crypto.randomUUID(),
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
      periodeStart: data.periode.start,
      periodeSlutt: data.periode.slutt,
      kostnadssted: data.kostnadssted,
      belop: data.belop,
    };

    alert(`Sending til beslutter er ikke implementert enda: ${JSON.stringify(request, null, 2)}`);
  };

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
            <FormGroup>
              <Select
                {...register("beslutter")}
                error={errors.beslutter?.message}
                label="Beslutter"
                size="small"
              >
                <option value={undefined}>Velg beslutter</option>
                {besluttere?.map((b) => {
                  return (
                    <option
                      key={b.navIdent}
                      value={b.navIdent}
                    >{`${b.fornavn} ${b.etternavn} (${b.navIdent})`}</option>
                  );
                })}
              </Select>
            </FormGroup>
            <HStack gap="2">
              <Button size="small" type="submit">
                Send til beslutter
              </Button>
              <Button
                onClick={() => navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)}
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
