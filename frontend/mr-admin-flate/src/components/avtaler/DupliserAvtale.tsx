import { useSetAtom } from "jotai";
import { Avtale, Opphav } from "@mr/api-client";
import { useNavigate } from "react-router-dom";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useMigrerteTiltakstyperForAvtaler } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { DupliserButton } from "../detaljside/DupliserButton";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  avtale: Avtale;
}

export function DupliserAvtale({ avtale }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyperForAvtaler();
  const setAvtaleDetaljerTab = useSetAtom(avtaleDetaljerTabAtom);

  if (!migrerteTiltakstyper.includes(avtale.tiltakstype.tiltakskode)) return null;

  function apneRedigeringForDupliseringAvAvtale() {
    setAvtaleDetaljerTab("detaljer");
    navigate(`/avtaler/skjema`, {
      state: {
        avtale: {
          opphav: Opphav.MR_ADMIN_FLATE,
          faneinnhold: avtale.faneinnhold,
          tiltakstype: avtale.tiltakstype,
          avtaletype: avtale.avtaletype,
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
