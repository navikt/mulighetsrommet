import { ReactNode } from "react";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { NavAnsatt, Rolle } from "@mr/api-client-v2";

interface Props {
  children: ReactNode;
  ressurs: "Avtale" | "Gjennomføring";
  condition?: boolean;
}

export function HarSkrivetilgang({ ressurs, children, condition }: Props) {
  const { data: ansatt } = useHentAnsatt();

  if (!ansatt || condition === false) {
    return null;
  } else if (ressurs === "Avtale" && harRolle(ansatt, Rolle.AVTALER_SKRIV)) {
    return children;
  } else if (ressurs === "Gjennomføring" && harRolle(ansatt, Rolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
    return children;
  } else {
    return null;
  }
}

function harRolle(ansatt: NavAnsatt, rolle: Rolle) {
  return ansatt.roller.includes(rolle);
}
