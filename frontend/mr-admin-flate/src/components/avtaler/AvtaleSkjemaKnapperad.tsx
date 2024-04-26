import { Button } from "@navikt/ds-react";
import styles from "../skjema/Skjema.module.scss";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { useSetAtom } from "jotai";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
}
export function AvtaleSkjemaKnapperad({ redigeringsModus, onClose }: Props) {
  const setTiltaksgjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);
  return (
    <div>
      <ValideringsfeilOppsummering />
      <Button
        size="small"
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
      >
        Avbryt
      </Button>
      <HarSkrivetilgang ressurs="Avtale">
        <Button
          className={styles.button}
          size="small"
          type="submit"
          onClick={() => setTiltaksgjennomforingFane("detaljer")}
        >
          {redigeringsModus ? "Lagre redigert avtale" : "Opprett ny avtale"}
        </Button>
      </HarSkrivetilgang>
    </div>
  );
}
