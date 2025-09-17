import { Tag, TagProps } from "@navikt/ds-react";
import classNames from "classnames";

export function StatusTag({ children, className, ...rest }: TagProps) {
  return (
    <Tag
      className={classNames("min-w-[140px] text-center whitespace-nowrap", className)}
      size="small"
      {...rest}
    >
      {children}
    </Tag>
  );
}
