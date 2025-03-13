import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { formaterDato, tilsagnTypeToString } from "@/utils/Utils";
import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingDto,
  NavAnsatt,
  NavAnsattRolle,
  ProblemDetail,
  TilsagnDto,
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
  ansatt: NavAnsatt;
}

export function DelutbetalingRow({ tilsagn, delutbetaling, ansatt }: Props) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const besluttMutation = useBesluttDelutbetaling(delutbetaling.id);

  const kanBeslutte =
    delutbetaling.opprettelse.behandletAv !== ansatt.navIdent &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER) &&
    delutbetaling.type === "DELUTBETALING_TIL_GODKJENNING";

  const godkjentUtbetaling =
    delutbetaling.type === "DELUTBETALING_OVERFORT_TIL_UTBETALING" ||
    delutbetaling.type === "DELUTBETALING_UTBETALT";

  function beslutt(body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(body, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
      },
      onError: (error: ProblemDetail) => {
        throw error;
      },
    });
  }

  function content() {
    if (delutbetaling.frigjorTilsagn && !godkjentUtbetaling)
      return (
        <Alert variant="warning">
          Når denne utbetalingen godkjennes av beslutter vil det ikke lenger være mulig å gjøre
          flere utbetalinger fra tilsagnet
        </Alert>
      );
    else if (godkjentUtbetaling)
      return (
        <HStack gap="4">
          <Metadata
            horizontal
            header="Behandlet av"
            verdi={delutbetaling.opprettelse.behandletAv}
          />
          <Metadata
            horizontal
            header="Besluttet av"
            verdi={delutbetaling.opprettelse.besluttetAv}
          />
        </HStack>
      );
    else return null;
  }

  return (
    <Table.ExpandableRow
      defaultOpen={delutbetaling.frigjorTilsagn}
      key={tilsagn.id}
      content={content()}
    >
      <Table.DataCell>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(tilsagn.beregning.output.belop)}</Table.DataCell>
      <Table.DataCell>
        <Checkbox hideLabel readOnly={true} checked={delutbetaling.frigjorTilsagn}>
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell>{formaterNOK(delutbetaling.belop)}</Table.DataCell>
      <Table.DataCell>
        <DelutbetalingTag type={delutbetaling.type} />
      </Table.DataCell>
      <Table.DataCell>
        {kanBeslutte && (
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
