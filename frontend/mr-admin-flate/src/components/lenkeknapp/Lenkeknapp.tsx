import classNames from "classnames";
import { Lenke } from "../../../../frontend-common/components/lenke/Lenke";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  children: React.ReactNode;
  variant: "primary" | "secondary" | "tertiary";
  handleClick?: () => void;
  className?: string;
  size?: "small" | "medium";
  isExternal?: boolean;
  dataTestid?: string;
}

export function Lenkeknapp({
  to,
  variant,
  handleClick,
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
      onClick={handleClick}
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
