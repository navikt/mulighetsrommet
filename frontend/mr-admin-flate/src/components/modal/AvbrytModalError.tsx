import styles from "@/components/modal/Modal.module.scss";
import { resolveErrorMessage } from "mulighetsrommet-frontend-common/components/error-handling/errors";

interface Props {
  mutation: any;
  aarsak: string | null;
  customAarsak: string | null;
}
export function AvbrytModalError({ mutation, aarsak, customAarsak }: Props) {
  const beskrivelser = () => {
    if (aarsak?.length === 0) return "Du må velge en årsak";
    else if (aarsak === "annet" && !customAarsak)
      return "Beskrivelse er obligatorisk når “Annet” er valgt som årsak";
    else if (aarsak === "annet" && customAarsak!.length > 100)
      return "Beskrivelse kan ikke inneholde mer enn 100 tegn";
    else return resolveErrorMessage(mutation.error);
  };

  return (
    <div className={styles.error}>
      <b>• {beskrivelser()}</b>
    </div>
  );
}
