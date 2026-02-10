import { Tag, TagProps } from "@navikt/ds-react";
import classNames from "classnames";
import { PropsWithChildren } from "react";

interface StatusTagProps extends PropsWithChildren {
  variant?: TagProps["variant"];
  dataColor: TagProps["data-color"];
  className?: string;
}

export function StatusTag({ variant = "outline", dataColor, children, className }: StatusTagProps) {
  return (
    <Tag
      className={classNames("text-center whitespace-nowrap", className)}
      size="small"
      data-color={dataColor}
      variant={variant}
    >
      {children}
    </Tag>
  );
}
