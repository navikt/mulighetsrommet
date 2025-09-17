import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { MetadataFritekstfelt } from "./Metadata";

interface Props {
  prisbetingelser?: string | null;
}

export function PrisOgBetaingsbetingelser({ prisbetingelser }: Props) {
  const header = avtaletekster.prisOgBetalingLabel;

  return <MetadataFritekstfelt header={header} value={prisbetingelser} />;
}
