import { ReactNode } from "react";
import { HelpText, HStack } from "@navikt/ds-react";

export interface LabelWithHelpTextProps {
  label: string;
  helpTextTitle: string;
  children: ReactNode;
}

export function LabelWithHelpText(props: LabelWithHelpTextProps) {
  const { label, helpTextTitle, children } = props;
  return (
    <HStack gap="1">
      <legend>{label}</legend>
      <HelpText style={{ marginTop: "-4px", }} title={helpTextTitle}>{children}</HelpText>
    </HStack>
  );
}
