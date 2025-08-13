import { TrashIcon } from "@navikt/aksel-icons";
import { Button, Tooltip } from "@navikt/ds-react";
import { MouseEventHandler } from "react";

interface Props {
  tooltip?: string;
  onClick?: MouseEventHandler<HTMLButtonElement>;
  className?: string;
}

export default function TrashButton({ onClick, tooltip, className }: Props) {
  const btn = (
    <Button
      className={className}
      size="small"
      variant="secondary-neutral"
      icon={<TrashIcon aria-hidden />}
      onClick={onClick}
    />
  );

  return tooltip ? <Tooltip content={tooltip}>{btn}</Tooltip> : btn;
}
