import { ReactNode } from "react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { NavAnsatt, Rolle } from "@mr/api-client-v2";

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

function harRolle(ansatt: NavAnsatt, rolle: Rolle) {
  return ansatt.roller.some((dto) => dto.rolle === rolle);
}
