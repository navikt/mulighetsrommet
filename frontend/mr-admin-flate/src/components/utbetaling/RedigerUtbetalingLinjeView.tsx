import { FieldError } from "@mr/api-client-v2";
import {
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingLinje,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Button, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { UtbetalingLinjerStateAction } from "@/pages/gjennomforing/utbetaling/helper";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  linjerDispatch: React.ActionDispatch<[action: UtbetalingLinjerStateAction]>;
}

export function RedigerUtbetalingLinjeView({ linjer, linjerDispatch, utbetaling }: Props) {
  const { gjennomforingId } = useParams();
  const [error, setError] = useState<FieldError[]>([]);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const tilsagnsTypeFraTilskudd = tilsagnType(utbetaling.tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = linjer.length === 1 ? linjer[0].tilsagn : undefined;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  async function oppdaterLinjer() {
    return await queryClient
      .invalidateQueries(
        {
          queryKey: [
            QueryKeys.utbetaling(utbetaling.id),
            QueryKeys.utbetalingsLinjer(utbetaling.id),
          ],
          refetchType: "all",
        },
        { cancelRefetch: false },
      )
      .then(() => linjerDispatch({ type: "RELOAD" }));
  }

  function fjernLinje(id: string) {
    setError([]);
    linjerDispatch({ type: "REMOVE", id });
  }

  return (
    <VStack>
      <HStack align="end">
        <Heading spacing size="medium" level="2">
          Utbetalingslinjer
        </Heading>
        <Spacer />
        <ActionMenu>
          <ActionMenu.Trigger>
            <Button variant="secondary" size="small">
              Handlinger
            </Button>
          </ActionMenu.Trigger>
          <ActionMenu.Content>
            <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
              Opprett {avtaletekster.tilsagn.type(tilsagnsTypeFraTilskudd).toLowerCase()}
            </ActionMenu.Item>
            <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={oppdaterLinjer}>
              Hent godkjente tilsagn
            </ActionMenu.Item>
          </ActionMenu.Content>
        </ActionMenu>
      </HStack>
      <UtbetalingLinjeTable
        utbetaling={utbetaling}
        linjer={linjer}
        renderRow={(linje, index) => {
          return (
            <UtbetalingLinjeRow
              key={linje.id}
              linje={linje}
              knappeColumn={
                <Button
                  size="small"
                  variant="secondary-neutral"
                  onClick={() => fjernLinje(linje.id)}
                >
                  Fjern
                </Button>
              }
              grayBackground
              onChange={(updated) => {
                linjerDispatch({ type: "UPDATE", linje: updated });
              }}
              errors={error.filter(
                (f) => f.pointer.startsWith(`/${index}`) || f.pointer.includes("totalbelop"),
              )}
            />
          );
        }}
      />
    </VStack>
  );
}

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}
