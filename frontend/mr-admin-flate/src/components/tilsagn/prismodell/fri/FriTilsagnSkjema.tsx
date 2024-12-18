import { addYear } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { Button, Heading, HGrid, HStack, TextField } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import styles from "./FriTilsagnSkjema.module.scss";
import { VelgKostnadssted } from "@/components/tilsagn/VelgKostnadssted";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { useOpprettTilsagn } from "@/api/tilsagn/useOpprettTilsagn";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import {
  FriTilsagnSchema,
  InferredFriTilsagn,
} from "@/components/tilsagn/prismodell/fri/FriTilsagnSchema";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: Partial<InferredFriTilsagn>;
  defaultKostnadssteder: string[];
}

export function FriTilsagnSkjema({
  gjennomforing,
  onSuccess,
  onAvbryt,
  defaultValues,
  defaultKostnadssteder,
}: Props) {
  const mutation = useOpprettTilsagn();

  const form = useForm<InferredFriTilsagn>({
    resolver: zodResolver(FriTilsagnSchema),
    defaultValues: defaultValues,
  });

  const {
    handleSubmit,
    register,
    setError,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<InferredFriTilsagn> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      type: "FRI",
      id: data.id || window.crypto.randomUUID(),
      gjennomforingId: gjennomforing.id,
      tilsagnType: data.type,
      periodeStart: data.periodeStart,
      periodeSlutt: data.periodeSlutt,
      kostnadssted: data.kostnadssted,
      belop: data.belop,
    };

    mutation.mutate(request, {
      onSuccess: onSuccess,
      onError: (error) => {
        if (isValidationError(error.body)) {
          error.body.errors.forEach((error) => {
            const name = error.name as keyof InferredFriTilsagn;
            setError(name, { type: "custom", message: error.message });
          });
        }
      },
    });
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <TiltakDetaljerForTilsagn tiltaksgjennomforing={gjennomforing} />
        <div className={styles.formContainer}>
          <div className={styles.formHeader}>
            <Heading size="medium">Tilsagn</Heading>
          </div>
          <div className={styles.formContent}>
            <div className={styles.formContentLeft}>
              <div className={styles.formGroup}>
                <HGrid columns={2}>
                  <ControlledDateInput
                    label="Dato fra"
                    fromDate={new Date(gjennomforing.startDato)}
                    toDate={addYear(new Date(), 50)}
                    format="iso-string"
                    {...register("periodeStart")}
                    size="small"
                  />
                  <ControlledDateInput
                    label="Dato til"
                    fromDate={new Date(gjennomforing.startDato)}
                    toDate={addYear(new Date(), 50)}
                    format="iso-string"
                    {...register("periodeSlutt")}
                    size="small"
                  />
                </HGrid>
              </div>
              <div className={styles.formGroup}>
                <TextField
                  size="small"
                  type="number"
                  label="Beløp"
                  style={{ width: "180px" }}
                  error={errors.belop?.message}
                  {...register("belop", { valueAsNumber: true })}
                />
              </div>
              <div className={styles.formGroup}>
                <VelgKostnadssted defaultKostnadssteder={defaultKostnadssteder} />
              </div>
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
