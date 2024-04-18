import { RadioGroup } from "@navikt/ds-react";
import { ReactNode, forwardRef } from "react";
import { Controller } from "react-hook-form";

export interface Props {
  readOnly?: boolean;
  size?: "small" | "medium";
  legend?: string;
  description?: string;
  hideLegend?: boolean;
  children: ReactNode;
  name: string;
}

export const ControlledRadioGroup = forwardRef(function ControlledRadioGroup(
  props: Props,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
  const {
    size,
    readOnly,
    legend,
    hideLegend = false,
    description,
    children,
    name,
    ...rest
  } = props;
  return (
    <div>
      <Controller
        name={name}
        {...rest}
        render={({ field: { onChange, value }, fieldState: { error } }) => {
          return (
            <RadioGroup
              readOnly={readOnly}
              legend={legend}
              hideLegend={hideLegend}
              description={description}
              size={size}
              onChange={onChange}
              value={value}
              error={error?.message as string}
            >
              {children}
            </RadioGroup>
          );
        }}
      />
    </div>
  );
});
