import { useOpprettUtbetaling } from "@/api/utbetaling/useOpprettUtbetaling";
import { formaterDato } from "@/utils/Utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { RefusjonKravKompakt, TilsagnDto, TilsagnType, UtbetalingRequest } from "@mr/api-client-v2";
import {
  Alert,
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
import { Link, useLoaderData, useNavigate } from "react-router";
import { z } from "zod";
import { behandleUtbetalingFormPageLoader } from "./behandleUtbetalingFormPageLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";

const UtbetalingSchema = z.object({
  kostnadsfordeling: z
    .object({
      tilsagnId: z.string(),
      belop: z.number({ required_error: "Du må velge beløp" }),
    })
    .array(),
});

type InferredUtbetalingSchema = z.infer<typeof UtbetalingSchema>;

function defaultValues(
  krav: RefusjonKravKompakt,
  tilsagn: TilsagnDto[],
): DeepPartial<InferredUtbetalingSchema> {
  const kunEttTilsagn = tilsagn.length === 1;
  return {
    kostnadsfordeling: tilsagn.map((t) => ({
      tilsagnId: t.id,
      belop: kunEttTilsagn ? Math.min(krav.beregning.belop, t.beregning.output.belop) : 0,
    })),
  };
}

export function BehandleUtbetalingFormPage() {
  const { gjennomforing, krav, tilsagn } = useLoaderData<typeof behandleUtbetalingFormPageLoader>();

  const mutation = useOpprettUtbetaling(krav.id);
  const navigate = useNavigate();

  const form = useForm<InferredUtbetalingSchema>({
    resolver: zodResolver(UtbetalingSchema),
    defaultValues: defaultValues(krav, tilsagn),
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
        navigate(-1);
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

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforing.id}/utbetalinger`,
    },
    { tittel: "Behandle utbetaling" },
  ];

  if (tilsagn.length === 0) {
    return <Alert variant="info">Tilsagn mangler</Alert>;
  }

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Utbetalingskrav for {gjennomforing.navn}
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <FormProvider {...form}>
            <form onSubmit={handleSubmit(postData)}>
              <VStack>
                <HStack gap="4" align="center">
                  <Heading size="small">Periode:</Heading>
                  <BodyShort>
                    {formaterDato(krav.beregning.periodeStart)} -{" "}
                    {formaterDato(krav.beregning.periodeSlutt)}
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
                    {tilsagn.map((t: TilsagnDto, i: number) => {
                      return (
                        <Table.Row
                          key={i}
                          className={
                            t.status.type !== "GODKJENT" ? "bg-surface-warning-moderate" : ""
                          }
                        >
                          <Table.DataCell>{t.kostnadssted.navn}</Table.DataCell>
                          <Table.DataCell>{`${t.beregning.output.belop} //TODO: Bruk faktisk gjenstående når vi har den dataen`}</Table.DataCell>
                          <Table.DataCell>
                            {t.status.type !== "GODKJENT" ? (
                              "Tilsagn ikke godkjent"
                            ) : (
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
                            )}
                          </Table.DataCell>
                          <Table.DataCell>
                            // TODO: Bruk belop basert på gjenstående ikke hele tilsagn belopet
                            {tilsagn.length === 1 &&
                              t.beregning.output.belop < krav.beregning.belop && (
                                <Link
                                  to={
                                    `/gjennomforinger/${gjennomforing.id}/tilsagn/opprett-tilsagn` +
                                    `?type=${TilsagnType.EKSTRATILSAGN}` +
                                    `&prismodell=FRI` +
                                    `&belop=${krav.beregning.belop - t.beregning.output.belop}` +
                                    `&periodeStart=${krav.beregning.periodeStart}` +
                                    `&periodeSlutt=${krav.beregning.periodeSlutt}` +
                                    `&kostnadssted=${t.kostnadssted.enhetsnummer}`
                                  }
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
                      <Table.DataCell>{krav.beregning.belop}</Table.DataCell>
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
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
