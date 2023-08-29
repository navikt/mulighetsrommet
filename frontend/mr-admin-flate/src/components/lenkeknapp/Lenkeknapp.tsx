import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import classNames from "classnames";
import style from "./Lenkeknapp.module.scss";

interface Props {
  to: string;
  lenketekst: string;
  variant: "primary" | "secondary" | "tertiary";
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
        "lagre_knapp",
        `navds-button--${variant}`,
        fontSize(),
        className,
      )}
      data-testid={dataTestId}
    >
      {lenketekst}
    </Lenke>
  );
}
