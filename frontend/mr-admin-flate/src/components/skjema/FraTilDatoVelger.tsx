import styles from "./FraTilDatoVelger.module.scss";
import { ControlledDateInput, DateInputProps } from "./ControlledDateInput";

export function FraTilDatoVelger({ fra, til }: { fra: DateInputProps; til: DateInputProps }) {
  return (
    <div className={styles.dato_container}>
      <ControlledDateInput {...fra} />
      <ControlledDateInput {...til} />
    </div>
  );
}