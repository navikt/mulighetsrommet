import { BodyShort, CopyButton, HStack, VStack } from "@navikt/ds-react";
import {
  UtbetalingBeregningDto,
  UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
  UtbetalingBeregningFri,
  UtbetalingBeregningPrisPerManedsverk,
  UtbetalingBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="2">
      {beregning.deltakerTableData.rows.length > 0 && (
        <DataDrivenTable data={beregning.deltakerTableData} />
      )}
      <Regnestykke beregning={beregning} />
    </VStack>
  );
}

function roundNdecimals(num: number, N: number) {
  return Number(num.toFixed(N));
}

function Regnestykke(props: { beregning: UtbetalingBeregningDto }) {
  switch (props.beregning.type) {
    case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
      return <FastSatsPerTiltaksplassPerManedRegnestykke beregning={props.beregning} />;
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsverkRegnestykke beregning={props.beregning} />;
    case "PRIS_PER_UKESVERK":
      return <PrisPerUkesverkRegnestykke beregning={props.beregning} />;
    case "FRI":
      return <FriRegnestykke beregning={props.beregning} />;
  }
}

function FastSatsPerTiltaksplassPerManedRegnestykke(props: { beregning: UtbetalingBeregningFastSatsPerTiltaksplassPerManed }) {
  const { beregning } = props;

  return (
    <HStack align="center" gap="2">
      <BodyShort className="font-bold">
        Månedsverk {roundNdecimals(beregning.manedsverkTotal, 5)}
      </BodyShort>
      <BodyShort className="font-bold">×</BodyShort>
      <BodyShort className="font-bold">Sats {formaterNOK(beregning.sats)}</BodyShort>
      <BodyShort className="font-bold">=</BodyShort>
      <CopyButton
        variant="action"
        copyText={beregning.belop.toString()}
        size="small"
        text={formaterNOK(beregning.belop)}
      />
    </HStack>
  );
}

function PrisPerManedsverkRegnestykke(props: { beregning: UtbetalingBeregningPrisPerManedsverk }) {
  const { beregning } = props;

  return (
    <HStack align="center" gap="2">
      <BodyShort className="font-bold">
        Månedsverk {roundNdecimals(beregning.manedsverkTotal, 5)}
      </BodyShort>
      <BodyShort className="font-bold">×</BodyShort>
      <BodyShort className="font-bold">Pris {formaterNOK(beregning.sats)}</BodyShort>
      <BodyShort className="font-bold">=</BodyShort>
      <CopyButton
        variant="action"
        copyText={beregning.belop.toString()}
        size="small"
        text={formaterNOK(beregning.belop)}
      />
    </HStack>
  );
}

function PrisPerUkesverkRegnestykke(props: { beregning: UtbetalingBeregningPrisPerUkesverk }) {
  const { beregning } = props;

  return (
    <HStack align="center" gap="2">
      <BodyShort className="font-bold">
        Ukesverk {roundNdecimals(beregning.ukesverkTotal, 5)}
      </BodyShort>
      <BodyShort className="font-bold">×</BodyShort>
      <BodyShort className="font-bold">Pris {formaterNOK(beregning.sats)}</BodyShort>
      <BodyShort className="font-bold">=</BodyShort>
      <CopyButton
        variant="action"
        copyText={beregning.belop.toString()}
        size="small"
        text={formaterNOK(beregning.belop)}
      />
    </HStack>
  );
}

function FriRegnestykke(props: { beregning: UtbetalingBeregningFri }) {
  const { beregning } = props;

  return (
    <HStack align="center" gap="2">
      <BodyShort className="font-bold">Innsendt beløp =</BodyShort>
      <CopyButton
        variant="action"
        copyText={beregning.belop.toString()}
        size="small"
        text={formaterNOK(beregning.belop)}
      />
    </HStack>
  );
}
