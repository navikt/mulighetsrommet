import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import classNames from "classnames";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  children: React.ReactNode;
  variant: "primary" | "secondary" | "tertiary";
  handleClick?: () => void;
  className?: string;
  size?: "small" | "medium";
  isExternal?: boolean;
}

export function Lenkeknapp({
  to,
  variant,
  handleClick,
  className,
  children,
  isExternal,
  size,
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
    >
      {children}
    </Lenke>
  );
}
