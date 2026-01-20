import {
  Betalingsinformasjon,
  GjennomforingDto,
  OpprettUtbetalingRequest,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  Button,
  Heading,
  HStack,
  Link,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { addDuration } from "@mr/frontend-common/utils/date";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useOpprettUtbetaling } from "@/api/utbetaling/mutations";

interface Props {
  gjennomforing: GjennomforingDto;
  betalingsinformasjon?: Betalingsinformasjon;
}

const MIN_BEGRUNNELSE_LENGDE = 10;
const MAKS_BEGRUNNELSE_LENGDE = 300;

export function OpprettUtbetalingForm({ gjennomforing, betalingsinformasjon }: Props) {
  const form = useForm<OpprettUtbetalingRequest>({
    resolver: async (values) => ({ values, errors: {} }),
  });
  const navigate = useNavigate();
  const utbetalingId = useRef(window.crypto.randomUUID());

  const { register, formState, handleSubmit, setError, setValue, getValues } = form;

  const mutation = useOpprettUtbetaling(utbetalingId.current);

  function postData(data: OpprettUtbetalingRequest) {
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
        onValidationError: (error: ValidationError) => {
          error.errors.forEach((error) => {
            const name = jsonPointerToFieldPath(error.pointer) as keyof Omit<
              OpprettUtbetalingRequest,
              "gjennomforingId"
            >;
            setError(name, { type: "custom", message: error.detail });
          });
        },
      },
    );
  }

  const errors = formState.errors;
  return (
    <>
      <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
      <div className="w-3/4">
        <FormProvider {...form}>
          <form onSubmit={handleSubmit(postData)}>
            <FormGroup>
              <Heading size="medium" level="2">
                Utbetalingsinformasjon
              </Heading>
              <HStack gap="2">
                <ControlledDateInput
                  label="Periodestart"
                  fromDate={new Date(gjennomforing.startDato)}
                  toDate={addDuration(new Date(), { years: 5 })}
                  onChange={(val) => setValue("periodeStart", val)}
                  defaultSelected={getValues("periodeStart")}
                  error={errors.periodeStart?.message}
                />
                <ControlledDateInput
                  label="Periodeslutt"
                  fromDate={new Date(gjennomforing.startDato)}
                  toDate={addDuration(new Date(), { years: 5 })}
                  onChange={(val) => setValue("periodeSlutt", val)}
                  defaultSelected={getValues("periodeSlutt")}
                  error={errors.periodeSlutt?.message}
                />
              </HStack>
              <VStack align={"start"}>
                <TextField
                  size="small"
                  label="Beløp (NOK)"
                  type="number"
                  {...register("belop", {
                    valueAsNumber: true,
                  })}
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
              {betalingsinformasjon && (
                <BetalingsinformasjonView betalingsinformasjon={betalingsinformasjon} />
              )}
              {!betalingsinformasjon ? (
                <Alert variant="warning" className="my-5">
                  <VStack align="start" gap="2">
                    <Heading spacing size="xsmall" level="3">
                      Kontonummer mangler for arrangør
                    </Heading>
                    <p className="text-balance">
                      Arrangøren har ikke registrert et kontonummer for utbetaling i Altinn.
                      Arrangør må legge inn kontonummer før du kan opprette utbetaling til
                      arrangøren. Les mer om <EndreKontonummerLink /> her.
                    </p>
                  </VStack>
                </Alert>
              ) : null}
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

function EndreKontonummerLink() {
  return (
    <Link
      rel="noopener noreferrer"
      href="https://www.nav.no/arbeidsgiver/endre-kontonummer#hvordan"
      target="_blank"
    >
      endring av kontonummer for refusjoner fra Nav
    </Link>
  );
}

function BetalingsinformasjonView({
  betalingsinformasjon,
}: {
  betalingsinformasjon: Betalingsinformasjon;
}) {
  const form = useForm<OpprettUtbetalingRequest>({
    resolver: async (values) => ({ values, errors: {} }),
  });
  const { register, formState } = form;

  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="2">
          <TextField
            size="small"
            label="Kontonummer til arrangør"
            readOnly
            value={betalingsinformasjon.kontonummer}
            description="Kontonummer hentes automatisk fra Altinn"
          />
          <small className="text-balance">
            Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Les mer her om{" "}
            <EndreKontonummerLink />.
          </small>
          <TextField
            size="small"
            label="Valgfritt KID-nummer"
            {...register("kidNummer")}
            error={formState.errors.kidNummer?.message}
          />
        </VStack>
      );
    case "IBan":
      return (
        <VStack gap="2" align="start">
          <Heading size="small">Bank</Heading>
          <TextField size="small" label="IBan" readOnly value={betalingsinformasjon.iban} />
          <TextField size="small" label="BIC/SWIFT" readOnly value={betalingsinformasjon.bic} />
          <TextField size="small" label="Banknavn" readOnly value={betalingsinformasjon.bankNavn} />
          <TextField
            size="small"
            label="Bank landkode"
            readOnly
            value={betalingsinformasjon.bankLandKode}
          />
          <small className="text-balance">
            Dersom informasjonen må oppdateres ta kontakt med team Valp.
          </small>
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
  }
}
