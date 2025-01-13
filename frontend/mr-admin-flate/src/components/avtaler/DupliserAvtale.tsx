import { useSetAtom } from "jotai";
import { AvtaleDto, Opphav } from "@mr/api-client";
import { useNavigate } from "react-router";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { DupliserButton } from "../detaljside/DupliserButton";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  avtale: AvtaleDto;
}

export function DupliserAvtale({ avtale }: Props) {
  const navigate = useNavigate();
  const setAvtaleDetaljerTab = useSetAtom(avtaleDetaljerTabAtom);

  function apneRedigeringForDupliseringAvAvtale() {
    setAvtaleDetaljerTab("detaljer");
    navigate(`/avtaler/skjema`, {
      state: {
        dupliserAvtale: {
          opphav: Opphav.MR_ADMIN_FLATE,
          tiltakstype: avtale.tiltakstype,
          avtaletype: avtale.avtaletype,
          beskrivelse: avtale.beskrivelse,
          faneinnhold: avtale.faneinnhold,
        },
      },
    });
  }

  return (
    <HarSkrivetilgang ressurs="Avtale">
      <DupliserButton title="Dupliser avtale" onClick={apneRedigeringForDupliseringAvAvtale} />
    </HarSkrivetilgang>
  );
}
