import { useSetAtom } from "jotai";
import { Avtale, Opphav } from "mulighetsrommet-api-client";
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

  if (!migrerteTiltakstyper.includes(avtale.tiltakstype.arenaKode)) return null;

  function apneRedigeringForDupliseringAvAvtale() {
    setAvtaleDetaljerTab("detaljer");
    navigate(`/avtaler/${avtale.id}/skjema`, {
      state: {
        avtale: {
          ...avtale,
          id: window.crypto.randomUUID(),
          startDato: undefined,
          sluttDato: undefined,
          avtalenummer: undefined,
          lopenummer: undefined,
          websaknummer: undefined,
          opphav: Opphav.MR_ADMIN_FLATE,
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
