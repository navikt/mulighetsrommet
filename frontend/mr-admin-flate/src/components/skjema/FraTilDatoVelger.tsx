import styles from "./FraTilDatoVelger.module.scss";
import { ControlledDateInput, DateInputProps } from "./ControlledDateInput";
import { ReactNode } from "react";

export function FraTilDatoVelger({
  fra,
  til,
  size,
  children,
}: {
  fra: DateInputProps;
  til: DateInputProps;
  size?: "small" | "medium";
  children?: ReactNode;
}) {
  return (
    <div className={styles.dato_container}>
      <ControlledDateInput size={size} {...fra} />
      <ControlledDateInput size={size} {...til} />
      {children}
    </div>
  );
}
