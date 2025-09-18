import { useOpprettTilsagn } from "@/api/tilsagn/useOpprettTilsagn";
import { VelgKostnadssted } from "@/components/tilsagn/form/VelgKostnadssted";
import { GjennomforingDto, ValidationError as LegacyValidationError } from "@mr/api-client-v2";
import { TilsagnRequest, TilsagnType, ValidationError } from "@tiltaksadministrasjon/api-client";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  Box,
  Button,
  Heading,
  HGrid,
  HStack,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useSearchParams } from "react-router";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { ReactElement } from "react";
import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { addDuration } from "@mr/frontend-common/utils/date";
import { tilsagnTekster } from "../TilsagnTekster";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { TilsagnBeregningPreview } from "./TilsagnBeregningPreview";

interface Props {
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  regioner: string[];
  beregningInput: ReactElement;
  gjennomforing: GjennomforingDto;
}

export function TilsagnForm(props: Props) {
  const { onSuccess, onAvbryt, defaultValues, regioner, gjennomforing } = props;
  const [searchParams] = useSearchParams();
  const { data: kostnadssteder } = useKostnadssted(regioner);
  const tilsagnstype: TilsagnType =
    (searchParams.get("type") as TilsagnType | null) || TilsagnType.TILSAGN;

  const mutation = useOpprettTilsagn();

  const forhandsvalgKostnadssted =
    kostnadssteder.length === 1 ? kostnadssteder[0].enhetsnummer : defaultValues.kostnadssted;
  const form = useForm<TilsagnRequest>({
    resolver: async (values) => ({ values, errors: {} }),
    defaultValues: {
      ...defaultValues,
      kostnadssted: forhandsvalgKostnadssted,
    } as TilsagnRequest,
  });

  const {
    handleSubmit,
    setError,
    register,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<TilsagnRequest> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      ...data,
      id: data.id ?? window.crypto.randomUUID(),
      kommentar: data.kommentar ?? null,
    };

    mutation.mutate(request, {
      onSuccess: onSuccess,
      onValidationError: (error: ValidationError | LegacyValidationError) => {
        error.errors.forEach((error) => {
          const name = jsonPointerToFieldPath(error.pointer) as keyof TilsagnRequest;
          setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <VStack gap="2">
          <Box borderColor="border-subtle" padding="4" borderWidth="1" borderRadius="large">
            <Heading className="my-3" size="medium" level="3">
              Tilsagn
            </Heading>
            <TwoColumnGrid separator>
              <VStack gap="6">
                <TextField
                  size="small"
                  label="Tilsagnstype"
                  readOnly
                  value={avtaletekster.tilsagn.type(tilsagnstype)}
                  className="max-w-fit"
                />
                {tilsagnstype === TilsagnType.INVESTERING && <InfomeldingOmInvesteringsTilsagn />}
                <HGrid columns={2}>
                  <ControlledDateInput
                    label={tilsagnTekster.periode.start.label}
                    fromDate={new Date(gjennomforing.startDato)}
                    toDate={addDuration(new Date(), { years: 1 })}
                    defaultSelected={form.getValues("periodeStart")}
                    onChange={(val) => form.setValue("periodeStart", val)}
                    error={errors.periodeStart?.message}
                  />
                  <ControlledDateInput
                    label={tilsagnTekster.periode.slutt.label}
                    fromDate={new Date(gjennomforing.startDato)}
                    toDate={addDuration(new Date(), { years: 1 })}
                    defaultSelected={form.getValues("periodeSlutt")}
                    onChange={(val) => form.setValue("periodeSlutt", val)}
                    error={errors.periodeSlutt?.message}
                  />
                </HGrid>
                <VelgKostnadssted kostnadssteder={kostnadssteder} />
                {props.beregningInput}
                <Textarea
                  size="small"
                  error={errors.kommentar?.message}
                  label={tilsagnTekster.kommentar.label}
                  maxLength={500}
                  {...register("kommentar")}
                />
              </VStack>
              <TilsagnBeregningPreview />
            </TwoColumnGrid>
          </Box>
          <VStack gap="2">
            <HStack gap="2" justify={"end"}>
              <ValideringsfeilOppsummering />
              <Button onClick={onAvbryt} size="small" type="button" variant="tertiary">
                Avbryt
              </Button>
              <Button size="small" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Sender til godkjenning" : "Send til godkjenning"}
              </Button>
            </HStack>
            {errors.id?.message && (
              <Alert className="self-end" variant="error" size="small">
                {errors.id.message}
              </Alert>
            )}
          </VStack>
        </VStack>
      </form>
    </FormProvider>
  );
}

function InfomeldingOmInvesteringsTilsagn() {
  return (
    <Alert size="small" variant="info" className="my-3">
      <Heading size="xsmall" spacing>
        Tilsagn for investeringer
      </Heading>
      Tilsagn for investeringer skal brukes ved opprettelse av nye tiltaksplasser, jfr.
      tiltaksforskriften §§{" "}
      <a
        target="_blank"
        rel="noopener noreferrer"
        href="https://lovdata.no/forskrift/2015-12-11-1598/§13-8"
      >
        13-8
      </a>{" "}
      og{" "}
      <a
        target="_blank"
        rel="noopener noreferrer"
        href="https://lovdata.no/forskrift/2015-12-11-1598/§14-9"
      >
        14-9
      </a>
      . Det kan ikke brukes til å utbetale ordinære driftsmidler til tiltaksarrangør.
    </Alert>
  );
}
