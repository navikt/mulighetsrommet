import { ReactNode } from "react";
import { HelpText, HStack, Label } from "@navikt/ds-react";

export interface LabelWithHelpTextProps {
  label: string;
  helpTextTitle: string;
  placement?:
    | "top"
    | "bottom"
    | "right"
    | "left"
    | "top-start"
    | "top-end"
    | "bottom-start"
    | "bottom-end"
    | "right-start"
    | "right-end"
    | "left-start"
    | "left-end";
  children: ReactNode;
}

export function LabelWithHelpText(props: LabelWithHelpTextProps) {
  const { label, helpTextTitle, placement = "top", children } = props;
  return (
    <HStack align="center" gap="2">
      <Label size="small">{label}</Label>
      <HelpText placement={placement} title={helpTextTitle}>
        {children}
      </HelpText>
    </HStack>
  );
}
