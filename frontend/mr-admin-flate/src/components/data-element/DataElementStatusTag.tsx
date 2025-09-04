import { DataElementStatusVariant } from "@tiltaksadministrasjon/api-client";
import { StatusTag } from "@mr/frontend-common";
import { TagProps } from "@navikt/ds-react";

interface DataElementStatusTagProps {
  value: string;
  variant: DataElementStatusVariant;
}

export function DataElementStatusTag(props: DataElementStatusTagProps) {
  const { variant, className } = getStatusTagStyles(props.variant);
  return (
    <StatusTag variant={variant} className={className}>
      {props.value}
    </StatusTag>
  );
}

function getStatusTagStyles(variant: DataElementStatusVariant): {
  variant: TagProps["variant"];
  className?: string;
} {
  switch (variant) {
    case DataElementStatusVariant.NEUTRAL:
      return { variant: "neutral" };
    case DataElementStatusVariant.ALT:
      return { variant: "alt1" };
    case DataElementStatusVariant.SUCCESS:
      return { variant: "success" };
    case DataElementStatusVariant.WARNING:
      return { variant: "warning" };
    case DataElementStatusVariant.ERROR:
      return { variant: "error" };
    case DataElementStatusVariant.ERROR_BORDER:
      return {
        variant: "neutral",
        className: "bg-white border-[color:var(--a-text-danger)]",
      };
    case DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH:
      return {
        variant: "neutral",
        className:
          "bg-white text-[color:var(--a-text-danger)] border-[color:var(--a-text-danger)] line-through",
      };
  }
}
