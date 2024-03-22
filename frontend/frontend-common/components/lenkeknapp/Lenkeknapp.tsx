import { MouseEventHandler, ReactNode } from "react";
import classNames from "classnames";
import { Lenke } from "../lenke/Lenke";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  children: ReactNode;
  variant: "primary" | "secondary" | "tertiary";
  onClick?: MouseEventHandler<HTMLAnchorElement>;
  className?: string;
  size?: "small" | "medium";
  isExternal?: boolean;
  dataTestid?: string;
}

export function Lenkeknapp({
  to,
  variant,
  onClick,
  className,
  children,
  isExternal,
  size,
  dataTestid,
}: Props) {
  return (
    <Lenke
      isExternal={isExternal}
      to={to}
      onClick={onClick}
      className={classNames(
        style.lenkeknapp,
        "navds-button",
        "button",
        `navds-button--${variant}`,
        {
          "navds-button--small": size === "small",
          "navds-label--small": size === "small",
        },
        className,
      )}
      data-testid={dataTestid}
    >
      {children}
    </Lenke>
  );
}
