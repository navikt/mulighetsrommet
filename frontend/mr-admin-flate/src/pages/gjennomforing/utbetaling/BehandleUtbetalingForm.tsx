import { useOpprettUtbetaling } from "@/api/utbetaling/useOpprettUtbetaling";
import { formaterDato } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { BehandleUtbetaling, TilsagnDto, TilsagnType, UtbetalingRequest } from "@mr/api-client-v2";
import {
  BodyLong,
  BodyShort,
  Button,
  Heading,
  HStack,
  Table,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router";
import { z } from "zod";

interface Props {
  behandling: BehandleUtbetaling;
  gjennomforingId: string;
}

const UtbetalingSchema = z.object({
  kostnadsfordeling: z
    .object({
      tilsagnId: z.string(),
      belop: z.number({ required_error: "Du må velge beløp" }),
    })
    .array(),
});

type InferredUtbetalingSchema = z.infer<typeof UtbetalingSchema>;

function defaultValues(behandling: BehandleUtbetaling): DeepPartial<InferredUtbetalingSchema> {
  return {
    kostnadsfordeling: behandling.tilsagn.map((t) => ({
      tilsagnId: t.id,
      belop: 0,
    })),
  };
}

export function BehandleUtbetalingForm({ behandling, gjennomforingId }: Props) {
  const mutation = useOpprettUtbetaling(behandling.krav.id);
  const navigate = useNavigate();

  const form = useForm<InferredUtbetalingSchema>({
    resolver: zodResolver(UtbetalingSchema),
    defaultValues: defaultValues(behandling),
  });

  const {
    handleSubmit,
    register,
    watch,
    formState: { errors },
  } = form;

  const postData: SubmitHandler<InferredUtbetalingSchema> = async (data): Promise<void> => {
    const body: UtbetalingRequest = {
      kostnadsfordeling: data.kostnadsfordeling,
    };

    mutation.mutate(body, {
      onSuccess: () => {
        navigate(".");
      },
    });
  };

  function utbetalesTotal(): number {
    const total = watch("kostnadsfordeling").reduce((total, item) => total + item.belop, 0);
    if (typeof total === "number") {
      return total;
    }
    return 0;
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <VStack>
          <HStack gap="4" align="center">
            <Heading size="small">Periode:</Heading>
            <BodyShort>
              {formaterDato(behandling.krav.beregning.periodeStart)} -{" "}
              {formaterDato(behandling.krav.beregning.periodeSlutt)}
            </BodyShort>
          </HStack>
          <BodyLong>Følgende utbetaling sendes til godkjenning:</BodyLong>
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
                <Table.HeaderCell>Gjenstående beløp</Table.HeaderCell>
                <Table.HeaderCell>Utbetales</Table.HeaderCell>
                <Table.HeaderCell></Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {behandling.tilsagn.map((t: TilsagnDto, i: number) => {
                return (
                  <Table.Row key={i}>
                    <Table.DataCell>{t.kostnadssted.navn}</Table.DataCell>
                    <Table.DataCell>{`${t.beregning.output.belop} //TODO: Bruk faktisk gjenstående når vi har den dataen`}</Table.DataCell>
                    <Table.DataCell>
                      <TextField
                        type="number"
                        size="small"
                        label=""
                        hideLabel
                        error={errors.kostnadsfordeling?.[i]?.belop?.message}
                        {...register(`kostnadsfordeling.${i}.belop`, {
                          valueAsNumber: true,
                        })}
                      />
                    </Table.DataCell>
                    <Table.DataCell>
                      {behandling.tilsagn.length === 1 &&
                        t.beregning.output.belop < behandling.krav.beregning.belop && (
                          <Link
                            // TODO: Gi med belop, periode, fri prismodell i query params
                            to={`/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn?type=${TilsagnType.EKSTRATILSAGN}`}
                          >
                            Opprett ekstratilsagn
                          </Link>
                        )}
                    </Table.DataCell>
                  </Table.Row>
                );
              })}
              <Table.Row>
                <Table.DataCell>Total</Table.DataCell>
                <Table.DataCell>-</Table.DataCell>
                <Table.DataCell>{behandling.krav.beregning.belop}</Table.DataCell>
                <Table.DataCell>{utbetalesTotal()}</Table.DataCell>
                <Table.DataCell></Table.DataCell>
              </Table.Row>
            </Table.Body>
          </Table>
          <HStack align="end">
            <Button size="small" type="submit">
              Send til godkjenning
            </Button>
          </HStack>
        </VStack>
      </form>
    </FormProvider>
  );
}
