import { TilsagnBeregningDto } from "@mr/api-client-v2";
import { FriBeregningTable } from "./FriBeregningTable";
import PrisPerUkesverkBeregning from "./PrisPerUkesverkBeregning";
import PrisPerManedsverkBeregning from "./PrisPerManedsverkBeregning";

interface Props {
  beregning: TilsagnBeregningDto;
}

export function TilsagnBeregning({ beregning }: Props) {
  switch (beregning.type) {
    case "FRI":
      return <FriBeregningTable medRadnummer linjer={beregning.linjer} />;
    case "PRIS_PER_UKESVERK":
      return <PrisPerUkesverkBeregning beregning={beregning} />;
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsverkBeregning beregning={beregning} />;
  }
}
