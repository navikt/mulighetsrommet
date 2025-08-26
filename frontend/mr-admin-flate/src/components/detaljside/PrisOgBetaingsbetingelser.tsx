import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { Fritekstfelt } from "./Fritekstfelt";
import { Metadata, MetadataHorisontal } from "./Metadata";

interface Props {
  prisbetingelser?: string | null;
  horizontal?: boolean;
}

export function PrisOgBetaingsbetingelser({ prisbetingelser, horizontal }: Props) {
  const header = avtaletekster.prisOgBetalingLabel;
  const value = prisbetingelser ? prisbetingelser : "-";
  const textField = <Fritekstfelt text={value} />;

  if (horizontal) {
    return <MetadataHorisontal header={header} verdi={textField} />;
  }
  return <Metadata header={header} verdi={textField} />;
}
