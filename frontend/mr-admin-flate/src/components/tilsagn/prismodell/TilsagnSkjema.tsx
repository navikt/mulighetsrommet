import { zodResolver } from "@hookform/resolvers/zod";
import { TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { Button, Heading, HStack } from "@navikt/ds-react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import styles from "./TilsagnSkjema.module.scss";
import { VelgKostnadssted } from "@/components/tilsagn/prismodell/VelgKostnadssted";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { useOpprettTilsagn } from "@/api/tilsagn/useOpprettTilsagn";
import { VelgPeriode } from "@/components/tilsagn/prismodell/VelgPeriode";
import { InferredTilsagn, TilsagnSchema } from "@/components/tilsagn/prismodell/TilsagnSchema";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<InferredTilsagn>;
  defaultKostnadssteder: string[];
  beregningInput: JSX.Element;
  beregningOutput: JSX.Element;
}

export function TilsagnSkjema(props: Props) {
  const { gjennomforing, onSuccess, onAvbryt, defaultValues, defaultKostnadssteder } = props;

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
      onError: (error) => {
        if (isValidationError(error.body)) {
          error.body.errors.forEach((error) => {
            const name = error.name as keyof InferredTilsagn;
            setError(name, { type: "custom", message: error.message });
          });
        }
      },
    });
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <div className={styles.formContainer}>
          <div className={styles.formHeader}>
            <Heading size="medium" level="3">
              Tilsagn
            </Heading>
          </div>
          <div className={styles.formContent}>
            <div className={styles.formContentLeft}>
              <div className={styles.formGroup}>
                <VelgPeriode startDato={gjennomforing.startDato} />
              </div>
              <div className={styles.formGroup}>{props.beregningInput}</div>
              <div className={styles.formGroup}>
                <VelgKostnadssted defaultKostnadssteder={defaultKostnadssteder} />
              </div>
            </div>
            <div className={styles.formContentRight}>{props.beregningOutput}</div>
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
