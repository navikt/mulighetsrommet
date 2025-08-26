import { Textarea } from "@navikt/ds-react/Textarea";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { Fritekstfelt } from "./Fritekstfelt";
import { Metadata, MetadataHorisontal } from "./Metadata";

interface Props {
  prisbetingelser?: string | null;
  horizontal?: boolean;
  lockedInput?: boolean;
}

export function PrisOgBetaingsbetingelser({ prisbetingelser, horizontal, lockedInput }: Props) {
  const header = avtaletekster.prisOgBetalingLabel;
  const value = prisbetingelser ? prisbetingelser : "-";

  if (lockedInput) {
    return <Textarea size="small" label={header} value={value} readOnly />;
  }

  const textField = <Fritekstfelt text={value} />;

  if (horizontal) {
    return <MetadataHorisontal header={header} verdi={textField} />;
  }
  return <Metadata header={header} verdi={textField} />;
}
