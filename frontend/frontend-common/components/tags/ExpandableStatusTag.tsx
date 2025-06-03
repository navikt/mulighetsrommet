import { Tag, TagProps } from "@navikt/ds-react";
import { useState } from "react";

export function ExpandableStatusTag({ children, ...rest }: TagProps) {
  const [truncateLabel, setTruncateLabel] = useState<boolean>(true);

  const label = truncateLabel && typeof children === "string" ? truncate(children, 30) : children;

  return (
    <Tag
      style={{ maxWidth: "400px" }}
      size="small"
      onMouseEnter={() => setTruncateLabel(false)}
      onMouseLeave={() => setTruncateLabel(true)}
      {...rest}
    >
      {label}
    </Tag>
  );
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
