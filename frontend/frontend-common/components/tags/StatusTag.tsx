import { Tag, TagProps } from "@navikt/ds-react";

export function StatusTag({ children, ...rest }: TagProps) {
  return (
    <Tag className="min-w-[140px] text-center whitespace-nowrap" size="small" {...rest}>
      {children}
    </Tag>
  );
}
