import { ReactNode } from "react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { NavAnsatt, NavAnsattRolle } from "@mr/api-client-v2";

interface Props {
  children: ReactNode;
  ressurs: "Avtale" | "Gjennomføring";
  condition?: boolean;
}

export function HarSkrivetilgang({ ressurs, children, condition }: Props) {
  const { data: ansatt } = useHentAnsatt();

  if (!ansatt || condition === false) {
    return null;
  } else if (ressurs === "Avtale" && harRolle(ansatt, NavAnsattRolle.AVTALER_SKRIV)) {
    return children;
  } else if (
    ressurs === "Gjennomføring" &&
    harRolle(ansatt, NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
  ) {
    return children;
  } else {
    return null;
  }
}

function harRolle(ansatt: NavAnsatt, rolle: NavAnsattRolle) {
  return ansatt.roller.includes(rolle);
}
