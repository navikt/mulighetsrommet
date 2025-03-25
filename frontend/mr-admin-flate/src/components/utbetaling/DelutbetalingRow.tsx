import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import { BesluttDelutbetalingRequest, Besluttelse, DelutbetalingStatus, ProblemDetail, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Button, Checkbox, HStack, Table } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { DelutbetalingTag } from "./DelutbetalingTag";

interface Props {
  linje: UtbetalingLinje;
}

export function DelutbetalingRow({ linje }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const besluttMutation = useBesluttDelutbetaling(linje.id);

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

  return (
    <Table.ExpandableRow content={null}>
      <Table.DataCell>{formaterPeriodeStart(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{formaterPeriodeSlutt(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(linje.tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(linje.tilsagn.belopGjenstaende)}</Table.DataCell>
      <Table.DataCell>
        <Checkbox hideLabel readOnly={true} checked={linje.gjorOppTilsagn}>
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell>{formaterNOK(linje.belop)}</Table.DataCell>
      <Table.DataCell>
        <DelutbetalingTag status={linje.status!} />
      </Table.DataCell>
      <Table.DataCell>
        { linje.status === DelutbetalingStatus.TIL_GODKJENNING && (
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
    </Table.ExpandableRow>
  );
}
