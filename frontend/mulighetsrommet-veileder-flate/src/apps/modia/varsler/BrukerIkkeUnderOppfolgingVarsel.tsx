import { Brukerdata, BrukerdataVarsel } from "@api-client";
import { Melding } from "@/components/melding/Melding";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukerIkkeUnderOppfolgingVarsel({ brukerdata }: Props) {
  if (brukerdata.varsler.includes(BrukerdataVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)) {
    return (
      <Melding header="Bruker ikke under oppfølging" variant="warning">
        Bruker er ikke under oppfølging, og kan derfor ikke meldes på noen tiltak.
      </Melding>
    );
  }

  return null;
}
