import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError, TilsagnDto, TilsagnRequest, TiltaksgjennomforingDto } from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { Alert, BodyShort, Button, DatePicker, HGrid, HStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { addYear } from "../../utils/Utils";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { FormGroup } from "../skjema/FormGroup";
import { AFTBeregningSkjema } from "./AFTBeregningSkjema";
import { FriBeregningSkjema } from "./FriBeregningSkjema";
import { InferredOpprettTilsagnSchema, OpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { TiltakDetaljerForTilsagn } from "./TiltakDetaljerForTilsagn";

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

  const { handleSubmit, register, setValue } = form;

  useEffect(() => {
    if (tilsagn) {
      setValue("id", tilsagn.id);
      setValue("kostnadssted", tilsagn?.kostnadssted.enhetsnummer);
      setValue("beregning", tilsagn.beregning);
      setValue("periode.start", tilsagn.periodeStart);
      setValue("periode.slutt", tilsagn.periodeSlutt);
    }
  }, [kostnadssteder, tilsagn, setValue]);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <TiltakDetaljerForTilsagn tiltaksgjennomforing={tiltaksgjennomforing} />
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
          {prismodell == "AFT" ? (
            <AFTBeregningSkjema defaultAntallPlasser={tiltaksgjennomforing.antallPlasser} />
          ) : (
            <FriBeregningSkjema />
          )}
        </FormGroup>
        <FormGroup>
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
        </FormGroup>
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
