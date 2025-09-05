import { ReactNode } from "react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { NavAnsattDto, Rolle } from "@tiltaksadministrasjon/api-client";

interface Props {
  children: ReactNode;
  rolle: Rolle;
  condition?: boolean;
}

export function HarTilgang({ rolle, children, condition }: Props) {
  const { data: ansatt } = useHentAnsatt();

  if (condition === false || !harRolle(ansatt, rolle)) {
    return null;
  } else {
    return children;
  }
}

function harRolle(ansatt: NavAnsattDto, rolle: Rolle) {
  return ansatt.roller.some((dto) => dto.rolle === rolle);
}
