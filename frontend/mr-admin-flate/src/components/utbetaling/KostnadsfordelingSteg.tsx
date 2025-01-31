import { formaterDato } from "@/utils/Utils";
import {
  GjennomforingDto,
  Prismodell,
  RefusjonKravKompakt,
  TilsagnDefaultsRequest,
  TilsagnDto,
  TilsagnType,
} from "@mr/api-client-v2";
import { Alert, BodyShort, Heading, HStack, Table, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredUtbetalingSchema } from "./UtbetalingSchema";
import { OpprettTilsagnLink } from "../tilsagn/OpprettTilsagnLink";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";

interface Props {
  gjennomforing: GjennomforingDto;
  krav: RefusjonKravKompakt;
  tilsagn: TilsagnDto[];
}

export function KostnadsfordelingSteg({ gjennomforing, krav, tilsagn }: Props) {
  const {
    watch,
    register,
    formState: { errors },
  } = useFormContext<InferredUtbetalingSchema>();

  function utbetalesTotal(): number {
    const total = watch("kostnadsfordeling").reduce((total, item) => total + item.belop, 0);
    if (typeof total === "number") {
      return total;
    }
    return 0;
  }

  function ekstraTilsagnDefaults(): TilsagnDefaultsRequest {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    return {
      gjennomforingId: gjennomforing.id,
      type: TilsagnType.EKSTRATILSAGN,
      prismodell: Prismodell.FRI,
      belop: defaultTilsagn ? krav.beregning.belop - defaultTilsagn.beregning.output.belop : null,
      periodeStart: krav.beregning.periodeStart,
      periodeSlutt: krav.beregning.periodeSlutt,
      kostnadssted: defaultTilsagn?.kostnadssted.enhetsnummer,
    };
  }

  return (
    <VStack gap="4" id="kostnadsfordeling">
      <VStack gap="2">
        <HStack gap="4" align="center">
          <Heading size="small">Periode:</Heading>
          <BodyShort>
            {formaterDato(krav.beregning.periodeStart)} -{" "}
            {formaterDato(krav.beregning.periodeSlutt)}
          </BodyShort>
        </HStack>
        <OpprettTilsagnLink defaults={ekstraTilsagnDefaults()} />
      </VStack>
      {tilsagn.length === 0 && <Alert variant="info">Tilsagn mangler</Alert>}
      {tilsagn.length > 0 && (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Tilsagnsnummer</Table.HeaderCell>
              <Table.HeaderCell>Periodestart</Table.HeaderCell>
              <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
              <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
              <Table.HeaderCell>Gjenstående beløp</Table.HeaderCell>
              <Table.HeaderCell>Utbetales</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tilsagn.map((t: TilsagnDto, i: number) => {
              return (
                <Table.Row
                  key={i}
                  className={t.status.type !== "GODKJENT" ? "bg-surface-warning-subtle" : ""}
                >
                  <Table.DataCell>{t.id}</Table.DataCell>
                  <Table.DataCell>{formaterDato(t.periodeStart)}</Table.DataCell>
                  <Table.DataCell>{formaterDato(t.periodeSlutt)}</Table.DataCell>
                  <Table.DataCell>{t.kostnadssted.navn}</Table.DataCell>
                  <Table.DataCell>{`${formaterNOK(t.beregning.output.belop)} //TODO: Bruk faktisk gjenstående`}</Table.DataCell>
                  <Table.DataCell>
                    <TextField
                      disabled={t.status.type !== "GODKJENT"}
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
                    <TilsagnTag status={t.status} />
                  </Table.DataCell>
                </Table.Row>
              );
            })}
            <Table.Row>
              <Table.DataCell className="font-bold">{`Opprinnelig krav ${formaterNOK(krav.beregning.belop)}`}</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell className="font-bold">{formaterNOK(utbetalesTotal())}</Table.DataCell>
              <Table.DataCell></Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      )}
    </VStack>
  );
}
