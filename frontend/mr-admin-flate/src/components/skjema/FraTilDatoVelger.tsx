import styles from "./FraTilDatoVelger.module.scss";
import { ControlledDateInput, DateInputProps } from "./ControlledDateInput";

export function FraTilDatoVelger({
  fra,
  til,
  size
}: {
  fra: DateInputProps;
  til: DateInputProps;
  size?: "small" | "medium"
}) {
  return (
    <div className={styles.dato_container}>
      <ControlledDateInput size={size} {...fra} />
      <ControlledDateInput size={size} {...til} />
    </div>
  );
}