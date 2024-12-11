import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { addMonths, addYear, subtractDays } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError, TilsagnDto, TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Button, DatePicker, Heading, HGrid, HStack, Label } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { AFTBeregningSkjema } from "./AFTBeregningSkjema";
import { FriBeregningSkjema } from "./FriBeregningSkjema";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import styles from "./TilsagnSkjema.module.scss";
import { TiltakDetaljerForTilsagn } from "./TiltakDetaljerForTilsagn";
import { useLocation } from "react-router-dom";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
  tilsagn?: TilsagnDto;
  onSubmit: (data: InferredOpprettTilsagnSchema) => void;
  onAvbryt?: () => void;
  mutation: UseMutationResult<TilsagnDto, ApiError, TilsagnRequest, unknown>;
  prismodell: "AFT" | "FRI";
}

export function TilsagnSkjema({
  tiltaksgjennomforing,
  tilsagn,
  onSubmit,
  onAvbryt,
  mutation,
  prismodell,
}: Props) {
  const locationState = useLocation()?.state as { ekstratilsagn?: boolean };
  const enheterForKostnadssted = locationState?.ekstratilsagn
    ? []
    : tiltaksgjennomforing.navRegion?.enhetsnummer
      ? [tiltaksgjennomforing.navRegion?.enhetsnummer]
      : [];
  const { data: kostnadssteder } = useKostnadssted(enheterForKostnadssted);

  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
    defaultValues: tilsagn
      ? {
          id: tilsagn.id,
          beregning: tilsagn.beregning,
          kostnadssted: tilsagn.kostnadssted.enhetsnummer,
          periode: {
            start: tilsagn.periodeStart,
            slutt: tilsagn.periodeSlutt,
          },
        }
      : {
          id: null,
          beregning: {
            antallPlasser: undefined,
            belop: undefined,
            periodeStart: undefined,
            periodeSlutt: undefined,
            sats: undefined,
            type: undefined,
          },
          kostnadssted: undefined,
          periode: !locationState?.ekstratilsagn
            ? {
                start: tiltaksgjennomforing.startDato,
                slutt: subtractDays(
                  addMonths(new Date(tiltaksgjennomforing.startDato), 6),
                  1,
                ).toISOString(),
              }
            : {},
        },
  });

  const { handleSubmit, register, setValue, watch } = form;
  useEffect(() => {
    if (tilsagn) {
      setValue("id", tilsagn.id);
      setValue("kostnadssted", tilsagn?.kostnadssted.enhetsnummer);
      setValue("beregning", tilsagn.beregning);
      setValue("periode.start", tilsagn.periodeStart);
      setValue("periode.slutt", tilsagn.periodeSlutt);
    }
  }, [kostnadssteder, tilsagn, setValue]);

  const beregning = watch("beregning");

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <TiltakDetaljerForTilsagn tiltaksgjennomforing={tiltaksgjennomforing} />
        <div className={styles.formContainer}>
          <div className={styles.formHeader}>
            <Heading size="medium">Tilsagn</Heading>
          </div>
          <div className={styles.formContent}>
            <div className={styles.formContentLeft}>
              <div className={styles.formGroup}>
                <DatePicker>
                  <HGrid columns={2} gap={"2"}>
                    <ControlledDateInput
                      label="Dato fra"
                      fromDate={new Date(tiltaksgjennomforing.startDato)}
                      toDate={addYear(new Date(), 50)}
                      format="iso-string"
                      {...register("periode.start")}
                      size="small"
                    />
                    <ControlledDateInput
                      label="Dato til"
                      fromDate={new Date(tiltaksgjennomforing.startDato)}
                      toDate={addYear(new Date(), 50)}
                      format="iso-string"
                      {...register("periode.slutt")}
                      size="small"
                    />
                  </HGrid>
                </DatePicker>
              </div>
              <div className={styles.formGroup}>
                {prismodell == "AFT" ? (
                  <AFTBeregningSkjema
                    defaultAntallPlasser={
                      locationState?.ekstratilsagn ? undefined : tiltaksgjennomforing.antallPlasser
                    }
                  />
                ) : (
                  <FriBeregningSkjema />
                )}
              </div>
              <div className={styles.formGroup}>
                <ControlledSokeSelect
                  placeholder="Velg kostnadssted"
                  size="small"
                  label="Kostnadssted"
                  {...register("kostnadssted")}
                  options={
                    kostnadssteder
                      ?.sort((a, b) => a.navn.localeCompare(b.navn))
                      .map(({ navn, enhetsnummer }) => {
                        return {
                          value: enhetsnummer,
                          label: `${navn} - ${enhetsnummer}`,
                        };
                      }) ?? []
                  }
                />
              </div>
            </div>
            <div className={styles.formContentRight}>
              <Heading size="small">Beløp</Heading>
              <div className={styles.rowSpaceBetween}>
                <Label size="medium">Totalbeløp</Label>
                {beregning?.belop && <Label size="medium">{formaterNOK(beregning?.belop)}</Label>}
              </div>
            </div>
          </div>
        </div>
        <div className={styles.alert}>
          {mutation.error ? (
            <Alert variant="error" size="small">
              Klarte ikke opprette tilsagn
            </Alert>
          ) : null}
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
