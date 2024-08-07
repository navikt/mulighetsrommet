import { PropsWithChildren } from "react";
import styles from "./DetaljerInfoContainer.module.scss";
import classNames from "classnames";

interface Props {
  withBorderRight?: boolean;
}

export function DetaljerInfoContainer(props: PropsWithChildren<Props>) {
  const { withBorderRight = true } = props;
  return (
    <div
      className={classNames(styles.detaljer_info_container, {
        [styles.border_right]: withBorderRight,
      })}
    >
      {props.children}
    </div>
  );
}
