import { Avtale, Opphav } from "mulighetsrommet-api-client";
import { useNavigate } from "react-router-dom";
import { useMigrerteTiltakstyperForAvtaler } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { DupliserButton } from "../detaljside/DupliserButton";
import { avtaleDetaljerTabAtom } from "../../api/atoms";
import { useAtom } from "jotai";

interface Props {
  avtale: Avtale;
}

export function DupliserAvtale({ avtale }: Props) {
  const navigate = useNavigate();
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyperForAvtaler();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setAvtaleDetaljerTab] = useAtom(avtaleDetaljerTabAtom);

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
          opphav: Opphav.MR_ADMIN_FLATE,
        },
      },
    });
  }

  return <DupliserButton title="Dupliser avtale" onClick={apneRedigeringForDupliseringAvAvtale} />;
}
