import { ReactNode } from "react";
import { HelpText, HStack } from "@navikt/ds-react";

export interface LabelWithHelpTextProps {
  label: string;
  helpTextTitle: string;
  placement?: "top" | "bottom" | "right" | "left" | "top-start" | "top-end" | "bottom-start" | "bottom-end" | "right-start" | "right-end" | "left-start" | "left-end";
  children: ReactNode;
}

export function LabelWithHelpText(props: LabelWithHelpTextProps) {
  const { label, helpTextTitle, placement = "top", children } = props;
  return (
    <HStack gap="1">
      <legend>{label}</legend>
      <HelpText placement={placement} style={{ marginTop: "-4px", }} title={helpTextTitle}>{children}</HelpText>
    </HStack>
  );
}
