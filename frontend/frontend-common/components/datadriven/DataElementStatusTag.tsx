import { TagProps } from "@navikt/ds-react";
import { DataElementStatusVariant } from "./types";
import { ExpandableStatusTag } from "../tags/ExpandableStatusTag";
import { StatusTag } from "../tags/StatusTag";

interface DataElementStatusTagProps {
  value: string;
  variant: DataElementStatusVariant;
  description?: string | null;
}

export function DataElementStatusTag(props: DataElementStatusTagProps) {
  const { dataColor, className } = getStatusTagStyles(props.variant);
  if (props.description) {
    const label = `${props.value} - ${props.description}`;
    return (
      <ExpandableStatusTag data-color={dataColor} className={className}>
        {label}
      </ExpandableStatusTag>
    );
  } else {
    return (
      <StatusTag dataColor={dataColor} variant="outline" className={className}>
        {props.value}
      </StatusTag>
    );
  }
}

function getStatusTagStyles(variant: DataElementStatusVariant): {
  dataColor: TagProps["data-color"];
  className?: string;
} {
  switch (variant) {
    case DataElementStatusVariant.BLANK:
      return {
        dataColor: "neutral",
        className: "bg-ax-bg-default border-[color:var(--ax-border-neutral)]",
      };
    case DataElementStatusVariant.NEUTRAL:
      return { dataColor: "neutral" };
    case DataElementStatusVariant.ALT_1:
      return { dataColor: "meta-purple" };
    case DataElementStatusVariant.ALT_2:
    case DataElementStatusVariant.ALT_3:
      return { dataColor: "meta-lime" };
    case DataElementStatusVariant.INFO:
      return { dataColor: "info" };
    case DataElementStatusVariant.SUCCESS:
      return { dataColor: "success" };
    case DataElementStatusVariant.WARNING:
      return { dataColor: "warning" };
    case DataElementStatusVariant.ERROR:
      return { dataColor: "danger" };
    case DataElementStatusVariant.ERROR_BORDER_STRIKETHROUGH:
      return {
        dataColor: "danger",
        className: "border-ax-bg-danger line-through",
      };
  }
}
