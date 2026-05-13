import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { HStack, Tabs } from "@navikt/ds-react";

interface Props {
  hasError: boolean;
  onClick: () => void;
  value: string;
  label: string;
  icon?: React.ReactElement;
}

export function TabWithErrorBorder(props: Props) {
  const { hasError, onClick, value, label, icon } = props;

  return (
    <Tabs.Tab
      onClick={onClick}
      style={{
        border: hasError ? "solid 2px #C30000" : "",
        borderRadius: hasError ? "8px" : 0,
      }}
      value={value}
      label={
        hasError ? (
          <span style={{ display: "flex", alignContent: "baseline", gap: "0.4rem" }}>
            <ExclamationmarkTriangleFillIcon aria-label={label} /> {label}
          </span>
        ) : (
          <HStack gap="space-4" align="center">
            {icon && icon}
            {label}
          </HStack>
        )
      }
    />
  );
}
