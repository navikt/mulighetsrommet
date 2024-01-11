import { ReactNode } from "react";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { NavAnsattRolle } from "mulighetsrommet-api-client";

interface Props {
  children: ReactNode;
  ressurs: "Avtale" | "Tiltaksgjennomføring";
}

export function HarSkrivetilgang({ ressurs, children }: Props) {
  const { data } = useHentAnsatt();

  if (
    ressurs === "Avtale" &&
    (data?.roller.includes(NavAnsattRolle.AVTALER_SKRIV) ||
      data?.roller.includes(NavAnsattRolle.BETABRUKER))
  ) {
    return children;
  } else if (
    ressurs === "Tiltaksgjennomføring" &&
    (data?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV) ||
      data?.roller.includes(NavAnsattRolle.BETABRUKER))
  ) {
    return children;
  } else return null;
}
