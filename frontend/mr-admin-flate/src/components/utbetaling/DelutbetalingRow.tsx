import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingDto,
  DelutbetalingStatus,
  ProblemDetail,
  TilsagnDto,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Button, Checkbox, HStack, Table } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Metadata } from "../detaljside/Metadata";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { DelutbetalingTag } from "./DelutbetalingTag";

interface Props {
  tilsagn: TilsagnDto;
  delutbetaling: DelutbetalingDto;
  opprettelse: TotrinnskontrollDto;
}

export function DelutbetalingRow({ tilsagn, delutbetaling, opprettelse }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const besluttMutation = useBesluttDelutbetaling(delutbetaling.id);

  const godkjentUtbetaling = [DelutbetalingStatus.GODKJENT, DelutbetalingStatus.UTBETALT].includes(
    delutbetaling.status,
  );

  function beslutt(body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(body, {
      onSuccess: () => {
        return queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
      },
      onError: (error: ProblemDetail) => {
        throw error;
      },
    });
  }

  function content() {
    if (delutbetaling.gjorOppTilsagn && !godkjentUtbetaling) {
      return (
        <Alert variant="warning">
          Når denne utbetalingen godkjennes av beslutter vil det ikke lenger være mulig å gjøre
          flere utbetalinger fra tilsagnet
        </Alert>
      );
    } else {
      return (
        <HStack gap="4">
          <Metadata horizontal header="Behandlet av" verdi={opprettelse.behandletAv} />
          {opprettelse.type === "BESLUTTET" && (
            <Metadata horizontal header="Besluttet av" verdi={opprettelse.besluttetAv} />
          )}
        </HStack>
      );
    }
  }

  return (
    <Table.ExpandableRow
      defaultOpen={delutbetaling.gjorOppTilsagn}
      key={tilsagn.id}
      content={content()}
    >
      <Table.DataCell>{formaterPeriodeStart(tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{formaterPeriodeSlutt(tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(tilsagn.belopGjenstaende)}</Table.DataCell>
      <Table.DataCell>
        <Checkbox hideLabel readOnly={true} checked={delutbetaling.gjorOppTilsagn}>
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell>{formaterNOK(delutbetaling.belop)}</Table.DataCell>
      <Table.DataCell>
        <DelutbetalingTag status={delutbetaling.status} />
      </Table.DataCell>
      <Table.DataCell>
        {opprettelse.kanBesluttes && (
          <HStack gap="4">
            <Button
              size="small"
              type="button"
              onClick={() =>
                beslutt({
                  besluttelse: Besluttelse.GODKJENT,
                })
              }
            >
              Godkjenn
            </Button>
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setAvvisModalOpen(true)}
            >
              Send i retur
            </Button>
          </HStack>
        )}
      </Table.DataCell>
      {delutbetaling && (
        <AarsakerOgForklaringModal
          open={avvisModalOpen}
          header="Send i retur med forklaring"
          buttonLabel="Send i retur"
          aarsaker={[
            { value: "FEIL_BELOP", label: "Feil beløp" },
            { value: "FEIL_ANNET", label: "Annet" },
          ]}
          onClose={() => setAvvisModalOpen(false)}
          onConfirm={({ aarsaker, forklaring }) => {
            beslutt({
              besluttelse: Besluttelse.AVVIST,
              aarsaker,
              forklaring: forklaring ?? null,
            });
            setAvvisModalOpen(false);
          }}
        />
      )}
    </Table.ExpandableRow>
  );
}
