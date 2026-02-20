import { Brukerdata, BrukerdataVarsel } from "@api-client";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { brukersEnhetFilterHasChanged } from "@/apps/modia/delMedBruker/helpers";
import { Melding } from "@/components/melding/Melding";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukersOppfolgingsenhetVarsel({ brukerdata }: Props) {
  const filter = useArbeidsmarkedstiltakFilterValue();

  return !brukersEnhetFilterHasChanged(filter, brukerdata) &&
    brukerdata.varsler.includes(BrukerdataVarsel.LOKAL_OPPFOLGINGSENHET) ? (
    <Melding header="Brukers oppfølgingsenhet" variant="info">
      Bruker har en annen oppfølgingsenhet enn geografisk enhet. Det er aktuelle tiltak knyttet til
      brukers oppfølgingsenhet som vises i listen.
    </Melding>
  ) : null;
}
