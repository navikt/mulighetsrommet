import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { addYear } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError, TilsagnDto, TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common";
import {
  Alert,
  BodyShort,
  Button,
  DatePicker,
  Heading,
  HGrid,
  HStack,
  Label,
} from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { FormGroup } from "../skjema/FormGroup";
import { AFTBeregningSkjema } from "./AFTBeregningSkjema";
import { FriBeregningSkjema } from "./FriBeregningSkjema";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { TiltakDetaljerForTilsagn } from "./TiltakDetaljerForTilsagn";
import styles from "./TilsagnSkjema.module.scss";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

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
  const { data: kostnadssteder } = useKostnadssted(
    tiltaksgjennomforing.navRegion?.enhetsnummer
      ? [tiltaksgjennomforing.navRegion.enhetsnummer]
      : [],
  );

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
      : {},
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
            <div className={styles.formMetadata}>
              <div className={styles.formMetadataLabels}>
                <div>Tilsagnstype:</div>
                <div>Tilsagnsnummer:</div>
              </div>
              <div className={styles.formMetadataLabels}>
                <div>{tiltaksgjennomforing.tiltakstype.navn}</div>
                <div>{tiltaksgjennomforing.tiltaksnummer}</div>
              </div>
            </div>
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
                  <AFTBeregningSkjema defaultAntallPlasser={tiltaksgjennomforing.antallPlasser} />
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
                <Label size="medium">Total beløp</Label>
                <Label size="medium">{formaterNOK(beregning?.belop)}</Label>
              </div>
            </div>
          </div>
        </div>
        <BodyShort spacing>
          {mutation.error ? (
            <Alert variant="error" size="small">
              Klarte ikke opprette tilsagn
            </Alert>
          ) : null}
        </BodyShort>
        <HStack gap="2" justify={"space-between"}>
          <Button size="small" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Sender til beslutning" : "Send til beslutning"}
          </Button>

          <Button onClick={onAvbryt} size="small" type="button" variant="primary-neutral">
            Avbryt
          </Button>
        </HStack>
      </form>
    </FormProvider>
  );
}
