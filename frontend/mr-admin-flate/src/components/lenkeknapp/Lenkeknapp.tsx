import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import classNames from "classnames";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  lenketekst: string;
  variant: string;
  handleClick?: () => void;
  className?: string;
  dataTestId?: string;
  size?: string;
}
export function Lenkeknapp({
  to,
  lenketekst,
  variant,
  handleClick,
  className,
  dataTestId,
  size,
}: Props) {
  const variantType = () => {
    if (variant === "primary") {
      return "navds-button--primary";
    } else if (variant === "secondary") {
      return "navds-button--secondary";
    } else if (variant === "tertiary") {
      return "navds-button--tertiary";
    }
  };

  const fontSize = () => {
    if (size === "small") {
      return "navds-button--small" && "navds-label--small";
    }
  };

  return (
    <Lenke
      to={to}
      onClick={handleClick}
      className={classNames(
        style.lenkeknapp,
        "navds-button",
        "button",
        variantType(),
        fontSize(),
        className,
      )}
      data-testid={dataTestId}
    >
      {lenketekst}
    </Lenke>
  );
}
