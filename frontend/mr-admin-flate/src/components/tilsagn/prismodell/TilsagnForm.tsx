import { useOpprettTilsagn } from "@/api/tilsagn/useOpprettTilsagn";
import { InferredTilsagn, TilsagnSchema } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { VelgKostnadssted } from "@/components/tilsagn/prismodell/VelgKostnadssted";
import { VelgPeriode } from "@/components/tilsagn/prismodell/VelgPeriode";
import { zodResolver } from "@hookform/resolvers/zod";
import { GjennomforingDto, TilsagnRequest, TilsagnType, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Alert, Button, Heading, HStack, TextField } from "@navikt/ds-react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useSearchParams } from "react-router";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { ReactElement } from "react";
import { useKostnadssted } from "@/api/enhet/useKostnadssted";

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<InferredTilsagn>;
  regioner: string[];
  beregningInput: ReactElement;
  beregningOutput: ReactElement;
}

export function TilsagnForm(props: Props) {
  const { gjennomforing, onSuccess, onAvbryt, defaultValues, regioner } = props;
  const [searchParams] = useSearchParams();
  const { data: kostnadssteder } = useKostnadssted(regioner);
  const tilsagnstype: TilsagnType =
    (searchParams.get("type") as TilsagnType) || TilsagnType.TILSAGN;

  const mutation = useOpprettTilsagn();

  const forhandsvalgKostnadssted =
    kostnadssteder?.length === 1 ? kostnadssteder[0].enhetsnummer : defaultValues.kostnadssted;
  const form = useForm<InferredTilsagn>({
    resolver: zodResolver(TilsagnSchema),
    defaultValues: {
      ...defaultValues,
      kostnadssted: forhandsvalgKostnadssted,
    },
  });

  const { handleSubmit, setError } = form;

  const postData: SubmitHandler<InferredTilsagn> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      ...data,
      id: data.id ?? window.crypto.randomUUID(),
    };

    mutation.mutate(request, {
      onSuccess: onSuccess,
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((error: { pointer: string; detail: string }) => {
          const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
          setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  };
 
  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <div className="border border-border-default rounded p-4 mt-6 mb-6">
          <div className="flex justify-between my-3 flex-col gap-5">
            <Heading size="medium" level="3">
              Tilsagn
            </Heading>
          </div>
          <div className="grid grid-cols-2">
            <div className="pr-6">
              <div className="grid grid-cols-2">
                <TextField
                  size="small"
                  label="Tilsagnstype"
                  readOnly
                  value={avtaletekster.tilsagn.type(tilsagnstype)}
                />
              </div>
              {tilsagnstype === TilsagnType.INVESTERING && <InfomeldingOmInvesteringsTilsagn />}
              <div className="py-3">
                <VelgPeriode startDato={gjennomforing.startDato} />
              </div>
              <div className="py-3">{props.beregningInput}</div>
              <div className="py-3">
                <VelgKostnadssted kostnadssteder={kostnadssteder} />
              </div>
            </div>
            <div className="border-l border-border-subtle pl-6 flex flex-col gap-2">
              {props.beregningOutput}
            </div>
          </div>
        </div>
        <HStack gap="2" justify={"end"}>
          <Button onClick={onAvbryt} size="small" type="button" variant="tertiary">
            Avbryt
          </Button>
          <Button size="small" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Sender til godkjenning" : "Send til godkjenning"}
          </Button>
        </HStack>
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
