import styles from "@/components/modal/Modal.module.scss";
import { resolveErrorMessage } from "mulighetsrommet-frontend-common/components/error-handling/errors";

interface Props {
  mutation: any;
  aarsak: string | null;
  customAarsak: string | null;
}
export function AvbrytModalError({ mutation, aarsak, customAarsak }: Props) {
  return (
    <div className={styles.error}>
      <b>
        •{" "}
        {aarsak === "annet" && !customAarsak
          ? "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"
          : resolveErrorMessage(mutation.error)}
      </b>
    </div>
  );
}
