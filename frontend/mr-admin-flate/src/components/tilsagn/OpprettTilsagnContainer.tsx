import { zodResolver } from "@hookform/resolvers/zod";
import { Button, DatePicker, HGrid, HStack, Select, TextField, VStack } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
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

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function OpprettTilsagnContainer({ tiltaksgjennomforing }: Props) {
  const { data: navEnheter } = useNavEnheter();
  const navigate = useNavigate();
  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
    defaultValues: {
      arrangorOrganisasjonsnummer: tiltaksgjennomforing.arrangor.organisasjonsnummer,
    },
  });

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
              <TextField
                size="small"
                readOnly
                label="Tiltaksgjennomføring"
                value={tiltaksgjennomforing.navn}
              />
            </FormGroup>
            <FormGroup>
              <HGrid columns={3}>
                <TextField
                  size="small"
                  error={errors.arrangorOrganisasjonsnummer?.message}
                  label="Organisasjonsnummer for arrangør"
                  {...register("arrangorOrganisasjonsnummer")}
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
              <HGrid columns={3}>
                <VStack gap="2">
                  <Select
                    size="small"
                    label="Kostnadssted"
                    {...register("kostnadssted")}
                    error={errors.kostnadssted?.message}
                  >
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
                    onValueChange={(e) => {
                      setValue("belop", Number.parseInt(e.value));
                    }}
                  />
                </VStack>
              </HGrid>
            </FormGroup>
            <FormGroup>
              <Select
                {...register("beslutter")}
                error={errors.beslutter?.message}
                label="Beslutter"
                size="small"
              >
                {/**
                 // TODO Ikke hardkode en tullete beslutter, men hente fra backend når vi har Beslutter-rollen
                 */}
                <option value={undefined}>Velg beslutter</option>
                <option value="B123456">Bertil Bengtson - B123456</option>
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
