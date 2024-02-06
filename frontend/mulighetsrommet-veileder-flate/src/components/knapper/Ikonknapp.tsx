import React from "react";
import { Button } from "@navikt/ds-react";
import classNames from "classnames";
import styles from "./Ikonknapp.module.scss";

interface SidemenyKnappProps {
  children?: React.ReactNode;
  icon?: React.ReactNode;
  className?: string;
  handleClick: (e: React.MouseEvent) => void;
  ariaLabel: string;
  dataTestId?: string;
}

const Ikonknapp = ({
  children,
  icon,
  ariaLabel,
  className,
  handleClick,
  dataTestId,
}: SidemenyKnappProps) => {
  return (
    <Button
      onClick={handleClick}
      icon={icon}
      variant="tertiary"
      className={classNames(styles.ikonknapp, className)}
      aria-label={ariaLabel}
      data-testid={dataTestId}
    >
      {children}
    </Button>
  );
};

export default Ikonknapp;
