import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import classNames from "classnames";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  children: React.ReactNode;
  variant: "primary" | "secondary" | "tertiary";
  handleClick?: () => void;
  className?: string;
  dataTestId?: string;
  size?: "small" | "medium";
}

export function Lenkeknapp({
  to,
  variant,
  handleClick,
  className,
  dataTestId,
  children,
  size,
}: Props) {
  return (
    <Lenke
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
      data-testid={dataTestId}
    >
      {children}
    </Lenke>
  );
}
