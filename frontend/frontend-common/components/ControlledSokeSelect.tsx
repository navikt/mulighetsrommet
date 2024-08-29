import React, { ForwardedRef } from "react";
import { Controller } from "react-hook-form";
import { SelectOption, SokeSelect } from "./SokeSelect";
import { shallowEquals } from "../utils/shallow-equals";

export interface ControlledSelectProps<T> {
  label: string;
  hideLabel?: boolean;
  placeholder: string;
  options: SelectOption<T>[];
  readOnly?: boolean;
  name: string;
  onChange?: (a0: {
    target: {
      value?: T;
      name?: string;
    };
  }) => void;
  onInputChange?: (input: string) => void;
  className?: string;
  size?: "small" | "medium";
  onClearValue?: () => void;
  description?: string;
  helpText?: React.ReactNode;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function ControlledSokeSelect<T>(props: ControlledSelectProps<T>, _: ForwardedRef<HTMLElement>) {
  const {
    label,
    hideLabel = false,
    placeholder,
    options,
    readOnly = false,
    onChange: providedOnChange,
    onInputChange: providedOnInputChange,
    description,
    className,
    size,
    helpText,
    onClearValue,
    ...rest
  } = props;

  return (
    <Controller
      {...rest}
      render={({ field: { onChange, value, name, ref }, fieldState: { error } }) => {
        const selectedOption = options.find((option) => shallowEquals(option.value, value));
        return (
          <SokeSelect<T>
            helpText={helpText}
            hideLabel={hideLabel}
            description={description}
            size={size}
            label={label}
            error={error}
            placeholder={placeholder}
            readOnly={readOnly}
            onClearValue={onClearValue}
            childRef={ref}
            name={name}
            value={selectedOption ?? null}
            onChange={(e) => {
              onChange(e?.target.value);
              providedOnChange?.(e);
            }}
            onInputChange={(e) => {
              providedOnInputChange?.(e);
            }}
            options={options}
            className={className}
          />
        );
      }}
    />
  );
}

const ControlledSokeSelectComponent = React.forwardRef(ControlledSokeSelect);

export { ControlledSokeSelectComponent as ControlledSokeSelect };
