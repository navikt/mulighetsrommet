import styles from "@/components/modal/Modal.module.scss";
import { resolveErrorMessage } from "@mr/frontend-common/components/error-handling/errors";
import { AvbrytAvtaleAarsak, AvbrytGjennomforingAarsak } from "@mr/api-client";
import { AnnetEnum } from "@/api/annetEnum";

interface Props {
  mutation: any;
  aarsak?: AvbrytAvtaleAarsak | AvbrytGjennomforingAarsak | AnnetEnum;
  customAarsak?: string;
}
export function AvbrytModalError({ mutation, aarsak, customAarsak }: Props) {
  const beskrivelser = () => {
    if (aarsak?.length === 0) return "Du må velge en årsak";
    else if (aarsak === "annet" && !customAarsak)
      return "Beskrivelse er obligatorisk når “Annet” er valgt som årsak";
    else if (aarsak === "annet" && customAarsak!.length > 100)
      return "Beskrivelse kan ikke inneholde mer enn 100 tegn";
    return resolveErrorMessage(mutation.error);
  };

  return (
    <div className={styles.error}>
      <b>• {beskrivelser()}</b>
    </div>
  );
}
