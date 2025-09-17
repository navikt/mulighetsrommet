import { BodyShort, CopyButton, HStack, VStack } from "@navikt/ds-react";
import {
  DataDrivenTableDto,
  UtbetalingBeregningDto,
  UtbetalingBeregningDtoFastSatsPerTiltaksplassPerManed,
  UtbetalingBeregningDtoFri,
  UtbetalingBeregningDtoPrisPerManedsverk,
  UtbetalingBeregningDtoPrisPerUkesverk,
} from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="2">
      {beregning.deltakerTableData.rows.length > 0 && (
        <DataDrivenTable data={beregning.deltakerTableData as unknown as DataDrivenTableDto} />
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
    case "no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.FastSatsPerTiltaksplassPerManed":
      return <FastSatsPerTiltaksplassPerManedRegnestykke beregning={props.beregning} />;
    case "no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.PrisPerManedsverk":
      return <PrisPerManedsverkRegnestykke beregning={props.beregning} />;
    case "no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.PrisPerUkesverk":
      return <PrisPerUkesverkRegnestykke beregning={props.beregning} />;
    case "no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.Fri":
      return <FriRegnestykke beregning={props.beregning} />;
    case undefined:
      throw new Error(`Ukjent beregning: ${props.beregning}`);
  }
}

function FastSatsPerTiltaksplassPerManedRegnestykke(props: {
  beregning: UtbetalingBeregningDtoFastSatsPerTiltaksplassPerManed;
}) {
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

function PrisPerManedsverkRegnestykke(props: {
  beregning: UtbetalingBeregningDtoPrisPerManedsverk;
}) {
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

function PrisPerUkesverkRegnestykke(props: { beregning: UtbetalingBeregningDtoPrisPerUkesverk }) {
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

function FriRegnestykke(props: { beregning: UtbetalingBeregningDtoFri }) {
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
