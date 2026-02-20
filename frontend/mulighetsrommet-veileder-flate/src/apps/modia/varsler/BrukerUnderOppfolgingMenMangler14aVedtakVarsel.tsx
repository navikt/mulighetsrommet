import { Brukerdata, BrukerdataVarsel } from "@api-client";
import { Melding } from "@/components/melding/Melding";

interface Props {
  brukerdata: Brukerdata;
}

export function BrukerUnderOppfolgingMenMangler14aVedtakVarsel({ brukerdata }: Props) {
  return brukerdata.varsler.includes(
    BrukerdataVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
  ) ? (
    <Melding
      variant="warning"
      header="Bruker mangler §14 a-vedtak"
      data-testid="varsel_servicesgruppe"
    >
      Brukeren har ikke fått §14 a-vedtak, og kan derfor ikke meldes på noen tiltak.
    </Melding>
  ) : null;
}
