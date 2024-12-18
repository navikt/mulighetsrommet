import { addYear } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { Button, Heading, HGrid, HStack, TextField } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { InferredTilsagnSchemaAft, TilsagnSchemaAft } from "./OpprettTilsagnSchema";
import styles from "./TilsagnSkjema.module.scss";
import { TiltakDetaljerForTilsagn } from "./TiltakDetaljerForTilsagn";
import { TilsagnBeregningPreview } from "@/components/tilsagn/TilsagnBeregningPreview";
import { VelgKostnadssted } from "@/components/tilsagn/VelgKostnadssted";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { useOpprettTilsagn } from "@/components/tilsagn/useOpprettTilsagn";

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: Partial<InferredTilsagnSchemaAft>;
  defaultKostnadssteder: string[];
}

export function TilsagnSkjemaAft({
  gjennomforing,
  onSuccess,
  onAvbryt,
  defaultValues,
  defaultKostnadssteder,
}: Props) {
  const mutation = useOpprettTilsagn();

  const form = useForm<InferredTilsagnSchemaAft>({
    resolver: zodResolver(TilsagnSchemaAft),
    defaultValues: defaultValues,
  });

  const {
    handleSubmit,
    register,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<InferredTilsagnSchemaAft> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      type: "AFT",
      id: data.id || window.crypto.randomUUID(),
      gjennomforingId: gjennomforing.id,
      tilsagnType: data.type,
      periodeStart: data.periodeStart,
      periodeSlutt: data.periodeSlutt,
      kostnadssted: data.kostnadssted,
      antallPlasser: data.antallPlasser,
    };

    mutation.mutate(request, {
      onSuccess: onSuccess,
      onError: (error) => {
        if (isValidationError(error.body)) {
          error.body.errors.forEach((error) => {
            const name = error.name as keyof InferredTilsagnSchemaAft;
            setError(name, { type: "custom", message: error.message });
          });
        }
      },
    });
  };

  const values = watch();

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
                <HGrid columns={2}>
                  <TextField
                    size="small"
                    type="number"
                    label="Antall plasser"
                    style={{ width: "180px" }}
                    error={errors.antallPlasser?.message}
                    {...register("antallPlasser", { valueAsNumber: true })}
                  />
                  <TextField
                    size="small"
                    type="number"
                    label="Sats"
                    style={{ width: "180px" }}
                    readOnly={true}
                    error={errors.sats?.message}
                    {...register("sats", { valueAsNumber: true })}
                  />
                </HGrid>
              </div>
              <div className={styles.formGroup}>
                <VelgKostnadssted defaultKostnadssteder={defaultKostnadssteder} />
              </div>
            </div>
            <div className={styles.formContentRight}>
              <TilsagnBeregningPreview
                input={{
                  type: "AFT",
                  periodeStart: values.periodeStart,
                  periodeSlutt: values.periodeSlutt,
                  antallPlasser: values.antallPlasser,
                }}
                onTilsagnBeregnet={(beregning) => {
                  const sats = beregning.type === "AFT" ? beregning.sats : 0;
                  setValue("sats", sats);
                }}
              />
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
