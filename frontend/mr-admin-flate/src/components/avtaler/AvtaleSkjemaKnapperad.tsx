import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { Button } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
}
export function AvtaleSkjemaKnapperad({ redigeringsModus, onClose }: Props) {
  const setTiltaksgjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);
  return (
    <SkjemaKnapperad>
      <ValideringsfeilOppsummering />
      <Button size="small" onClick={onClose} variant="tertiary" type="button">
        Avbryt
      </Button>
      <HarSkrivetilgang ressurs="Avtale">
        <Button size="small" type="submit" onClick={() => setTiltaksgjennomforingFane("detaljer")}>
          {redigeringsModus ? "Lagre redigert avtale" : "Opprett ny avtale"}
        </Button>
      </HarSkrivetilgang>
    </SkjemaKnapperad>
  );
}
