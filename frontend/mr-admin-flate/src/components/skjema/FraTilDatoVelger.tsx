import { ControlledDateInput, DateInputProps } from "./ControlledDateInput";
import { ReactNode } from "react";
import { HGrid } from "@navikt/ds-react";

export interface FraTilDatoVelgerProps {
  fra: DateInputProps;
  til: DateInputProps;
  size?: "small" | "medium";
  children?: ReactNode;
}

export function FraTilDatoVelger({ fra, til, size }: FraTilDatoVelgerProps) {
  return (
    <HGrid columns={2}>
      <ControlledDateInput size={size} {...fra} />
      <ControlledDateInput size={size} {...til} />
    </HGrid>
  );
}
