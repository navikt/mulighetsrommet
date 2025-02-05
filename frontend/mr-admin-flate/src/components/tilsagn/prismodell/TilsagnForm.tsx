import { useOpprettTilsagn } from "@/api/tilsagn/useOpprettTilsagn";
import { InferredTilsagn, TilsagnSchema } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { VelgKostnadssted } from "@/components/tilsagn/prismodell/VelgKostnadssted";
import { VelgPeriode } from "@/components/tilsagn/prismodell/VelgPeriode";
import { zodResolver } from "@hookform/resolvers/zod";
import { GjennomforingDto, ProblemDetail, TilsagnRequest, TilsagnType } from "@mr/api-client-v2";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Button, Heading, HStack, TextField } from "@navikt/ds-react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useSearchParams } from "react-router";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<InferredTilsagn>;
  defaultKostnadssteder: string[];
  beregningInput: JSX.Element;
  beregningOutput: JSX.Element;
}

export function TilsagnForm(props: Props) {
  const { gjennomforing, onSuccess, onAvbryt, defaultValues, defaultKostnadssteder } = props;
  const [searchParams] = useSearchParams();
  const tilsagnstype: TilsagnType =
    (searchParams.get("type") as TilsagnType) || TilsagnType.TILSAGN;

  const mutation = useOpprettTilsagn();

  const form = useForm<InferredTilsagn>({
    resolver: zodResolver(TilsagnSchema),
    defaultValues: defaultValues,
  });

  const { handleSubmit, setError } = form;

  const postData: SubmitHandler<InferredTilsagn> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      ...data,
      id: data.id ?? window.crypto.randomUUID(),
    };

    mutation.mutate(request, {
      onSuccess: onSuccess,
      onError: (error: ProblemDetail) => {
        if (isValidationError(error)) {
          error.errors.forEach((error: { pointer: string; detail: string }) => {
            const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
            setError(name, { type: "custom", message: error.detail });
          });
        }
      },
    });
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <div className="border border-border-default rounded p-4 mt-8 mb-2">
          <div className="flex justify-between my-3">
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
              <div className="py-3">
                <VelgPeriode startDato={gjennomforing.startDato} />
              </div>
              <div className="py-3">{props.beregningInput}</div>
              <div className="py-3">
                <VelgKostnadssted defaultKostnadssteder={defaultKostnadssteder} />
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
