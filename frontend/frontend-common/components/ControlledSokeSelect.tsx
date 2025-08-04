import React, { ForwardedRef } from "react";
import { Controller } from "react-hook-form";
import { UNSAFE_Combobox } from "@navikt/ds-react";

export interface ControlledSelectProps {
  label: string;
  hideLabel?: boolean;
  placeholder: string;
  options: { value: string; label: string }[];
  readOnly?: boolean;
  name: string;
  multiselect?: boolean;
  /*handleOnChange: (a0: {
    target: {
      value?: string;
      name?: string;
    };
  }) => void;*/
  //onInputChange?: (input: string) => void;
  //className?: string;
  size?: "small" | "medium";
  // onClearValue?: () => void;
  description?: string;
  // helpText?: React.ReactNode;
  id?: string;
}

function ControlledSokeSelect<T>(props: ControlledSelectProps, _: ForwardedRef<HTMLElement>) {
  const {
    label,
    hideLabel = false,
    readOnly = false,
    size = "small",
    multiselect = false,
    placeholder,
    options,
    description,
    id,
    ...rest
  } = props;

  return (
    <Controller
      {...rest}
      render={({ field: { onChange, value, name, ref }, fieldState: { error } }) => {
        return (
          <UNSAFE_Combobox
            ref={ref}
            id={id}
            label={label}
            placeholder={placeholder}
            readOnly={readOnly}
            hideLabel={hideLabel}
            isMultiSelect={multiselect}
            size={size}
            name={name}
            description={description}
            error={error?.message}
            options={options}
            onToggleSelected={(option, isSelected) => {
              if (isSelected) {
                onChange([...value, option]);
              } else {
                onChange(value.filter((v: T) => v !== option));
              }
            }}
          />
        );
      }}
    />
  );
}

const ControlledSokeSelectComponent = React.forwardRef(ControlledSokeSelect);

export { ControlledSokeSelectComponent as ControlledSokeSelect };
