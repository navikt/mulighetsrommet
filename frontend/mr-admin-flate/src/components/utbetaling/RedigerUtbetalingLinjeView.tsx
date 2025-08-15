import { utbetalingLinjeCompareFn } from "@/utils/Utils";
import {
  FieldError,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingLinje,
} from "@mr/api-client-v2";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Button, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  tilsagn: TilsagnDto[];
  setLinjer: React.Dispatch<React.SetStateAction<UtbetalingLinje[]>>;
}

function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status === TilsagnStatus.GODKJENT)
    .map((t) => ({
      belop: 0,
      tilsagn: t,
      gjorOppTilsagn: false,
      id: uuidv4(),
    }))
    .toSorted(utbetalingLinjeCompareFn);
}

export function RedigerUtbetalingLinjeView({ linjer, setLinjer, utbetaling, tilsagn }: Props) {
  const { gjennomforingId } = useParams();
  const [error, setError] = useState<FieldError[]>([]);
  const navigate = useNavigate();

  const tilsagnsTypeFraTilskudd = tilsagnType(utbetaling.tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop = tilsagn.length === 0 ? utbetaling.belop : 0;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&belop=${defaultBelop}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  function leggTilLinjer() {
    const nyeLinjer = genrererUtbetalingLinjer(tilsagn).filter(
      (linje) => !linjer.find((l) => l.tilsagn.id === linje.tilsagn.id),
    );
    setLinjer([...linjer, ...nyeLinjer].toSorted(utbetalingLinjeCompareFn));
  }

  function fjernLinje(id: string) {
    setError([]);
    const remaining = linjer.filter((d) => d.id !== id);
    setLinjer([...remaining]);
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
            <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={leggTilLinjer}>
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
                setLinjer((prev: UtbetalingLinje[]) =>
                  prev.map((linje) => (linje.id === updated.id ? updated : linje)),
                );
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
